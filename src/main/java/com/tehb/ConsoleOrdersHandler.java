package com.tehb;

import com.tehb.AuctionDiscrete.RejectCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class ConsoleOrdersHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleOrdersHandler.class);

    private AuctionDiscrete auctionDiscrete;

    private ConsoleOrdersHandler() {
        init();
    }

    public static void main(String[] args) {
        new ConsoleOrdersHandler();
    }

    private void init() {
        auctionDiscrete = new AuctionDiscrete("SBER", 1_000_000, 1, 100, 1000);

        showUsage();

        try(Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.isEmpty()) {
                    logger.info("Auction result:");
                    logger.info(auctionDiscrete.getAuctionResult());
                    break;
                }
                userInputHandler(line.toUpperCase());
            }
        }
    }

    private void userInputHandler(String line) {
        if("Q".equals(line)) {
            System.exit(0);
        }

        RejectCode rejectCode = auctionDiscrete.addOrder(line);
        if(rejectCode != RejectCode.NONE) {
            produceOrderRejectMsg(line, rejectCode);
        }
    }

    private void produceOrderRejectMsg(String orderMsg, RejectCode rejectCode) {
        logger.error("Can't handle: {} reason: {}", orderMsg, rejectCode);
    }

    private void showUsage() {
        logger.info("Please enter orders in format: side (B|S) qty side");
        logger.info("example: B 120 10.15");
        logger.info("or empty line for starting auction matching or Q for program termination");
    }
}