package com.toyota.restdataprovider.config;

import com.toyota.restdataprovider.entity.ForexUser;
import com.toyota.restdataprovider.entity.PricingPlan;
import com.toyota.restdataprovider.entity.Rate;
import com.toyota.restdataprovider.repository.RateRepository;
import com.toyota.restdataprovider.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupDataLoader implements CommandLineRunner {

    private final RateRepository rateRepository;
    private final UserRepository userRepository;
    private final InitialRateConfig initialRateConfig;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Loading Currency Pairs from Configuration file.");

        if (initialRateConfig == null || initialRateConfig.getRates() == null) {
            log.error("Cannot load currency pair from configuration file. Please check application.yml file.");
            throw new FileNotFoundException("Cannot load currency pair from configuration file. Please check application.yml file.");
        }

        Instant timeStamp = Instant.now();

        initialRateConfig.getRates().forEach((rateName, rateConfig) -> {
            rateRepository.save(new Rate(
                    rateName.toUpperCase(),
                    rateConfig.getBid(),
                    rateConfig.getAsk(),
                    timeStamp,
                    rateConfig.getMinLimit(),
                    rateConfig.getMaxLimit()
            ));
            log.info("{} has been loaded successfully.", rateName.toUpperCase());
        });

        log.info("All currency pairs have been loaded from 'application.yml' file.");

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
            log.info("Default user is added to the database: {}", user);
        }
    }
}
