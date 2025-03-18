package com.toyota.restdataprovider.controller;

import com.toyota.restdataprovider.entity.Rate;
import com.toyota.restdataprovider.service.abstracts.RateService;
import lombok.RequiredArgsConstructor;
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
public class RateController {


    private final RateService rateService;


    @GetMapping(value = "/{rateName}",produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Rate> getCurrencyPair(@PathVariable(name = "rateName") String rateName){

        Rate rate = rateService.getCurrencyPair(rateName);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(rate);

    }



}










