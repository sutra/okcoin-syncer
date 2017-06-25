-- Create database schema.

-- org.oxerr.okcoin.rest.dto.Type
CREATE TYPE "type" AS ENUM ('buy', 'sell', 'buy_market', 'sell_market');

CREATE TABLE trade (
	tid bigint PRIMARY KEY,

	-- transaction time
	date timestamp with time zone NOT NULL,

	-- buy/sell
	"type" "type" NOT NULL,

	-- quantity in BTC (or LTC)
	amount numeric(16,8) NOT NULL,

	-- transaction price
	price numeric(32,8) NOT NULL
);
CREATE INDEX trade_date ON trade(date);
CREATE INDEX trade_type ON trade(type);

CREATE TABLE "order" (
	-- order ID
	id bigint PRIMARY KEY,

	date timestamp with time zone NOT NULL,

	symbol char(7) NOT NULL,

	-- buy_market = market buy order, sell_market = market sell order
	"type" "type" NOT NULL,

	-- order price
	price numeric(32,8) NOT NULL,

	-- for limit orders, the order quantity
	-- for market orders,the filled quantity
	amount numeric(16,8) NOT NULL,

	-- filled quantity
	deal_amount numeric(16,8) NOT NULL,

	-- -1 = cancelled,
	-- 0 = unfilled,
	-- 1 = partially filled,
	-- 2 = fully filled,
	-- 4 = cancel request in process
	status int NOT NULL,

	-- average transaction price
	avg_price numeric(32,8) NOT NULL
);
CREATE INDEX order_date ON "order"(date);
CREATE INDEX order_symbol ON "order"(symbol);
CREATE INDEX order_type ON "order"(type);
CREATE INDEX order_status ON "order"(status);

CREATE TABLE asset (
	id uuid PRIMARY KEY,
	date timestamp with time zone NOT NULL,
	net numeric(32, 8) NOT NULL,
	total numeric(32, 8) NOT NULL
);
CREATE INDEX asset_date ON asset(date);

CREATE TABLE fund (
	id uuid PRIMARY KEY,
	asset_id uuid NOT NULL REFERENCES asset (id),
	currency char(3) NOT NULL,
	free numeric(32, 8) NOT NULL,
	frozen numeric(32, 8) NOT NULL,
	borrow numeric(32, 8) NOT NULL,
	"union" numeric(32, 8) NOT NULL,
	UNIQUE(asset_id, currency)
);
