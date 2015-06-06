package org.oxerr.okcoin.syncer.dao.jdbc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.oxerr.okcoin.rest.dto.Funds;
import org.oxerr.okcoin.rest.dto.UserInfo;
import org.oxerr.okcoin.syncer.dao.FundDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcFundDao extends JdbcDaoSupport implements FundDao {

	@Autowired
	public JdbcFundDao(DataSource dataSource) {
		setDataSource(dataSource);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public void saveFund(UserInfo userInfo) {
		final Date now = Date.from(Instant.now());
		final Funds funds = userInfo.getInfo().getFunds();
		final UUID assetId = UUID.randomUUID();
		final List<Object[]> fundList = new ArrayList<>(funds.getFree().size());
		funds.getFree().forEach((key, value) -> {
			final UUID fundId = UUID.randomUUID();
			final String currency = key.toUpperCase();
			final BigDecimal free = value;
			final BigDecimal frozen = getAmount(funds.getFrozen(), key);
			final BigDecimal borrow = getAmount(funds.getBorrow(), key);
			final BigDecimal union = getAmount(funds.getUnionFund(), key);
			fundList.add(new Object[] { fundId, assetId, currency, free, frozen, borrow, union, });
		});

		getJdbcTemplate().update("insert into asset(id, date, net, total) values(?, ?, ?, ?)",
				assetId,
				now,
				userInfo.getInfo().getFunds().getAsset().get("net"),
				userInfo.getInfo().getFunds().getAsset().get("total"));
		getJdbcTemplate().batchUpdate(
			"insert into fund(id, asset_id, currency, free, frozen, borrow, \"union\") values(?, ?, ?, ?, ?, ?, ?)",
			fundList);
	}

	private BigDecimal getAmount(final Map<String, BigDecimal> amounts, final String key) {
		final BigDecimal amount;
		if (amounts == null) {
			amount = BigDecimal.ZERO;
		} else {
			amount = toZeroIfNull(amounts.get(key));
		}
		return amount;
	}

	private BigDecimal toZeroIfNull(BigDecimal d) {
		return d == null ? BigDecimal.ZERO : d;
	}

}
