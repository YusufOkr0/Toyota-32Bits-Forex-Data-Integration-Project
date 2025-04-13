package com.toyota.cache.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toyota.cache.CacheService;
import com.toyota.config.ApplicationConfig;
import com.toyota.entity.CalculatedRate;
import com.toyota.entity.Rate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.ArrayList;
import java.util.List;

public class RedisServiceImpl implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(RedisServiceImpl.class);

    private static final long TTL_IN_SECONDS = 1800L;
    private static final String RAW_RATES_KEY_PREFIX = "RawRates::";
    private static final String CALCULATED_RATES_KEY_PREFIX = "CalculatedRates::";

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final ApplicationConfig appConfig;

    public RedisServiceImpl(ApplicationConfig appConfig) {
        this.appConfig = appConfig;
        this.jedisPool = configureJedisPool();
        this.objectMapper = configureObjectMapper();
    }

    @Override
    public void saveRawRate(String platformName, String rateName, Rate rate) {  // RawRates::TCP::USDTRY

        String redisKey = RAW_RATES_KEY_PREFIX + platformName + "::" + rateName;
        try (Jedis jedis = jedisPool.getResource()) {

            String rateInJson = objectMapper.writeValueAsString(rate);

            jedis.setex(redisKey,TTL_IN_SECONDS,rateInJson);

        } catch (JedisConnectionException | JsonProcessingException e) {
            logger.error("Error when save raw rates to the redis.",e);
        }
    }

    @Override
    public void saveCalculatedRate(String rateName, CalculatedRate rate) {

        String redisKey = CALCULATED_RATES_KEY_PREFIX + "::" + rateName;
        try(Jedis jedis = jedisPool.getResource()){

            String rateInJson = objectMapper.writeValueAsString(rate);

            jedis.setex(redisKey,TTL_IN_SECONDS,rateInJson);

        } catch (JedisConnectionException | JsonProcessingException e) {
            logger.error("Error when save calculated rates to the redis.",e);
        }
    }


    @Override
    public List<Rate> getAllRawRatesByRateName(String rateName) {
        List<Rate> rates = new ArrayList<>();
        String pattern = RAW_RATES_KEY_PREFIX + "*::" + rateName;

        try (Jedis jedis = jedisPool.getResource()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams params = new ScanParams().match(pattern).count(20);

            do {
                ScanResult<String> result = jedis.scan(cursor, params);
                for (String key : result.getResult()) {
                    String json = jedis.get(key);
                    if (json != null) {
                        try {
                            rates.add(objectMapper.readValue(json, Rate.class));
                        } catch (JsonProcessingException e) {
                            logger.warn("Could not parse JSON for key: {}", key, e);
                        }
                    }
                }
                cursor = result.getCursor();
            } while (!cursor.equals(ScanParams.SCAN_POINTER_START));
        } catch (JedisConnectionException e) {
            logger.error("Redis connection error : {}", e.getMessage());
        }

        return rates;
    }


    private void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.debug("Redis connection pool closed.");
        }
    }

    private JedisPool configureJedisPool() {

        String redisHost = appConfig.getValue("redis.host");
        int redisPort = appConfig.getIntValue("redis.port");

        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(2);

        return new JedisPool(poolConfig,redisHost,redisPort);
    }

    private ObjectMapper configureObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

}