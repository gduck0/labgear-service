
package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.repository.AccountRepository;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.service.ReservationService;

@SpringBootTest
@Transactional
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Test
    void reservationSaveAndReadWorks() {
        String email = UUID.randomUUID() + "@example.com";
        int equipmentId = ThreadLocalRandom.current()
                .nextInt(100_000_000, Integer.MAX_VALUE);

        Account account = accountRepository.save(
                new Account(email, "test-password-hash"));

        equipmentRepository.save(
                new Equipment(equipmentId, "TEST-CAMERA"));

        LocalDate startDate = LocalDate.of(2026, 11, 10);
        LocalDate endDate = LocalDate.of(2026, 11, 12);

        Reservation created = reservationService.createReservation(
                email, equipmentId, startDate, endDate);

        assertNotNull(created.getId());

        Reservation found = reservationService.getReservation(
                created.getId(), email);

        assertEquals(created.getId(), found.getId());
        assertEquals(account.getId(), found.getAccount().getId());
        assertEquals(equipmentId, found.getEquipment().getId());
        assertEquals(startDate, found.getStartDate());
        assertEquals(endDate, found.getEndDate());
    }
}
