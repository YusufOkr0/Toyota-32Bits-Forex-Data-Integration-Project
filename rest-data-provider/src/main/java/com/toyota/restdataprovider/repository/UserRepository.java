package com.toyota.restdataprovider.repository;

import com.toyota.restdataprovider.entity.ForexUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<ForexUser,Long> {
}
