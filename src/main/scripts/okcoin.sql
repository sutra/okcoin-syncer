create table trade (
	tid bigint primary key,

	-- transaction time
	date timestamp with time zone not null,

	-- buy/sell
	type varchar(4) not null,

	-- quantity in BTC (or LTC)
	amount numeric(16,8) not null,

	-- transaction price
	price numeric(32,8) not null
);
create index trade_date on trade(date);
create index trade_type on trade(type);

create table "order" (
	-- order ID
	id bigint primary key,

	date timestamp with time zone not null,

	symbol char(7) not null,

	-- buy_market = market buy order, sell_market = market sell order
	type varchar(16) not null,

	-- order price
	price numeric(32,8) not null,

	-- for limit orders, the order quantity
	-- for market orders,the filled quantity
	amount numeric(16,8) not null,

	-- filled quantity
	deal_amount numeric(16,8) not null,

	-- -1 = cancelled,
	-- 0 = unfilled,
	-- 1 = partially filled,
	-- 2 = fully filled,
	-- 4 = cancel request in process
	status int not null,

	-- average transaction price
	avg_price numeric(32,8) not null
);
create index order_date on "order"(date);
create index order_symbol on "order"(symbol);
create index order_type on "order"(type);
create index order_status on "order"(status);
