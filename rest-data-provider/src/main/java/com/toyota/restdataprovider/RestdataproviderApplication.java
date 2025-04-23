package com.toyota.restdataprovider;

import com.toyota.restdataprovider.config.InitialRateConfig;
import com.toyota.restdataprovider.entity.Rate;
import com.toyota.restdataprovider.repository.RateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableScheduling
public class RestdataproviderApplication implements CommandLineRunner {

	@Autowired
	private RateRepository rateRepository;
	@Autowired
	private InitialRateConfig initialRateConfig;

	public static void main(String[] args) {
		SpringApplication.run(RestdataproviderApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		LocalDateTime timeStamp = LocalDateTime.now();
		initialRateConfig.getRates().forEach((rateName,rateConfig) -> {
			rateRepository.save(new Rate(
					"REST_" + rateName.toUpperCase(),
					rateConfig.getBid(),
					rateConfig.getAsk(),
					timeStamp,
					rateConfig.getMinLimit(),
					rateConfig.getMaxLimit()
			));
		});
	}
}
