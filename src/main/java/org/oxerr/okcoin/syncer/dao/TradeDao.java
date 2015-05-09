package org.oxerr.okcoin.syncer.dao;

import org.oxerr.okcoin.rest.dto.Trade;

public interface TradeDao {

	long getLastId();

	int[] insert(Trade[] trades);

}
