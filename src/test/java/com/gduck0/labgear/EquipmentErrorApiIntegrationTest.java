
package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.gduck0.labgear.repository.SpringDataEquipmentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EquipmentErrorApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataEquipmentRepository equipmentRepository;

    @Test
    void invalidEquipmentRequestReturns400() throws Exception {
        String requestJson = """
            {
                "id": 0,
                "name": " "
            }
            """;

        mockMvc.perform(
                post("/equipment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message")
                    .value("입력값이 올바르지 않습니다."));
    }

    @Test
    void missingEquipmentReturns404() throws Exception {
        int missingId = ThreadLocalRandom.current()
                .nextInt(100_000_000, Integer.MAX_VALUE);

        assertFalse(equipmentRepository.findById(missingId).isPresent());

        mockMvc.perform(
                get("/equipment/{id}", missingId)
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message")
                    .value("기자재를 찾을 수 없습니다. ID: " + missingId));
    }
}
