
package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.repository.AccountRepository;

// 회원가입 API의 성공, 중복 이메일 및 입력 오류를 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerReturns201AndStoresPasswordHash() throws Exception {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        String password = "password123";

        String requestJson = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/accounts").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated());

        Account saved = accountRepository.findByEmail(email)
                .orElseThrow();

        assertEquals(email, saved.getEmail());
        assertNotEquals(password, saved.getPasswordHash());
        assertTrue(passwordEncoder.matches(
                password, saved.getPasswordHash()));
    }

    @Test
    void duplicateEmailReturns409AndPreservesAccount() throws Exception {
        String email = "test-" + UUID.randomUUID() + "@example.com";

        String firstRequest = """
                {
                    "email": "%s",
                    "password": "firstPassword123"
                }
                """.formatted(email);

        String secondRequest = """
                {
                    "email": "%s",
                    "password": "secondPassword456"
                }
                """.formatted(email);

        mockMvc.perform(post("/accounts").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstRequest))
                .andExpect(status().isCreated());

        Account original = accountRepository.findByEmail(email)
                .orElseThrow();

        Long originalId = original.getId();
        String originalHash = original.getPasswordHash();

        mockMvc.perform(post("/accounts").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondRequest))
                .andExpect(status().isConflict());

        Account unchanged = accountRepository.findByEmail(email)
                .orElseThrow();

        assertEquals(originalId, unchanged.getId());
        assertEquals(originalHash, unchanged.getPasswordHash());
    }

    @Test
    void invalidEmailReturns400() throws Exception {
        String requestJson = """
                {
                    "email": "invalid-email",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/accounts").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortPasswordReturns400() throws Exception {
        String requestJson = """
                {
                    "email": "student@example.com",
                    "password": "123"
                }
                """;

        mockMvc.perform(post("/accounts").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }
}
