package com.toyota;

import com.toyota.cache.CacheService;
import com.toyota.cache.Impl.RedisServiceImpl;
import com.toyota.calculation.CalculationService;
import com.toyota.calculation.Impl.PythonCalculator;
import com.toyota.config.ApplicationConfig;
import com.toyota.publisher.Impl.KafkaServiceImpl;
import com.toyota.publisher.KafkaService;
import com.toyota.service.CoordinatorService;
import com.toyota.service.Impl.CoordinatorImpl;
import com.toyota.service.Impl.EmailSenderImpl;
import com.toyota.service.Impl.RateManagerImpl;
import com.toyota.service.MailSender;
import com.toyota.service.RateManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.Context;

public class ForexDataCollector {

    public static final Logger log = LogManager.getLogger(ForexDataCollector.class);

    public static void main(String[] args) {

        try {
            ApplicationConfig appConfig = ApplicationConfig.getInstance();

            CacheService redisService = new RedisServiceImpl(appConfig);
            CalculationService calculationService = new PythonCalculator();
            KafkaService kafkaService = new KafkaServiceImpl(appConfig);

            RateManager rateManager = new RateManagerImpl(
                    kafkaService,
                    redisService,
                    calculationService
            );

            MailSender mailSender = new EmailSenderImpl(appConfig);

            CoordinatorService coordinatorService = new CoordinatorImpl(
                    rateManager,
                    mailSender,
                    appConfig
            );

        } catch (RuntimeException e) {
            log.error("Application run failed. Error: {}",e.getMessage());
            throw new RuntimeException(e);
        }
    }
}