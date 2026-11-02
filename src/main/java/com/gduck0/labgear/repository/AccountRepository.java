package com.gduck0.labgear.repository;

import com.gduck0.labgear.entity.Account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByEmail(String email);

    Optional<Account> findByEmail(String email);
}
