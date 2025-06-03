package com.toyota.restdataprovider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class RestdataproviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestdataproviderApplication.class, args);
    }

}
