package com.toyota.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toyota.config.ConfigUtil;
import com.toyota.entity.Rate;
import com.toyota.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class RedisServiceImpl implements RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisServiceImpl.class);

    private static final String RAW_RATES_KEY = "RawRates::";
    private static final String CALCULATED_RATES_KEY = "CalculatedRates::";

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;


    public RedisServiceImpl() {
        this.jedisPool = configureJedisPool();
        this.objectMapper = configureObjectMapper();
    }

    @Override
    public void saveRawRate(String platformName, String rateName, Rate rate) {
        try (Jedis jedis = jedisPool.getResource()) {

            System.out.println(rate.toString());
            String redisKey = RAW_RATES_KEY + platformName;
            String hashKey = platformName + rateName;

            String rateInJson = objectMapper.writeValueAsString(rate);

            jedis.hset(redisKey,hashKey,rateInJson);

        } catch (JedisConnectionException | JsonProcessingException e) {
            logger.error("Error when save raw rates to the redis.",e);
        }
    }


    @Override
    public void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            System.out.println("Redis connection pool closed.");
        }
    }

    private JedisPool configureJedisPool() {

        String redisHost = ConfigUtil.getValue("redis.host");
        int redisPort = ConfigUtil.getIntValue("redis.port");

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