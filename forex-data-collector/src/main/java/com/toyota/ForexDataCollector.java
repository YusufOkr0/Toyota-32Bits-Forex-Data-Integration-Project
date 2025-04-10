package com.toyota;

import com.toyota.calculation.CalculationService;
import com.toyota.calculation.Impl.PythonCalculator;
import com.toyota.config.ApplicationConfig;
import com.toyota.service.CoordinatorService;
import com.toyota.service.Impl.CoordinatorImpl;
import com.toyota.service.Impl.RateManagerImpl;
import com.toyota.service.Impl.RedisServiceImpl;
import com.toyota.service.RateManager;
import com.toyota.service.RedisService;

public class ForexDataCollector {
    public static void main(String[] args) {

        ApplicationConfig appConfig = ApplicationConfig.getInstance();

        RedisService redisService = new RedisServiceImpl(appConfig);

        CalculationService calculationService = new PythonCalculator();
        RateManager rateManager = new RateManagerImpl(redisService,calculationService);

        CoordinatorService coordinatorService = new CoordinatorImpl(rateManager,appConfig);

    }
}