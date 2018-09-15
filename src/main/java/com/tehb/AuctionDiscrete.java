package com.tehb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

public class AuctionDiscrete {
    private static final Logger logger = LoggerFactory.getLogger(AuctionDiscrete.class);

    public enum RejectCode {
        NONE,

        INVALID_ORDER_STRING_FORMAT,
        INVALID_ORDER_SIZE,
        INVALID_ORDER_PRICE,
        INVALID_ORDER_SIDE,

        AUCTION_ORDERS_LIMIT,
        AUCTION_ORDERS_ACCEPT_ENDED
    }

    private final String ticker;
    private final int ordersLimit;
    private final int minPriceRub, maxPriceRub;
    private final int maxSizeLimit;

    private final int[] sumOrderQtysBuySide, sumOrderQtysSellSide;
    private int maxBuyIdx = Integer.MIN_VALUE, minSellIdx = Integer.MAX_VALUE;
    private int ordersCounter;

    private double auctionPrice = -1;     // -1 auction matching is not started yet, 0 optimal price calculated but there was no match
    private int auctionSize;

    public AuctionDiscrete(String ticker, int ordersLimit, int minPriceRub, int maxPriceRub, int maxSizeLimit) {
        if(ordersLimit <= 0) {
            throw new RuntimeException("Invalid orders limit for ticker: " + ticker);    // die fast in case problems with initial params
        }

        if(minPriceRub <= 0 || maxPriceRub <= 0 || minPriceRub > maxPriceRub) {
            throw new RuntimeException("Invalid price limits for ticker: " + ticker);
        }

        if(maxSizeLimit <= 0) {
            throw new RuntimeException("Invalid size limits for ticker: " + ticker);
        }

        this.ticker = ticker;
        this.ordersLimit = ordersLimit;
        this.minPriceRub = minPriceRub;
        this.maxPriceRub = maxPriceRub;
        this.maxSizeLimit = maxSizeLimit;

        // each element index represent specific discreet price. Example element 173 = 1 ruble 73 kop
        sumOrderQtysBuySide = new int[maxPriceRub * 100 + 1];
        sumOrderQtysSellSide = new int[maxPriceRub * 100 + 1];

        logger.info("Created new instance of discreet auction: {}", this);
    }

    public RejectCode addOrder(String orderStr) {
        String[] orderProps = orderStr.split(" ");
        if(orderProps.length != 3) {
            return RejectCode.INVALID_ORDER_STRING_FORMAT;
        }

        if(orderProps[0].length() != 1) {
            return RejectCode.INVALID_ORDER_STRING_FORMAT;
        }

        try {
            int size = Integer.parseInt(orderProps[1]);
            double price = Double.parseDouble(orderProps[2]);

            return addOrder(orderProps[0].charAt(0), price, size);
        } catch (Exception e) {
            logger.error("Can't parse order string {}", orderStr);
            return RejectCode.INVALID_ORDER_STRING_FORMAT;
        }
    }

    public synchronized RejectCode addOrder(char side, double price, int size) {
        if(auctionPrice >= 0) {
            return RejectCode.AUCTION_ORDERS_ACCEPT_ENDED;
        }

        if(ordersCounter == ordersLimit) {
            return RejectCode.AUCTION_ORDERS_LIMIT;
        }

        if(price < minPriceRub || price > maxPriceRub) {
            return RejectCode.INVALID_ORDER_PRICE;
        }

        if(size < 1 || size > maxSizeLimit) {
            return RejectCode.INVALID_ORDER_SIZE;
        }

        int priceIdx = (int)Math.round(price / 0.01d);

        if('B' == side) {
            sumOrderQtysBuySide[priceIdx] += size;
            maxBuyIdx = Math.max(maxBuyIdx, priceIdx);
        } else if('S' == side) {
            sumOrderQtysSellSide[priceIdx] += size;
            minSellIdx = Math.min(minSellIdx, priceIdx);
        } else {
            return RejectCode.INVALID_ORDER_SIDE;
        }

        ordersCounter++;
        return RejectCode.NONE;
    }

    public synchronized String getAuctionResult() {
        if(auctionPrice == -1) {
            findPriceAndSize();
        }

        return auctionPrice != 0 ? String.format("%d %.2f", auctionSize, auctionPrice) : "0 n/a";
    }

    private void findPriceAndSize() {
        if(auctionPrice >= 0) {
            logger.warn("Auction order matching for ticker: {} is already triggered", ticker);
            return;
        }

        auctionPrice = 0;

        if(maxBuyIdx == Integer.MIN_VALUE || minSellIdx == Integer.MAX_VALUE) {
            return;                                         // one of the sides is empty, do nothing
        }

        if(maxBuyIdx < minSellIdx) {
            return;                                         // order prices are not intersected
        }

        int aggressiveSellToFill = 0;
        for(int i = minSellIdx; i <= maxBuyIdx; i++) {
            aggressiveSellToFill += sumOrderQtysSellSide[i];

            int buyQty = sumOrderQtysBuySide[i];

            if(buyQty >= aggressiveSellToFill) {
                auctionSize += aggressiveSellToFill;
                aggressiveSellToFill = 0;
            } else {
                auctionSize += buyQty;
                aggressiveSellToFill -= buyQty;
            }
        }

        auctionPrice =  0.01d * (aggressiveSellToFill > 0 ? maxBuyIdx : minSellIdx);
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        sw.append("Ticker: ");
        sw.append(ticker);
        sw.append(" orders limit: ");
        sw.append(String.valueOf(ordersLimit));
        return sw.toString();
    }
}
