package com.toyota.restdataprovider.service.concretes;

import com.toyota.restdataprovider.entity.ForexUser;
import com.toyota.restdataprovider.exception.UserNotFoundException;
import com.toyota.restdataprovider.repository.UserRepository;
import com.toyota.restdataprovider.service.abstracts.RateLimitService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final ProxyManager<String> proxyManager;
    private final UserRepository userRepository;


    @Override
    public Bucket getUsersBucket(String username) {
        return proxyManager.builder().build(username,() -> createBucketForUser(username));
    }

    @Override
    public void removeUserBucket(String username){
        proxyManager.removeProxy(username);
    }


    private BucketConfiguration createBucketForUser(String username) {
        ForexUser forexUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(String.format("There is no such a user in the system with the username: {%s}", username)));

        int limitPerMinute = forexUser.getPricingPlan().getLimitPerMinute();

        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(limitPerMinute).refillIntervally(limitPerMinute, Duration.ofMinutes(1L)))
                .build();
    }
}
