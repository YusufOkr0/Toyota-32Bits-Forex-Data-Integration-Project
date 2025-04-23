package com.toyota.restdataprovider.repository;

import com.toyota.restdataprovider.entity.Rate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface RateRepository extends CrudRepository<Rate,String> {

    Optional<Rate> findByNameIgnoreCase(String rateName);

}
