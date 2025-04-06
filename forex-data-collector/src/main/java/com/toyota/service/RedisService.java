package com.toyota.service;

import com.toyota.entity.Rate;


public interface RedisService {

    void saveRawRate(String platformName, String rateName, Rate rate);

    void shutdown();
}