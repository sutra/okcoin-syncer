package org.oxerr.okcoin.syncer.dao;

import java.util.List;

import org.oxerr.okcoin.rest.dto.Order;

public interface OrderDao {

	long getLastId();

	List<Order> getOrders(int status, long sinceId, long limit);

	void insert(Order order);

	void insert(Iterable<Order> orders);

	/**
	 * Update the {@code deal_amount}, {@code status}, {@code avg_price} of the
	 * order.
	 *
	 * @param order the order to update.
	 * @return the number of rows affected
	 */
	int update(Order order);

	void merge(Iterable<Order> orders);

}
