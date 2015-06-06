package org.oxerr.okcoin.syncer.syncer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.oxerr.okcoin.rest.dto.UserInfo;
import org.oxerr.okcoin.rest.service.polling.OKCoinAccountServiceRaw;
import org.oxerr.okcoin.syncer.dao.FundDao;
import org.oxerr.okcoin.syncer.service.IOExceptionRetryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FundSyncer {

	private final Logger log = Logger.getLogger(FundSyncer.class.getName());
	private final IOExceptionRetryService retryService;
	private final OKCoinAccountServiceRaw accountServiceRaw;
	private final FundDao fundDao;

	@Autowired
	public FundSyncer(IOExceptionRetryService retryService,
			OKCoinAccountServiceRaw accountServiceRaw, FundDao fundDao) {
		this.retryService = retryService;
		this.accountServiceRaw = accountServiceRaw;
		this.fundDao = fundDao;
	}

	public void sync() throws IOException {
		log.log(Level.INFO, "Syncing funds...");
		final UserInfo userInfo = retryService.retry(() -> accountServiceRaw.getUserInfo());
		fundDao.saveFund(userInfo);
		log.log(Level.INFO, "Funds synced.");
	}

}
