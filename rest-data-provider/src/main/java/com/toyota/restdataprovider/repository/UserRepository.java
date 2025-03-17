package com.toyota.restdataprovider.repository;

import com.toyota.restdataprovider.entity.ForexUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<ForexUser,Long> {

    Optional<ForexUser> findByUsername(String username);

    Optional<ForexUser> findByEmail(String email);

}
