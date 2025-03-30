package com.toyota;

import com.toyota.coordinator.CoordinatorService;
import com.toyota.coordinator.Impl.CoordinatorImpl;

public class ForexDataCollector {
    public static void main(String[] args) {

        CoordinatorService coordinatorService = new CoordinatorImpl();
    }
}