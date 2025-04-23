package com.toyota.restdataprovider;

import com.toyota.restdataprovider.config.InitialRateConfig;
import com.toyota.restdataprovider.entity.Rate;
import com.toyota.restdataprovider.repository.RateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;

@Slf4j
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

		log.info("Loading Currency Pairs from Configuration file.");

		LocalDateTime timeStamp = LocalDateTime.now();

		if(initialRateConfig == null || initialRateConfig.getRates() == null){
			log.error("Cannot load currency pair from configuration file. Please check application.yml file.");
			throw new FileNotFoundException("Cannot load currency pair from configuration file. Please check application.yml file.");
		}


		initialRateConfig.getRates().forEach((rateName,rateConfig) -> {
			rateRepository.save(new Rate(
					"REST_" + rateName.toUpperCase(),			// HERE I PUT A PREFIX TO THE RATE NAME.
					rateConfig.getBid(),
					rateConfig.getAsk(),
					timeStamp,
					rateConfig.getMinLimit(),
					rateConfig.getMaxLimit()
			));

		log.info("All currency pairs has been loaded successfully from 'application.yml' file.");

		});



	}
}
