package com.toyota.restdataprovider.service.concretes;

import com.toyota.restdataprovider.dtos.request.UpdatePlanRequest;
import com.toyota.restdataprovider.dtos.response.UpdatePlanResponse;
import com.toyota.restdataprovider.entity.ForexUser;
import com.toyota.restdataprovider.entity.PricingPlan;
import com.toyota.restdataprovider.exception.InvalidPricingPlanException;
import com.toyota.restdataprovider.exception.UserNotFoundException;
import com.toyota.restdataprovider.repository.UserRepository;
import com.toyota.restdataprovider.service.abstracts.RateLimitService;
import com.toyota.restdataprovider.service.abstracts.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;


    @Override
    public UpdatePlanResponse updatePricingPlanByUsername(String username, UpdatePlanRequest updatePlanRequest) {

        ForexUser forexUser = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(String.format("There is no such a user in the system with the username: {%s}", username)));

        String comingPlan = updatePlanRequest.getNewPricingPlan().toUpperCase();

        try {
            PricingPlan updatedPlan = PricingPlan.valueOf(comingPlan);
            forexUser.setPricingPlan(updatedPlan);
            forexUser.setUpdatedAt(LocalDateTime.now());
            userRepository.save(forexUser);

            log.info("Pricing plan updated. Username: {}, New Plan: {}, Updated At: {}",
                    forexUser.getUsername(),
                    forexUser.getPricingPlan().name(),
                    forexUser.getUpdatedAt()
            );

            rateLimitService.removeUserBucket(username);    // DELETE USER'S BUCKET TO RELOAD WITH THE NEW LIMIT.

        }catch (IllegalArgumentException exception){
            throw new InvalidPricingPlanException(String.format("Given plan: %s does not exists", comingPlan));
        }

        return new UpdatePlanResponse(
                forexUser.getUsername(),
                forexUser.getPricingPlan().name()
        );
    }
}
