package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.gduck0.labgear.entity.EquipmentItem;
import com.gduck0.labgear.entity.Student;
import com.gduck0.labgear.repository.SpringDataEquipmentRepository;
import com.gduck0.labgear.repository.SpringDataStudentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationConflictApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataStudentRepository studentRepository;

    @Autowired
    private SpringDataEquipmentRepository equipmentRepository;

    @Test
    void overlappingReservationReturns409AndAdjacentDatesWork()
            throws Exception {

        int baseId = ThreadLocalRandom.current()
                .nextInt(100_000_000, 600_000_000);

        int studentId = baseId + 1;
        int equipmentId = baseId + 2;
        int firstId = baseId + 3;
        int conflictId = baseId + 4;
        int nextId = baseId + 5;

        studentRepository.save(new Student(studentId, "TEST-STUDENT"));
        equipmentRepository.save(new EquipmentItem(equipmentId, "TEST-CAMERA"));

        String firstRequest = requestJson(
                firstId, studentId, equipmentId,
                "2026-10-20", "2026-10-22"
        );

        mockMvc.perform(post("/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstRequest))
            .andExpect(status().isCreated());

        String conflictRequest = requestJson(
                conflictId, studentId, equipmentId,
                "2026-10-22", "2026-10-23"
        );

        mockMvc.perform(post("/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(conflictRequest))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(get("/reservations/{id}", conflictId))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/reservations/{id}", firstId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startDate").value("2026-10-20"))
            .andExpect(jsonPath("$.endDate").value("2026-10-22"));

        String nextRequest = requestJson(
                nextId, studentId, equipmentId,
                "2026-10-23", "2026-10-24"
        );

        mockMvc.perform(post("/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nextRequest))
            .andExpect(status().isCreated());
    }

    private String requestJson(
            int id, int studentId, int equipmentId,
            String startDate, String endDate) {

        return """
            {
                "id": %d,
                "studentId": %d,
                "equipmentItemId": %d,
                "startDate": "%s",
                "endDate": "%s"
            }
            """.formatted(
                id, studentId, equipmentId, startDate, endDate
            );
    }
}
