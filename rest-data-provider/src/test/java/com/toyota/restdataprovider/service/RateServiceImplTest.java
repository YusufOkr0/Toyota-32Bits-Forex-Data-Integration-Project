package com.toyota.restdataprovider.service;

import com.toyota.restdataprovider.dtos.response.RateDto;
import com.toyota.restdataprovider.entity.Rate;
import com.toyota.restdataprovider.exception.CurrencyPairNotFoundException;
import com.toyota.restdataprovider.repository.RateRepository;
import com.toyota.restdataprovider.service.concretes.RateServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class RateServiceImplTest {

    @InjectMocks
    private RateServiceImpl rateService;

    @Mock
    private RateRepository rateRepository;


    @Test
    void whenGetCurrencyPairWithValidCurrencyPairName_ThenReturnRateDto() {
        String validRateName = "TEST_RATE";
        BigDecimal bid = new BigDecimal("99.99");
        BigDecimal ask = new BigDecimal("100.00");
        Instant timestamp = Instant.now();

        Rate existsRate = Rate.builder()
                .name(validRateName)
                .bid(bid)
                .ask(ask)
                .timestamp(timestamp)
                .build();

        Mockito.when(rateRepository.findByNameIgnoreCase(validRateName)).thenReturn(Optional.ofNullable(existsRate));

        RateDto rateDto = rateService.getCurrencyPair(validRateName);


        Assertions.assertEquals(validRateName, rateDto.getName());
        Assertions.assertEquals(bid, rateDto.getBid());
        Assertions.assertEquals(ask, rateDto.getAsk());
        Assertions.assertEquals(timestamp, rateDto.getTimestamp());

        Mockito.verify(rateRepository, Mockito.times(1)).findByNameIgnoreCase(Mockito.any(String.class));

    }


    @Test
    void whenGetCurrencyPairWithNonExistsCurrencyPairName_ThenThrowsCurrencyPairNotFoundException() {
        String invalidRateName = "INVALID_RATE_NAME";

        Mockito.when(rateRepository.findByNameIgnoreCase(invalidRateName)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                CurrencyPairNotFoundException.class,
                () -> rateService.getCurrencyPair(invalidRateName)
        );

        Mockito.verify(rateRepository, Mockito.times(1)).findByNameIgnoreCase(Mockito.any(String.class));
    }


}
