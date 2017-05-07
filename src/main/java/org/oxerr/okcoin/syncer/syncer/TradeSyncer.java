package org.oxerr.okcoin.syncer.syncer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.oxerr.okcoin.rest.dto.Trade;
import org.oxerr.okcoin.rest.service.OKCoinMarketDataServiceRaw;
import org.oxerr.okcoin.syncer.dao.TradeDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TradeSyncer {

	private final Logger log = Logger.getLogger(TradeSyncer.class.getName());
	private final OKCoinMarketDataServiceRaw rawMdService;
	private final TradeDao tradeDao;
	private final String symbol;
	private long lastId;

	@Autowired
	public TradeSyncer(
			OKCoinMarketDataServiceRaw rawMdService,
			TradeDao tradeDao,
			@Value("${okcoin.trade.symbol}") String symbol) {
		this.rawMdService = rawMdService;
		this.tradeDao = tradeDao;
		this.symbol = symbol;
	}

	@PostConstruct
	private void init() {
		this.lastId = tradeDao.getLastId();
	}

	public void sync() throws IOException {
		Trade[] trades;
		do {
			log.log(Level.INFO, "Last ID: {0}", lastId);
			trades = rawMdService.getTrades(symbol, lastId);
			log.log(Level.FINE, "trades.length: {0}", trades.length);
			tradeDao.insert(trades);
			if (trades.length > 0) {
				lastId = trades[trades.length - 1].getTid();
			}
		} while (!Thread.interrupted() && trades.length > 0);
	}

}
