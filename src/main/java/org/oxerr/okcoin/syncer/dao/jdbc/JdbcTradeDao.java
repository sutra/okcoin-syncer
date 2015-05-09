package org.oxerr.okcoin.syncer.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.oxerr.okcoin.rest.dto.Trade;
import org.oxerr.okcoin.syncer.dao.TradeDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTradeDao extends JdbcDaoSupport implements TradeDao {

	@Autowired
	public JdbcTradeDao(DataSource dataSource) {
		setDataSource(dataSource);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getLastId() {
		return getJdbcTemplate().queryForObject("select max(tid) from trade",
				Long.class);
	}

	@Override
	public int[] insert(Trade[] trades) {
		return getJdbcTemplate().batchUpdate(
			"insert into trade(tid, date, type, amount, price) values(?, ?, ?, ?, ?)",
			new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					Trade trade = trades[i];
					ps.setLong(1, trade.getTid());
					ps.setTimestamp(2, Timestamp.from(trade.getDate()));
					ps.setString(3, trade.getType().toString());
					ps.setBigDecimal(4, trade.getAmount());
					ps.setBigDecimal(5, trade.getPrice());
				}

				@Override
				public int getBatchSize() {
					return trades.length;
				}
			}
		);
	}

}
