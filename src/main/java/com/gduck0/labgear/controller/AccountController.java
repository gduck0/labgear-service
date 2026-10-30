package com.gduck0.labgear.controller;

import java.security.Principal;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gduck0.labgear.dto.AccountCreateRequest;
import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.exception.ResourceNotFoundException;
import com.gduck0.labgear.repository.AccountRepository;
import com.gduck0.labgear.service.AccountService;

@RestController
public class AccountController {

    private final AccountService accountService;
    private final AccountRepository accountRepository;

    public AccountController(AccountService accountService, AccountRepository accountRepository) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody AccountCreateRequest request) {
        accountService.register(request.getEmail(), request.getPassword());
    }

    @GetMapping("/accounts/me")
    public Map<String, String> me(Principal principal) {
        Account account = accountRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("계정을 찾을 수 없습니다."));

        // 변경: 프론트엔드에서 USER와 ADMIN 화면을 구분할 수 있도록 역할 반환
        return Map.of("email", account.getEmail(), "role", account.getRole().name());
    }
}
