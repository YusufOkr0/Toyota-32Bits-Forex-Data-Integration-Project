package com.toyota.restdataprovider;

import com.toyota.restdataprovider.config.InitialRateConfig;
import com.toyota.restdataprovider.entity.ForexUser;
import com.toyota.restdataprovider.entity.PricingPlan;
import com.toyota.restdataprovider.entity.Rate;
import com.toyota.restdataprovider.repository.RateRepository;
import com.toyota.restdataprovider.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class RestdataproviderApplication implements CommandLineRunner {

	@Autowired
	private RateRepository rateRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private InitialRateConfig initialRateConfig;

	@Autowired
	private PasswordEncoder passwordEncoder;

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
			String currency_pair = "REST_" + rateName.toUpperCase();
			log.info("{} has been loaded successfully.",currency_pair);
		});

		log.info("All currency pairs has been loaded from 'application.yml' file.");


		if (userRepository.count() == 0) {
			ForexUser user = ForexUser.builder()
					.username("YusufOkr0")
					.email("admin@example.com")
					.password(passwordEncoder.encode("YusufOkr0"))
					.pricingPlan(PricingPlan.PREMIUM)
					.createdAt(LocalDateTime.now())
					.updatedAt(LocalDateTime.now())
					.build();
			userRepository.save(user);
			log.info("Default user is added to the database: {}",user);
		}



	}

}
