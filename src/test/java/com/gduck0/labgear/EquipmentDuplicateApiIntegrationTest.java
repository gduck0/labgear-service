package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.gduck0.labgear.entity.EquipmentItem;
import com.gduck0.labgear.repository.SpringDataEquipmentRepository;
import com.gduck0.labgear.service.EquipmentService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EquipmentDuplicateApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private SpringDataEquipmentRepository equipmentRepository;

    @Test
    void duplicateEquipmentReturns409AndPreservesData() throws Exception {
        int id = ThreadLocalRandom.current()
                .nextInt(100_000_000, Integer.MAX_VALUE);

        assertFalse(equipmentRepository.findById(id).isPresent());
    
        equipmentService.addEquipment(
                new EquipmentItem(id, "TEST-CAMERA")
        );

        String requestJson = """
            {
                "id": %d,
                "name": "TEST-LAPTOP"
            }
            """.formatted(id);

        mockMvc.perform(
                post("/equipment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message")
                    .value("이미 존재하는 기자재 ID입니다."));

        EquipmentItem unchanged =
                equipmentService.getEquipmentById(id);

        assertEquals("TEST-CAMERA", unchanged.getName());
    }
}
