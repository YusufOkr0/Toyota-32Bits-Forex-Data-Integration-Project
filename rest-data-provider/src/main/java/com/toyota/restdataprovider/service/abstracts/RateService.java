package com.toyota.restdataprovider.service.abstracts;


import com.toyota.restdataprovider.dtos.response.RateDto;

public interface RateService {

    RateDto getCurrencyPair(String rateName);

}
