package com.toyota.restdataprovider.service.abstracts;


import com.toyota.restdataprovider.entity.Rate;

public interface RateService {

    Rate getCurrencyPair(String rateName);

}
