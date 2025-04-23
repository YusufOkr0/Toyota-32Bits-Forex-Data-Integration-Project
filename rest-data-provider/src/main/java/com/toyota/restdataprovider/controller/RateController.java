package com.toyota.restdataprovider.controller;

import com.toyota.restdataprovider.dtos.response.RateDto;
import com.toyota.restdataprovider.entity.Rate;
import com.toyota.restdataprovider.service.abstracts.RateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
@Slf4j
public class RateController {

   private final RateService rateService;

    @GetMapping(value = "/{rateName}",produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<RateDto> getCurrencyPair(@PathVariable(name = "rateName") String rateName){
        log.info("Incoming rate request for rate: {}",rateName);

        RateDto rate = rateService.getCurrencyPair(rateName);

        return ResponseEntity
                .status(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body(rate);

    }



}










