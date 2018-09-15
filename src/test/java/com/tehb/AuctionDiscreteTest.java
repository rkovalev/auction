package com.tehb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionDiscreteTest {

    @Test
    void noMatchTest() {
        AuctionDiscrete auctionDiscrete = new AuctionDiscrete("SBER", 1_000_000, 1, 100, 1000);

        auctionDiscrete.addOrder("B 100 10.00");
        auctionDiscrete.addOrder("S 150 10.10");

        assertEquals("0 n/a", auctionDiscrete.getAuctionResult());
    }

    @Test
    void aggressiveSell() {
        AuctionDiscrete auctionDiscrete = new AuctionDiscrete("SBER", 1_000_000, 1, 100, 1000);

        auctionDiscrete.addOrder("B 100 15.40");
        auctionDiscrete.addOrder("B 100 15.30");
        auctionDiscrete.addOrder("S 150 15.30");

        assertEquals("150 15.30", auctionDiscrete.getAuctionResult());
    }
}