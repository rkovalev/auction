package com.tehb;

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
    void noMatchTest() {
        auctionDiscrete.addOrder("B 100 10.00");
        auctionDiscrete.addOrder("S 150 10.10");

        assertEquals("0 n/a", auctionDiscrete.getAuctionResult());
    }

    @Test
    void aggressiveSell() {
        auctionDiscrete.addOrder("B 100 15.40");
        auctionDiscrete.addOrder("B 100 15.30");
        auctionDiscrete.addOrder("S 150 15.30");

        assertEquals("150 15.30", auctionDiscrete.getAuctionResult());
    }

    @Test
    void performanceTest() {
        logger.info("Start adding orders");
        Random rnd = new Random();
        for(int i = 0; i < 1_000_000; i++) {
            char side = rnd.nextBoolean() ? 'B': 'S';
            double price = 1 + 99 * rnd.nextDouble();
            int size = 1 + rnd.nextInt(100);
            auctionDiscrete.addOrder(side, price, size);
        }

        logger.info("Start auction");
        long startTime = System.currentTimeMillis();
        String result = auctionDiscrete.getAuctionResult();
        long elapsedTime = (System.currentTimeMillis() - startTime);
        logger.info("Result: {} time, ms: {}", result, elapsedTime);
        assertTrue(elapsedTime <= 3);
    }
}