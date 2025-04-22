package com.toyota.kafkadbconsumer.repository;


import com.toyota.kafkadbconsumer.entity.RawRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawRateRepository extends JpaRepository<RawRate,Long> {

}
