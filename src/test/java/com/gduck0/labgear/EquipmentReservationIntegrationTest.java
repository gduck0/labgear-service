
package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

import com.gduck0.labgear.entity.EquipmentItem;
import com.gduck0.labgear.entity.EquipmentReservation;
import com.gduck0.labgear.entity.Student;
import com.gduck0.labgear.repository.SpringDataEquipmentRepository;
import com.gduck0.labgear.repository.SpringDataStudentRepository;
import com.gduck0.labgear.service.EquipmentReservationService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EquipmentReservationIntegrationTest {

    @Autowired
    private EquipmentReservationService reservationService;

    @Autowired
    private SpringDataStudentRepository studentRepository;

    @Autowired
    private SpringDataEquipmentRepository equipmentRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reservationSaveAndReadWorks() {
        int baseId = ThreadLocalRandom.current()
                .nextInt(100_000_000, 600_000_000);

        int reservationId = baseId;
        int studentId = baseId + 1;
        int equipmentItemId = baseId + 2;

        LocalDate startDate = LocalDate.of(2026, 10, 10);
        LocalDate endDate = LocalDate.of(2026, 10, 12);

        studentRepository.save(
                new Student(studentId, "TEST-STUDENT-DIRECT"));

        equipmentRepository.save(
                new EquipmentItem(equipmentItemId, "TEST-CAMERA-DIRECT"));

        reservationService.addReservation(
                reservationId, studentId, equipmentItemId,
                startDate, endDate);

        EquipmentReservation reservation =
                reservationService.getReservation(reservationId);

        assertEquals(reservationId, reservation.getId());
        assertEquals(studentId, reservation.getStudent().getId());
        assertEquals(equipmentItemId, reservation.getEquipmentItem().getId());
        assertEquals(startDate, reservation.getStartDate());
        assertEquals(endDate, reservation.getEndDate());
    }

    @Test
    void reservationPostAndGetWorks() throws Exception {
        int baseId = ThreadLocalRandom.current()
                .nextInt(100_000_000, 600_000_000);

        int reservationId = baseId;
        int studentId = baseId + 1;
        int equipmentItemId = baseId + 2;

        studentRepository.save(
                new Student(studentId, "TEST-STUDENT-HTTP"));

        equipmentRepository.save(
                new EquipmentItem(equipmentItemId, "TEST-CAMERA-HTTP"));

        String requestJson = """
            {
                "id": %d,
                "studentId": %d,
                "equipmentItemId": %d,
                "startDate": "2026-10-20",
                "endDate": "2026-10-22"
            }
            """.formatted(reservationId, studentId, equipmentItemId);

        mockMvc.perform(
                post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        mockMvc.perform(get("/reservations/{id}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.equipmentItemId").value(equipmentItemId))
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
                new EquipmentItem(firstEquipmentId, "TEST-CAMERA"));

        equipmentRepository.save(
                new EquipmentItem(secondEquipmentId, "TEST-LAPTOP"));

        String firstRequest = """
            {
                "id": %d,
                "studentId": %d,
                "equipmentItemId": %d,
                "startDate": "2026-10-20",
                "endDate": "2026-10-22"
            }
            """.formatted(reservationId, studentId, firstEquipmentId);

        mockMvc.perform(
                post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isCreated());

        String duplicateRequest = """
            {
                "id": %d,
                "studentId": %d,
                "equipmentItemId": %d,
                "startDate": "2026-10-25",
                "endDate": "2026-10-27"
            }
            """.formatted(reservationId, studentId, secondEquipmentId);

        mockMvc.perform(
                post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("이미 존재하는 예약 ID입니다."));

        mockMvc.perform(get("/reservations/{id}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.equipmentItemId").value(firstEquipmentId))
                .andExpect(jsonPath("$.startDate").value("2026-10-20"))
                .andExpect(jsonPath("$.endDate").value("2026-10-22"));
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
                new EquipmentItem(equipmentId, "TEST-CAMERA"));

        reservationService.addReservation(
                reservationId,
                studentId,
                equipmentId,
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 22));

        mockMvc.perform(delete("/equipment/{id}", equipmentId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        EquipmentItem item =
                equipmentRepository.findById(equipmentId).orElseThrow();

        EquipmentReservation reservation =
                reservationService.getReservation(reservationId);

        assertEquals("TEST-CAMERA", item.getName());
        assertEquals(equipmentId, reservation.getEquipmentItem().getId());
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
                new EquipmentItem(equipmentId, "TEST-AVAILABLE-CAMERA"));

        mockMvc.perform(delete("/equipment/{id}", equipmentId))
                .andExpect(status().isOk());

        assertFalse(equipmentRepository.findById(equipmentId).isPresent());
    }
}
