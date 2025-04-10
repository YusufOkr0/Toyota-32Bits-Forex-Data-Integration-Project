package com.toyota.service;

import com.toyota.entity.Rate;
import java.util.List;


public interface RedisService {

    void saveRawRate(String platformName, String rateName, Rate rate);

    List<Rate> getAllRawRatesByRateName(String rateName);

}