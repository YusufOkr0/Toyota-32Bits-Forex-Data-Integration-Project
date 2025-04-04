package com.toyota.broadcast;

import com.toyota.entity.Rate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FxDataPublisher {


    private static final BigDecimal MAXIMUM_RATE_CHANGE = new BigDecimal("0.004");
    private static final BigDecimal MINIMUM_RATE_CHANGE = new BigDecimal("0.001");

    private static final BigDecimal SPIKE_PERCENTAGE = new BigDecimal("0.011");  // % 1.1  FOR NOW. CHECK LATER
    private static final int SPIKE_INTERVAL = 30;
    private int spikeCounter = 0;

    private final int PUBLISH_FREQUENCY;
    private final List<Rate> rates;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, Set<SocketChannel>> subscriptions;

    private static final Logger logger = LogManager.getLogger(FxDataPublisher.class);



    public FxDataPublisher(
            ConcurrentHashMap<String, Set<SocketChannel>> subscriptions,
            List<Rate> initial_rates,
            int publishFrequency) {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.subscriptions = subscriptions;
        this.rates = initial_rates;
        this.PUBLISH_FREQUENCY = publishFrequency;

        logger.info("FxDataPublisher initialized.");
        logger.info("Publish Frequency: {} ms", PUBLISH_FREQUENCY);
        logger.info("Spike Interval: every {} cycles", SPIKE_INTERVAL);
        logger.info("Spike Percentage: {}", SPIKE_PERCENTAGE);
        logger.info("Initial rates: {}", initial_rates.stream().map(Rate::getRateName).collect(Collectors.toSet()));
    }

    public void startBroadcast() {
        scheduler.scheduleWithFixedDelay(
                this::publishRates,
                1, // Initial delay
                PUBLISH_FREQUENCY,
                TimeUnit.MILLISECONDS
        );
        logger.info("FX data broadcast scheduler started with fixed delay of {} ms.", PUBLISH_FREQUENCY);
    }

    private void publishRates() {
        spikeCounter++;

        for (Rate rate : rates) {
            updateRate(rate);

            String message = formatRateMessage(rate);

            Set<SocketChannel> clients = subscriptions.get(rate.getRateName());

            if (clients != null && !clients.isEmpty()) {
                for (SocketChannel client : clients) {
                    sendToClient(client, message);
                }
            }
        }
    }

    private void updateRate(Rate rate) {

        BigDecimal changePercentage = determineChangePercentage();

        BigDecimal spread = rate
                .getAsk()
                .subtract(rate.getBid());  // SPREAD IS CONSTANT.

        BigDecimal newBid;
        BigDecimal newAsk;

        newBid = rate
                .getBid()
                .multiply(BigDecimal.ONE.add(changePercentage));

        newBid = applyRateBounds(
                newBid,
                rate.getMinLimit(),
                rate.getMaxLimit()
        );

        newAsk = newBid.add(spread);


        rate.setBid(newBid.setScale(16, RoundingMode.HALF_UP));
        rate.setAsk(newAsk.setScale(16,RoundingMode.HALF_UP));

        rate.setTimestamp(LocalDateTime.now());
        logger.trace("Rate {} updated. New Bid: {}, New Ask: {}", rate.getRateName(), rate.getBid(), rate.getAsk());
    }



    private BigDecimal determineChangePercentage(){

        BigDecimal changePercentage;

        if (spikeCounter % SPIKE_INTERVAL == 0) {
            changePercentage = SPIKE_PERCENTAGE;
            logger.debug("Spike triggered! Applying spike percentage: {}", changePercentage);
        } else {
            changePercentage = MAXIMUM_RATE_CHANGE
                    .subtract(MINIMUM_RATE_CHANGE)
                    .multiply(BigDecimal.valueOf(Math.random()))
                    .add(MINIMUM_RATE_CHANGE);
        }

        if(Math.random() < 0.5){
            changePercentage = changePercentage.negate();
        }

        return changePercentage;
    }


    private BigDecimal applyRateBounds(BigDecimal bidValue, BigDecimal minLimit, BigDecimal maxLimit) {
        if (bidValue.compareTo(minLimit) < 0) {
            return minLimit;
        } else if (bidValue.compareTo(maxLimit) > 0) {
            return maxLimit;
        }
        return bidValue;
    }

    private String formatRateMessage(Rate rate) {
        return String.format(
                "%s|B:%s|A:%s|T:%s",
                rate.getRateName(),
                rate.getBid(),
                rate.getAsk(),
                rate.getTimestamp()
        );
    }

    private void sendToClient(SocketChannel client, String message) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap((message + "\r\n").getBytes());
            client.write(buffer);
        } catch (IOException e) {
            logger.error("Failed to send message to client: {}", message, e);
        }
    }

}