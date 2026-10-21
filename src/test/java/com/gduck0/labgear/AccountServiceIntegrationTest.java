
package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.exception.ResourceConflictException;
import com.gduck0.labgear.repository.AccountRepository;
import com.gduck0.labgear.service.AccountService;

// 회원가입과 로그인 비밀번호 검증의 정상·실패 동작을 확인한다.
@SpringBootTest
@Transactional
class AccountServiceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerStoresPasswordHash() {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        String password = "abc123";

        accountService.register(email, password);

        Account savedAccount = accountRepository.findByEmail(email)
                .orElseThrow();

        assertEquals(email, savedAccount.getEmail());
        assertNotEquals(password, savedAccount.getPasswordHash());
        assertTrue(passwordEncoder.matches(
                password,
                savedAccount.getPasswordHash()
        ));
    }

    @Test
    void duplicateEmailDoesNotOverwriteExistingAccount() {
        String email = "test-" + UUID.randomUUID() + "@example.com";

        accountService.register(email, "firstPassword123!");

        Account original = accountRepository.findByEmail(email)
                .orElseThrow();

        String originalHash = original.getPasswordHash();

        assertThrows(ResourceConflictException.class, () -> {
            accountService.register(email, "secondPassword456!");
        });

        Account unchanged = accountRepository.findByEmail(email)
                .orElseThrow();

        assertEquals(original.getId(), unchanged.getId());
        assertEquals(originalHash, unchanged.getPasswordHash());
        assertTrue(passwordEncoder.matches(
                "firstPassword123!",
                unchanged.getPasswordHash()
        ));
    }
}
