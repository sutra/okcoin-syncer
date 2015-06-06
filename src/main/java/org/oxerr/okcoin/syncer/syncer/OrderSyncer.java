package org.oxerr.okcoin.syncer.syncer;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.oxerr.okcoin.rest.OKCoinException;
import org.oxerr.okcoin.rest.dto.Order;
import org.oxerr.okcoin.rest.dto.Status;
import org.oxerr.okcoin.syncer.dao.OrderDao;
import org.oxerr.okcoin.syncer.service.OKCoinTradeServiceRawExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderSyncer {

	private final Logger log = Logger.getLogger(OrderSyncer.class.getName());
	private final OKCoinTradeServiceRawExt extRawTradeService;
	private final OrderDao orderDao;
	private final String symbol;
	private long lastId;

	@Autowired
	public OrderSyncer(
			OKCoinTradeServiceRawExt extRawTradeService,
			OrderDao orderDao,
			@Value("${okcoin.order.symbol}") String symbol) {
		this.extRawTradeService = extRawTradeService;
		this.orderDao = orderDao;
		this.symbol = symbol;
	}

	@PostConstruct
	private void init() {
		this.lastId = orderDao.getLastId();
	}

	public void sync() throws IOException {
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

	/**
	 * Synchronize the order status from exchange to database.
	 *
	 * @param orders the orders to be synchronized.
	 * @throws OKCoinException indicates exchange side exception.
	 * @throws IOException indicates I/O exception.
	 */
	private void syncOrders(List<Order> orders) throws OKCoinException,
			IOException {
		final Map<Long, Order> oldStatus = orders.stream()
			.collect(toMap(Order::getOrderId, identity()));
		final List<Long> orderIds = orders.stream()
			.mapToLong(o -> o.getOrderId())
			.boxed()
			.collect(toList());

		// Query type: 0 for unfilled (open) orders, 1 for filled orders
		syncOrders(oldStatus, orderIds, 0);
		syncOrders(oldStatus, orderIds, 1);
	}

	private void syncOrders(Map<Long, Order> oldStatus, List<Long> orderIds,
			int type) throws OKCoinException, IOException {
		final Order[] newStatus = extRawTradeService.getOrders(symbol, type,
				orderIds.toArray(new Long[] {}));
		Arrays.stream(newStatus).forEach(
			order -> {
				final Order oldOrder = oldStatus.get(order.getOrderId());
				if (order.getStatus() != oldOrder.getStatus()
					|| order.getDealAmount().compareTo(oldOrder.getDealAmount()) != 0
					|| order.getAvgPrice().compareTo(oldOrder.getAvgPrice()) != 0) {
						log.log(Level.FINEST,
							"Updating order {0}({1}, {2}, {3}) to ({4}, {5}, {6})",
							new Object[] {
								oldOrder.getOrderId(),
								oldOrder.getStatus(),
								oldOrder.getDealAmount(),
								oldOrder.getAvgPrice(),
								order.getStatus(),
								order.getDealAmount(),
								order.getAvgPrice(),
							}
						);
					orderDao.update(order);
				}
			}
		);
	}

}
