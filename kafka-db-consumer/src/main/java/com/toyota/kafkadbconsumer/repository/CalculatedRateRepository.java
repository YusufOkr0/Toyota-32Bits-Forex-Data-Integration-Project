package com.toyota.kafkadbconsumer.repository;


import com.toyota.kafkadbconsumer.entity.CalculatedRate;
import org.springframework.data.jpa.repository.JpaRepository;



public interface CalculatedRateRepository extends JpaRepository<CalculatedRate,Long> {
}
