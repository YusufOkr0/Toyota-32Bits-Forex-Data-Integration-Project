package com.toyota.restdataprovider.service.abstracts;

import io.github.bucket4j.Bucket;

public interface RateLimitService {

    Bucket getUsersBucket(String username);

    void removeUserBucket(String username);

}
