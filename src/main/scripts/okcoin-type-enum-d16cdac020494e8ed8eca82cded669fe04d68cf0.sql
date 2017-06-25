-- Upgrade database schema.
-- Use enum instead of varchar to store order.type and trade.type.

-- org.oxerr.okcoin.rest.dto.Type
CREATE TYPE "type" AS ENUM ('buy', 'sell', 'buy_market', 'sell_market');

ALTER TABLE "order" ALTER COLUMN "type" SET DATA TYPE "type" USING "type"::"type";
ALTER TABLE "trade" ALTER COLUMN "type" SET DATA TYPE "type" USING "type"::"type";
