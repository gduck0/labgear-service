package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.entity.Student;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.StudentRepository;
import com.gduck0.labgear.service.EquipmentService;
import com.gduck0.labgear.service.ReservationService;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

// 기자재 API의 생성·오류 응답·삭제 조건과 데이터 보존을 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EquipmentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void creatingEquipmentReturns201() throws Exception {
        int id = ThreadLocalRandom.current().nextInt(100_000_000, Integer.MAX_VALUE);

        String requestJson = """
            {
                "id": %d,
                "name": "TEST-NEW-CAMERA"
            }
            """.formatted(id);

        mockMvc.perform(post("/equipment").with(user("test@example.com").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        assertEquals("TEST-NEW-CAMERA",
                equipmentRepository.findById(id).orElseThrow().getName());
    }

    @Test
    void duplicateEquipmentReturns409AndPreservesData() throws Exception {
        int id = ThreadLocalRandom.current()
                .nextInt(100_000_000, Integer.MAX_VALUE);

        assertFalse(equipmentRepository.findById(id).isPresent());
    
        equipmentService.createEquipment(
                new Equipment(id, "TEST-CAMERA")
        );

        String requestJson = """
            {
                "id": %d,
                "name": "TEST-LAPTOP"
            }
            """.formatted(id);

        mockMvc.perform(
                post("/equipment").with(user("test@example.com").roles("USER")).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message")
                    .value("이미 존재하는 기자재 ID입니다."));

        Equipment unchanged =
                equipmentService.getEquipment(id);

        assertEquals("TEST-CAMERA", unchanged.getName());
    }

    @Test
    void invalidEquipmentRequestReturns400() throws Exception {
        String requestJson = """
            {
                "id": 0,
                "name": " "
            }
            """;

        mockMvc.perform(
                post("/equipment").with(user("test@example.com").roles("USER")).with(csrf())
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
                .with(user("test@example.com").roles("USER"))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message")
                .value("기자재를 찾을 수 없습니다. ID: " + missingId));
        }

    @Test
    void deletingReservedEquipmentReturns409AndPreservesData()
            throws Exception {

        int baseId = ThreadLocalRandom.current()
                .nextInt(100_000_000, 600_000_000);

        int reservationId = baseId;
        int studentId = baseId + 1;
        int equipmentId = baseId + 2;

        studentRepository.save(
                new Student(studentId, "TEST-STUDENT"));

        equipmentRepository.save(
                new Equipment(equipmentId, "TEST-CAMERA"));

        reservationService.createReservation(
                reservationId,
                studentId,
                equipmentId,
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 22));

        mockMvc.perform(delete("/equipment/{id}", equipmentId).with(user("test@example.com").roles("USER")).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        Equipment item =
                equipmentRepository.findById(equipmentId).orElseThrow();

        Reservation reservation =
                reservationService.getReservation(reservationId);

        assertEquals("TEST-CAMERA", item.getName());
        assertEquals(equipmentId, reservation.getEquipment().getId());
        assertEquals(
                LocalDate.of(2026, 10, 20),
                reservation.getStartDate());
        assertEquals(
                LocalDate.of(2026, 10, 22),
                reservation.getEndDate());
    }

    @Test
    void deletingUnreservedEquipmentSucceeds() throws Exception {
        int equipmentId = ThreadLocalRandom.current()
                .nextInt(600_000_000, 900_000_000);

        equipmentRepository.save(
                new Equipment(equipmentId, "TEST-AVAILABLE-CAMERA"));

        mockMvc.perform(delete("/equipment/{id}", equipmentId).with(user("test@example.com").roles("USER")).with(csrf()))
                .andExpect(status().isOk());

        assertFalse(equipmentRepository.findById(equipmentId).isPresent());
    }
}
