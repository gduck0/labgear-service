package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.entity.Student;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.StudentRepository;
import com.gduck0.labgear.service.ReservationService;

// Service의 예약 저장과 조회 결과를 DB와 함께 확인한다.
@SpringBootTest
@Transactional
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Test
    void reservationSaveAndReadWorks() {
        int baseId = ThreadLocalRandom.current()
                .nextInt(100_000_000, 600_000_000);

        int reservationId = baseId;
        int studentId = baseId + 1;
        int equipmentId = baseId + 2;

        LocalDate startDate = LocalDate.of(2026, 10, 10);
        LocalDate endDate = LocalDate.of(2026, 10, 12);

        studentRepository.save(
                new Student(studentId, "TEST-STUDENT-DIRECT"));

        equipmentRepository.save(
                new Equipment(equipmentId, "TEST-CAMERA-DIRECT"));

        reservationService.createReservation(
                reservationId, studentId, equipmentId,
                startDate, endDate);

        Reservation reservation =
                reservationService.getReservation(reservationId);

        assertEquals(reservationId, reservation.getId());
        assertEquals(studentId, reservation.getStudent().getId());
        assertEquals(equipmentId, reservation.getEquipment().getId());
        assertEquals(startDate, reservation.getStartDate());
        assertEquals(endDate, reservation.getEndDate());
    }
}
