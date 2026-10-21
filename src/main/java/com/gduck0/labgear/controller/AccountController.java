package com.gduck0.labgear.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

import com.gduck0.labgear.dto.AccountCreateRequest;
import com.gduck0.labgear.service.AccountService;

import java.security.Principal;
import java.util.Map;

@RestController
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody AccountCreateRequest request) {
        accountService.register(request.getEmail(), request.getPassword());
    }

    @GetMapping("/accounts/me")
    public Map<String, String> me(Principal principal) {
        return Map.of("email", principal.getName());
    }
}
