package com.toyota.restdataprovider.controller;

import com.toyota.restdataprovider.entity.Rate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rates")
public class RateController {

    @GetMapping(value = "/{rateName}")
    ResponseEntity<Rate> getCurrencyPair(@PathVariable(name = "rateName") String rateName){


        return null;
    }
}
