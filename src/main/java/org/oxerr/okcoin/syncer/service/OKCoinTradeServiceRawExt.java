package org.oxerr.okcoin.syncer.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.oxerr.okcoin.rest.OKCoinException;
import org.oxerr.okcoin.rest.dto.Order;
import org.oxerr.okcoin.rest.dto.OrderHistory;
import org.oxerr.okcoin.rest.dto.OrderResult;
import org.oxerr.okcoin.rest.service.polling.OKCoinTradeServiceRaw;

public class OKCoinTradeServiceRawExt {

	private static final int MAX_PAGE_LENGTH = 200;
	private final Logger log = Logger.getLogger(OKCoinTradeServiceRawExt.class.getName());
	private final IOExceptionRetryService retryService;
	private final OKCoinTradeServiceRaw rawTradeService;

	public OKCoinTradeServiceRawExt(
			IOExceptionRetryService retryService,
			OKCoinTradeServiceRaw rawTradeService) {
		this.retryService = retryService;
		this.rawTradeService = rawTradeService;
	}

	public Order[] getOrders(String symbol, int type, Long[] orderIds)
			throws OKCoinException, IOException {
		OrderResult result = retryService.retry(() -> rawTradeService
				.getOrders(symbol, type, orderIds));
		return result.getOrders();
	}

	public SortedSet<Order> getOrders(String symbol, int status, long sinceId)
			throws OKCoinException, IOException {
		final SortedSet<Order> orders = newSortedSet();
		int currentPage = 1;
		int pageLength = MAX_PAGE_LENGTH;
		SortedSet<Order> preBatch = null, batch;
		do {
			boolean hasGap;
			do {
				batch = getOrders(symbol, status, currentPage, pageLength);
				hasGap = !orders.isEmpty()
					&& !batch.isEmpty()
					&& (batch.last().getOrderId() < orders.first().getOrderId()
						|| orders.last().getOrderId() < batch.first().getOrderId());
				if (pageLength < MAX_PAGE_LENGTH) {
					pageLength++;
				} else {
					currentPage--;
				}
			} while (hasGap);

			if (preBatch != null
					&& !preBatch.isEmpty()
					&& !batch.isEmpty()
					&& preBatch.first().getOrderId() == batch.first().getOrderId()
					&& preBatch.last().getOrderId() == batch.last().getOrderId()) {
				log.log(Level.FINE, "pre batch is same as current batch.");
				break;
			}

			preBatch = batch;
			orders.addAll(batch);
			log.log(Level.FINEST, "Order count: {0}", orders.size());

			if (!orders.isEmpty() && orders.first().getOrderId() <= sinceId) {
				break;
			}

			currentPage++;
			pageLength--;
		} while (true);
		return toSortedSet(
				orders
				.stream()
				.filter(o -> o.getOrderId() > sinceId)
				.collect(Collectors.toSet()));
	}

	public SortedSet<Order> getOrders(String symbol, int status,
			int currentPage, int pageLength) throws OKCoinException,
			IOException {
		OrderHistory history = getOrderHistory(symbol, status, currentPage,
				pageLength);
		return toSortedSet(history.getOrders());
	}

	public OrderHistory getOrderHistory(String symbol, int status,
			int currentPage, int pageLength) throws OKCoinException,
			IOException {
		log.log(Level.FINEST,
				"Getting order history: symbol = {0}, status = {1}, currentPage = {2}, pageLength = {3}",
				new Object[] { symbol, status, currentPage, pageLength, });
		return retryService.retry(() -> rawTradeService.getOrderHistory(symbol,
				status, currentPage, pageLength));
	}

	private SortedSet<Order> toSortedSet(Order[] orders) {
		return toSortedSet(Arrays.asList(orders));
	}

	private SortedSet<Order> toSortedSet(Collection<Order> orders) {
		SortedSet<Order> set = newSortedSet();
		set.addAll(orders);
		return set;
	}

	private SortedSet<Order> newSortedSet() {
		return new TreeSet<>((o1, o2) -> Long.compare(
				o1.getOrderId(), o2.getOrderId()));
	}

}
