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
