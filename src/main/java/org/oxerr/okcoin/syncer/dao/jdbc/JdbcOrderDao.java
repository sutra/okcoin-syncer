package org.oxerr.okcoin.syncer.dao.jdbc;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.sql.DataSource;

import org.oxerr.okcoin.rest.dto.Order;
import org.oxerr.okcoin.rest.dto.Status;
import org.oxerr.okcoin.rest.dto.Type;
import org.oxerr.okcoin.syncer.dao.OrderDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOrderDao extends JdbcDaoSupport implements OrderDao {

	private static final String GET_MAX_ID_SQL = "select max(id) from \"order\"";
	private static final String SELECT_ORDER_SQL = "select id, date, symbol, type, price, amount, deal_amount, status, avg_price from \"order\" where status = ? and id > ? order by id limit ?";
	private static final String INSERT_ORDER_SQL = "insert into \"order\"(id, date, symbol, type, price, amount, deal_amount, status, avg_price) values(?, ?, ?, ?::\"type\", ?, ?, ?, ?, ?)";
	private static final String UPDATE_ORDER_SQL = "update \"order\" set deal_amount = ?, status = ?, avg_price = ? where id = ?";

	private final Logger log = Logger.getLogger(JdbcOrderDao.class.getName());

	@Autowired
	public JdbcOrderDao(DataSource dataSource) {
		setDataSource(dataSource);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getLastId() {
		Long lastId = getJdbcTemplate().queryForObject(GET_MAX_ID_SQL, Long.class);
		return lastId == null ? 0L : lastId.longValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Order> getOrders(int status, long sinceId, long limit) {
		log.log(Level.FINER, "getOrders({0}, {1}, {2})",
			new Object[] {
				status, sinceId, limit,
			}
		);
		return getJdbcTemplate().query(SELECT_ORDER_SQL,
			new RowMapper<Order>() {

				@Override
				public Order mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					long orderId = rs.getLong("id");
					Instant createDate = rs.getTimestamp("date").toInstant();
					String symbol = rs.getString("symbol");
					Type type = Type.of(rs.getString("type"));
					BigDecimal price = rs.getBigDecimal("price");
					BigDecimal amount = rs.getBigDecimal("amount");
					BigDecimal dealAmount = rs.getBigDecimal("deal_amount");
					Status status = Status.of(rs.getInt("status"));
					BigDecimal avgPrice = rs.getBigDecimal("avg_price");
					return new Order(orderId, status, symbol, type, price, amount, dealAmount, avgPrice, createDate);
				}

			}, status, sinceId, limit);
	}

	@Override
	public void insert(Order order) {
		this.insert(Arrays.asList(order));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void insert(Iterable<Order> orders) {
		List<Order> orderList = StreamSupport.stream(orders.spliterator(), false).collect(Collectors.toList());
		getJdbcTemplate().batchUpdate(INSERT_ORDER_SQL, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				Order order = orderList.get(i);
				ps.setLong(1, order.getOrderId());
				ps.setTimestamp(2, Timestamp.from(order.getCreateDate()));
				ps.setString(3, order.getSymbol());
				ps.setString(4, order.getType().getCode());
				ps.setBigDecimal(5, order.getPrice());
				ps.setBigDecimal(6, order.getAmount());
				ps.setBigDecimal(7, order.getDealAmount());
				ps.setInt(8, order.getStatus().getCode());
				ps.setBigDecimal(9, order.getAvgPrice());
			}

			@Override
			public int getBatchSize() {
				return orderList.size();
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int update(Order order) {
		return getJdbcTemplate().update(UPDATE_ORDER_SQL,
			order.getDealAmount(),
			order.getStatus().getCode(),
			order.getAvgPrice(),
			order.getOrderId());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void merge(Iterable<Order> orders) {
		try {
			insert(orders);
		} catch (DuplicateKeyException e) {
			log.log(Level.FINER, e.getMessage(), e);
			for (Order order : orders) {
				int rows = update(order);
				log.log(Level.FINER, "order id: {0}, rows: {1}", new Object[] {
						order.getOrderId(), rows, });
				if (rows == 0) {
					insert(order);
				}
			}
		}
	}

}
