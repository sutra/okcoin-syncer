package org.oxerr.okcoin.syncer.dao;

import org.oxerr.okcoin.rest.dto.UserInfo;

public interface FundDao {

	void saveFund(UserInfo userInfo);

}
