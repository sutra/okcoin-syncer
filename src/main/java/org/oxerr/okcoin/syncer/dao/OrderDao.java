package org.oxerr.okcoin.syncer.dao;

import java.util.List;

import org.oxerr.okcoin.rest.dto.Order;

public interface OrderDao {

	long getLastId();

	List<Order> getOrders(int status, long sinceId, long limit);

	void insert(Iterable<Order> orders);

	void update(Order order);

}
