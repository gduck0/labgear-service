package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Student;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.StudentRepository;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

// 예약 API의 생성·조회, 중복·날짜 충돌 및 없는 기자재의 오류를 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Test
    void reservationPostAndGetWorks() throws Exception {
        int baseId = ThreadLocalRandom.current()
                .nextInt(100_000_000, 600_000_000);

        int reservationId = baseId;
        int studentId = baseId + 1;
        int equipmentId = baseId + 2;

        studentRepository.save(
                new Student(studentId, "TEST-STUDENT-HTTP"));

        equipmentRepository.save(
                new Equipment(equipmentId, "TEST-CAMERA-HTTP"));

        String requestJson = """
            {
                "id": %d,
                "studentId": %d,
                "equipmentId": %d,
                "startDate": "2026-10-20",
                "endDate": "2026-10-22"
            }
            """.formatted(reservationId, studentId, equipmentId);

        mockMvc.perform(
                post("/reservations").with(user("test@example.com").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        mockMvc.perform(get("/reservations/{id}", reservationId).with(user("test@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.equipmentId").value(equipmentId))
                .andExpect(jsonPath("$.startDate").value("2026-10-20"))
                .andExpect(jsonPath("$.endDate").value("2026-10-22"));
    }

    @Test
    void duplicateReservationIdReturns409AndPreservesOriginal()
            throws Exception {

        int baseId = ThreadLocalRandom.current()
                .nextInt(100_000_000, 600_000_000);

        int reservationId = baseId;
        int studentId = baseId + 1;
        int firstEquipmentId = baseId + 2;
        int secondEquipmentId = baseId + 3;

        studentRepository.save(
                new Student(studentId, "TEST-STUDENT"));

        equipmentRepository.save(
                new Equipment(firstEquipmentId, "TEST-CAMERA"));

        equipmentRepository.save(
                new Equipment(secondEquipmentId, "TEST-LAPTOP"));

        String firstRequest = """
            {
                "id": %d,
                "studentId": %d,
                "equipmentId": %d,
                "startDate": "2026-10-20",
                "endDate": "2026-10-22"
            }
            """.formatted(reservationId, studentId, firstEquipmentId);

        mockMvc.perform(
                post("/reservations").with(user("test@example.com").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isCreated());

        String duplicateRequest = """
            {
                "id": %d,
                "studentId": %d,
                "equipmentId": %d,
                "startDate": "2026-10-25",
                "endDate": "2026-10-27"
            }
            """.formatted(reservationId, studentId, secondEquipmentId);

        mockMvc.perform(
                post("/reservations").with(user("test@example.com").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("이미 존재하는 예약 ID입니다."));

        mockMvc.perform(get("/reservations/{id}", reservationId).with(user("test@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.equipmentId").value(firstEquipmentId))
                .andExpect(jsonPath("$.startDate").value("2026-10-20"))
                .andExpect(jsonPath("$.endDate").value("2026-10-22"));
    }

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
        equipmentRepository.save(new Equipment(equipmentId, "TEST-CAMERA"));

        String firstRequest = requestJson(
                firstId, studentId, equipmentId,
                "2026-10-20", "2026-10-22"
        );

        mockMvc.perform(post("/reservations").with(user("test@example.com").roles("USER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstRequest))
            .andExpect(status().isCreated());

        String conflictRequest = requestJson(
                conflictId, studentId, equipmentId,
                "2026-10-22", "2026-10-23"
        );

        mockMvc.perform(post("/reservations").with(user("test@example.com").roles("USER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(conflictRequest))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(get("/reservations/{id}", conflictId).with(user("test@example.com").roles("USER")))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/reservations/{id}", firstId).with(user("test@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startDate").value("2026-10-20"))
            .andExpect(jsonPath("$.endDate").value("2026-10-22"));

        String nextRequest = requestJson(
                nextId, studentId, equipmentId,
                "2026-10-23", "2026-10-24"
        );

        mockMvc.perform(post("/reservations").with(user("test@example.com").roles("USER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(nextRequest))
            .andExpect(status().isCreated());
    }

    @Test
    void missingEquipmentReturns404AndDoesNotCreateReservation() throws Exception {
        int baseId = ThreadLocalRandom.current()
                .nextInt(100_000_000, 600_000_000);

        int studentId = baseId + 1;
        int equipmentId = baseId + 2;
        int reservationId = baseId + 3;

        studentRepository.save(new Student(studentId, "TEST-STUDENT"));

        assertFalse(equipmentRepository.existsById(equipmentId));

        String requestJson = requestJson(
                reservationId, studentId, equipmentId,
                "2026-10-20", "2026-10-22");

        mockMvc.perform(post("/reservations").with(user("test@example.com").roles("USER")).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("기자재를 찾을 수 없습니다. ID: " + equipmentId));

        mockMvc.perform(get("/reservations/{id}", reservationId).with(user("test@example.com").roles("USER")))
                .andExpect(status().isNotFound());
    }

    private String requestJson(
            int id, int studentId, int equipmentId,
            String startDate, String endDate) {

        return """
            {
                "id": %d,
                "studentId": %d,
                "equipmentId": %d,
                "startDate": "%s",
                "endDate": "%s"
            }
            """.formatted(
                id, studentId, equipmentId, startDate, endDate
            );
    }
}
