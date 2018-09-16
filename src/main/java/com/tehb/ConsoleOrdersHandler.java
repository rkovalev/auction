package com.tehb;

import com.tehb.AuctionDiscrete.RejectCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

class ConsoleOrdersHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleOrdersHandler.class);

    private final AuctionDiscrete auctionDiscrete;

    private ConsoleOrdersHandler() {
        auctionDiscrete = new AuctionDiscrete("SBER", 1_000_000, 1, 100, 1000);
        showUsage();
        handleUserInput();
    }

    public static void main(String[] args) {
        new ConsoleOrdersHandler();
    }

    private void handleUserInput() {
        try(Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.isEmpty()) {
                    logger.info("Auction result: {}", auctionDiscrete.getAuctionResult());
                    break;
                }

                RejectCode rejectCode = auctionDiscrete.addOrder(line);
                if(rejectCode != RejectCode.NONE) {
                    produceOrderRejectMsg(line.toUpperCase(), rejectCode);
                }
            }
        }
    }

    private void produceOrderRejectMsg(String orderMsg, RejectCode rejectCode) {
        logger.error("Can't handle: {} reason: {}", orderMsg, rejectCode);
    }

    private void showUsage() {
        logger.info("Please enter orders in format: side (B|S) qty side");
        logger.info("example: B 120 10.15");
        logger.info("or empty line for starting auction matching");
    }
}