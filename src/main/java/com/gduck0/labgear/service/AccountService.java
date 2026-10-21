package com.gduck0.labgear.service;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.repository.AccountRepository;
import com.gduck0.labgear.exception.ResourceConflictException;


@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(String email, String password) {
        if(accountRepository.existsByEmail(email)){
            throw new ResourceConflictException("이미 가입된 이메일입니다.");
        }
        String passwordHash = passwordEncoder.encode(password);
        Account account = new Account(email, passwordHash);
        accountRepository.save(account);
    }
}
