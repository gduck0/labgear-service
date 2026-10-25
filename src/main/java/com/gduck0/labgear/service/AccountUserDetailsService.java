package com.gduck0.labgear.service;

import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.repository.AccountRepository;

@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final AccountRepository accountRepository;

    public AccountUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Account account = accountRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException ("존재하지 않는 이메일입니다."));
        return User.withUsername(account.getEmail()).password(account.getPasswordHash()).roles("USER").build();
    }
}
