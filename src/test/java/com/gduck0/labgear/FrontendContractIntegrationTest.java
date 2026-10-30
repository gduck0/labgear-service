
package com.gduck0.labgear;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FrontendContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Test
    void anonymousUserCanReadEquipment() throws Exception {
        mockMvc.perform(get("/equipment"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousUserCanReceiveCsrfToken() throws Exception {
        mockMvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.headerName").isString());
    }

    @Test
    void loggedInAccountResponseIncludesRole() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String password = "password123";
        accountService.register(email, password);

        MvcResult result = mockMvc.perform(post("/login")
                .with(csrf())
                .param("email", email)
                .param("password", password))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) result.getRequest().getSession(false);

        mockMvc.perform(get("/accounts/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
