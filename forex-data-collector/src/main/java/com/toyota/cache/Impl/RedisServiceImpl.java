package com.toyota.cache.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toyota.cache.CacheService;
import com.toyota.config.ApplicationConfig;
import com.toyota.entity.CalculatedRate;
import com.toyota.entity.Rate;
import com.toyota.exception.ConnectionException;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.ArrayList;
import java.util.List;

public class RedisServiceImpl implements CacheService {

    private static final Logger logger = LogManager.getLogger(RedisServiceImpl.class);

    private static final long TTL_IN_SECONDS = 300L;
    private static final String RAW_RATES_KEY_PREFIX = "RawRates";
    private static final String CALCULATED_RATES_KEY_PREFIX = "CalculatedRates";

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final ApplicationConfig appConfig;

    public RedisServiceImpl(ApplicationConfig appConfig) {
        this.appConfig = appConfig;
        this.jedisPool = configureJedisPool();
        this.objectMapper = configureObjectMapper();
        testRedisConnection();
    }

    @Override
    public void saveRawRate(String platformName, String rateName, Rate rate) {  // RawRates::TCP::USDTRY

        String redisKey = RAW_RATES_KEY_PREFIX + "::" + platformName + "::" + rateName;
        try (Jedis jedis = jedisPool.getResource()) {

            String rateInJson = objectMapper.writeValueAsString(rate);

            jedis.setex(redisKey, TTL_IN_SECONDS, rateInJson);
            logger.debug("RedisServiceImpl: Successfully saved raw rate for key: {} with TTL: {} seconds", redisKey, TTL_IN_SECONDS);

        } catch (JedisConnectionException | JsonProcessingException e) {
            logger.error("RedisServiceImpl: Error when save raw rates to the redis. Exception Message: {}.", e.getMessage());
        }
    }

    @Override
    public void saveCalculatedRate(String rateName, CalculatedRate rate) {

        String redisKey = CALCULATED_RATES_KEY_PREFIX + "::" + rateName;
        try (Jedis jedis = jedisPool.getResource()) {

            String rateInJson = objectMapper.writeValueAsString(rate);

            jedis.setex(redisKey, TTL_IN_SECONDS, rateInJson);
            logger.debug("RedisServiceImpl: Successfully saved calculated rate for key: {} with TTL: {} seconds", redisKey, TTL_IN_SECONDS);
        } catch (JedisConnectionException | JsonProcessingException e) {
            logger.error("RedisServiceImpl: Error when save calculated rates to the redis. Exception Message: {}", e.getMessage());
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
                            logger.error("RedisServiceImpl: Could not parse JSON for key: {}. Exception Message: {}", key, e.getMessage());
                        }
                    }
                }
                cursor = result.getCursor();
            } while (!cursor.equals(ScanParams.SCAN_POINTER_START));

        } catch (JedisConnectionException e) {
            logger.error("RedisServiceImpl: Redis connection error : {}", e.getMessage());
        }

        logger.debug("RedisServiceImpl: Fetched {} raw rates for rateName: {}", rates.size(), rateName);
        return rates;
    }


    private void testRedisConnection() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.ping();
        } catch (JedisConnectionException e) {
            throw new ConnectionException("Unable to connect to Redis.");
        }
    }

    private JedisPool configureJedisPool() {

        String redisHost = appConfig.getValue("redis.host");
        int redisPort = appConfig.getIntValue("redis.port");

        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(15);
        poolConfig.setMaxIdle(6);
        poolConfig.setMinIdle(4);

        return new JedisPool(poolConfig, redisHost, redisPort);
    }

    private ObjectMapper configureObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

}