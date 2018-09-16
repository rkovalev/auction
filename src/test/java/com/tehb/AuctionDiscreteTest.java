package com.tehb;

import com.tehb.AuctionDiscrete.RejectCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionDiscreteTest {

    private static final Logger logger = LoggerFactory.getLogger(AuctionDiscreteTest.class);

    private static AuctionDiscrete auctionDiscrete;

    @BeforeEach()
    void init() {
        auctionDiscrete = new AuctionDiscrete("SBER", 1_000_000, 1, 100, 1000);
    }

    @Test
    void rejectCodesTest() {
        assertEquals(RejectCode.INVALID_ORDER_STRING_FORMAT, auctionDiscrete.addOrder("test"));
        assertEquals(RejectCode.INVALID_ORDER_STRING_FORMAT, auctionDiscrete.addOrder("test 100 15.5"));
        assertEquals(RejectCode.INVALID_ORDER_STRING_FORMAT, auctionDiscrete.addOrder("B 100 a15.5"));
        assertEquals(RejectCode.INVALID_ORDER_STRING_FORMAT, auctionDiscrete.addOrder("B 100.1 15.5"));

        assertEquals(RejectCode.INVALID_ORDER_SIDE, auctionDiscrete.addOrder("V 100 15.5"));
        assertEquals(RejectCode.INVALID_ORDER_SIDE, auctionDiscrete.addOrder("b 100 15.5"));

        assertEquals(RejectCode.INVALID_ORDER_SIZE, auctionDiscrete.addOrder("S 0 15.5"));
        assertEquals(RejectCode.INVALID_ORDER_SIZE, auctionDiscrete.addOrder("S -10 15.5"));

        assertEquals(RejectCode.INVALID_ORDER_PRICE, auctionDiscrete.addOrder("S 20 0"));
        assertEquals(RejectCode.INVALID_ORDER_PRICE, auctionDiscrete.addOrder("S 20 -10"));

        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 1000 13.10"));
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 200 14.10"));

        assertEquals("0 n/a", auctionDiscrete.getAuctionResult());

        assertEquals(RejectCode.AUCTION_ORDERS_ACCEPT_ENDED, auctionDiscrete.addOrder("B 1000 13.10"));
        assertEquals(RejectCode.AUCTION_ORDERS_ACCEPT_ENDED, auctionDiscrete.addOrder("S 200 14.10"));
    }

    @Test
    void noMatchTest() {
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 100 10.00"));
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 150 10.10"));

        assertEquals("0 n/a", auctionDiscrete.getAuctionResult());
    }

    @Test
    void aggressiveSellTest() {
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 100 15.40"));
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 100 15.30"));
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 150 15.30"));

        assertEquals("150 15.30", auctionDiscrete.getAuctionResult());
    }

    @Test
    void aggressiveBuyTest() {
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 300 15.40"));
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 150 15.30"));
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 600 15.40"));

        assertEquals("450 15.40", auctionDiscrete.getAuctionResult());
    }

    @Test
    void avgPriceTest() {
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 150 15.35"));        // 1
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 100 15.40"));        // 2
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 100 15.30"));

        // as far price here is depends from 1 & 2 event sequence, taking avg price rounded up
        assertEquals("100 15.38", auctionDiscrete.getAuctionResult());
    }

    @Test
    void book1SampleTest() {
        /*
                     15.70  200
                100  15.60  150     <- auction 250
                100  15.50
                500  15.40  50
                     15.30  100
                300  15.10
         */
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 300 15.10"));
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 500 15.40"));        // -100 -50
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 100 15.50"));
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 100 15.60"));        // -100

        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 100 15.30"));        // F 15.40 100
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 50 15.40"));         // F 15.40  50
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 150 15.60"));        // F 15.60 100
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 200 15.70"));

        assertEquals("250 15.60", auctionDiscrete.getAuctionResult());
    }

    @Test
    void book2SampleTest() {
        /*
                 100  17.10
                 100  16.10  100      <- auction 400
                1000  15.10  100
                      14.10  200

         */
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 1000 15.10"));       // -200 -100
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 100 16.10"));        // -100
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 100 17.10"));

        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 200 14.10"));        // F 15.10 200
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 100 15.10"));        // F 15.10 100
        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 100 16.10"));        // F 16.10 100

        assertEquals("400 16.10", auctionDiscrete.getAuctionResult());
    }

    @Test
    void ordersLimitTest() {
        for (int i = 0; i < 999_999; i++) {
            auctionDiscrete.addOrder("B 1000 15.10");
        }

        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 1000 15.10"));
        assertEquals(RejectCode.AUCTION_ORDERS_LIMIT, auctionDiscrete.addOrder("B 1000 15.10"));
    }

    @Test
    void rejectOverflowTest() {
        auctionDiscrete = new AuctionDiscrete("SBER", 1_000_000, 1, 100, 1_000_000);
        for (int i = 0; i < 2146; i++) {
            auctionDiscrete.addOrder("B 1000000 15.10");
            auctionDiscrete.addOrder("S 1000000 16.10");
        }

        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("B 1000000 15.10"));
        assertEquals(RejectCode.AUCTION_ORDERS_QTY_LIMIT, auctionDiscrete.addOrder("B 1000000 15.10"));

        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 1000000 16.10"));
        assertEquals(RejectCode.AUCTION_ORDERS_QTY_LIMIT, auctionDiscrete.addOrder("S 1000000 16.10"));

        assertEquals(RejectCode.NONE, auctionDiscrete.addOrder("S 1000000 15.10"));                     // this will be filled
        assertEquals("1000000 15.10", auctionDiscrete.getAuctionResult());
    }

    @Test
    void performanceTest() {
        int ordersCount = 1_000_000;

        Random rnd = new Random();
        double[] prices = rnd.doubles(ordersCount).toArray();
        int[] sizes = rnd.ints(ordersCount, 1, 101).toArray();

        logger.info("Start adding orders");
        long addOrdersStartTime = System.currentTimeMillis();
        for (int i = 0; i < ordersCount; i++) {
            char side = rnd.nextBoolean() ? 'B' : 'S';
            auctionDiscrete.addOrder(side, 1 + 99 * prices[i], sizes[i]);
        }
        long addOrdersElapsedTime = System.currentTimeMillis() - addOrdersStartTime;
        logger.info("Add orders time, ms: {}", addOrdersElapsedTime);
        assertTrue(addOrdersElapsedTime <= 50);


        logger.info("Start auction");
        long auctionMatchStartTime = System.currentTimeMillis();
        String result = auctionDiscrete.getAuctionResult();
        long auctionMatchElapsedTime = System.currentTimeMillis() - auctionMatchStartTime;
        logger.info("Result: {} time, ms: {}", result, auctionMatchElapsedTime);
        assertTrue(auctionMatchElapsedTime <= 3);
    }
}