
package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.gduck0.labgear.service.AccountService;

// 실제 DB 계정으로 Spring Security 로그인·세션·로그아웃 동작을 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Test
    void successfulLoginKeepsAuthenticationAcrossRequests() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        String password = "password123";

        accountService.register(email, password);

        MvcResult loginResult = mockMvc.perform(post("/login")
                .with(csrf())
                .param("email", email)
                .param("password", password))
                .andExpect(status().isNoContent())
                .andExpect(authenticated().withUsername(email))
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(get("/accounts/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";

        accountService.register(email, "correctPassword123");

        mockMvc.perform(post("/login")
                .with(csrf())
                .param("email", email)
                .param("password", "wrongPassword456"))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    @Test
    void unknownEmailIsRejected() throws Exception {
        String email = "missing-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/login")
                .with(csrf())
                .param("email", email)
                .param("password", "password123"))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    @Test
    void unauthenticatedUserCannotAccessMe() throws Exception {
        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        String email = "logout-" + UUID.randomUUID() + "@example.com";
        String password = "password123";

        accountService.register(email, password);

        MvcResult loginResult = mockMvc.perform(post("/login")
                .with(csrf())
                .param("email", email)
                .param("password", password))
                .andExpect(authenticated())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(post("/logout")
                .session(session)
                .with(csrf()))
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());

        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isUnauthorized());
    }
}
