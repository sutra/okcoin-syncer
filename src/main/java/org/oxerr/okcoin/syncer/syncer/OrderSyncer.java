package org.oxerr.okcoin.syncer.syncer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.oxerr.okcoin.rest.OKCoinException;
import org.oxerr.okcoin.rest.dto.Order;
import org.oxerr.okcoin.rest.dto.Status;
import org.oxerr.okcoin.syncer.dao.OrderDao;
import org.oxerr.okcoin.syncer.service.OKCoinTradeServiceRawExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderSyncer extends AbstractSyncer {

	private final Logger log = Logger.getLogger(OrderSyncer.class.getName());
	private final OKCoinTradeServiceRawExt extRawTradeService;
	private final OrderDao orderDao;
	private final String symbol;
	private long lastId;

	@Autowired
	public OrderSyncer(
			OKCoinTradeServiceRawExt extRawTradeService,
			OrderDao orderDao,
			@Value("${okcoin.order.interval}") long interval,
			@Value("${okcoin.order.symbol}") String symbol) {
		super(interval);
		this.extRawTradeService = extRawTradeService;
		this.orderDao = orderDao;
		this.symbol = symbol;
	}

	@Override
	protected void init() {
		this.lastId = orderDao.getLastId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void sync() throws IOException {
		if (!Thread.interrupted()) {
			syncOrders();
		}

		// status: -1 = cancelled, 0 = unfilled, 1 = partially filled,
		// 2 = fully filled, 4 = cancel request in process
		Arrays.stream(new Status[] {
			Status.UNFILLED,
			Status.PARTIALLY_FILLED,
			Status.CANCEL_REQUEST_IN_PROCESS,
		}).forEach(status -> {
			if (!Thread.interrupted()) {
				try {
					syncOrders(status);
				} catch (Exception e) {
					log.log(Level.WARNING, e.getMessage());
				}
			}
		});
	}

	private void syncOrders() throws IOException {
		SortedSet<Order> openOrders = sync(0, lastId);
		SortedSet<Order> closedOrders = sync(1, lastId);

		orderDao.merge(openOrders);
		orderDao.merge(closedOrders);

		this.lastId = Math.max(
				openOrders.isEmpty() ? this.lastId : openOrders.last().getOrderId(),
				closedOrders.isEmpty() ? this.lastId : closedOrders.last().getOrderId());
		log.log(Level.FINE, "Last ID: {0}", this.lastId);
	}

	private SortedSet<Order> sync(int status, long lastId) throws IOException {
		SortedSet<Order> orders = extRawTradeService.getOrders(symbol, status, lastId);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Order count: {0}", orders.size());
			orders.forEach(o -> log.log(Level.FINEST, "{0}", o.getCreateDate()));
		}
		return orders;
	}

	private void syncOrders(Status status) throws OKCoinException, IOException {
		log.log(Level.FINE, "Syncing orders which status is {0}...", status);
		List<Order> orders;
		long sinceId = 0;
		do {
			orders = orderDao.getOrders(status.getCode(), sinceId, 50);
			if (!orders.isEmpty()) {
				syncOrders(orders);
				sinceId = orders.get(orders.size() - 1).getOrderId();
			}
		} while (!Thread.interrupted() && orders.size() > 0);
	}

	private void syncOrders(List<Order> orders) throws OKCoinException, IOException {
		List<Long> orderIds = orders.stream().mapToLong(o -> o.getOrderId()).boxed().collect(Collectors.toList());
		Arrays.stream(extRawTradeService.getOrders(symbol, 0, orderIds.toArray(new Long[] {}))).forEach(order -> orderDao.update(order));
	}

}
