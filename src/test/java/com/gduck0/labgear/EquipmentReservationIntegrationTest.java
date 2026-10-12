
package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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
import com.gduck0.labgear.repository.EquipmentRepository;
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
    private EquipmentRepository equipmentRepository;

    @Autowired
    private MockMvc mockMvc;

    // 1. 기존 Service + DB 통합 테스트
    @Test
    void reservationSaveAndReadWorks() {
        int reservationId = 9701;
        int studentId = 9702;
        int equipmentItemId = 9703;

        LocalDate startDate = LocalDate.of(2026, 10, 10);
        LocalDate endDate = LocalDate.of(2026, 10, 12);

        studentRepository.save(
            new Student(studentId, "TEST-STUDENT-DIRECT")
        );

        equipmentRepository.save(
            new EquipmentItem(equipmentItemId, "TEST-CAMERA-DIRECT")
        );

        reservationService.addReservation(
            reservationId,
            studentId,
            equipmentItemId,
            startDate,
            endDate
        );

        EquipmentReservation reservation =
            reservationService.getReservation(reservationId)
                .orElseThrow();

        assertEquals(reservationId, reservation.getId());
        assertEquals(studentId, reservation.getStudent().getId());
        assertEquals(equipmentItemId, reservation.getEquipmentItem().getId());
        assertEquals(startDate, reservation.getStartDate());
        assertEquals(endDate, reservation.getEndDate());
    }

    // 2. MockMvc를 통한 실제 Controller 경로 검증
    @Test
    void reservationPostAndGetWorks() throws Exception {
        int reservationId = 9801;
        int studentId = 9802;
        int equipmentItemId = 9803;

        // 예약에 필요한 학생과 기자재를 먼저 준비
        studentRepository.save(
            new Student(studentId, "TEST-STUDENT-HTTP")
        );

        equipmentRepository.save(
            new EquipmentItem(equipmentItemId, "TEST-CAMERA-HTTP")
        );

        // POST 요청에 사용할 JSON
        String requestJson = """
            {
                "id": 9801,
                "studentId": 9802,
                "equipmentItemId": 9803,
                "startDate": "2026-10-20",
                "endDate": "2026-10-22"
            }
            """;

        // 예약 생성 API → HTTP 201, 빈 응답 확인
        mockMvc.perform(
                post("/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
            .andExpect(status().isCreated())
            .andExpect(content().string(""));

        // 예약 조회 API → HTTP 200, JSON 필드 확인
        mockMvc.perform(
                get("/reservations/{id}", reservationId)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(reservationId))
            .andExpect(jsonPath("$.studentId").value(studentId))
            .andExpect(jsonPath("$.equipmentItemId").value(equipmentItemId))
            .andExpect(jsonPath("$.startDate").value("2026-10-20"))
            .andExpect(jsonPath("$.endDate").value("2026-10-22"));
    }
}
