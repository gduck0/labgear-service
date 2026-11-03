package com.gduck0.labgear;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.entity.ReservationStatus;
import com.gduck0.labgear.repository.AccountRepository;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.ReservationRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservedPeriodsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void returnsOnlyBlockingPeriodsWithoutPersonalInformation() throws Exception {
        Account owner = saveAccount();
        Equipment equipment = saveEquipment();

        saveReservation(owner, equipment, "2027-04-05", "2027-04-07", ReservationStatus.ACTIVE);
        saveReservation(owner, equipment, "2027-04-09", "2027-04-10", ReservationStatus.RENTED);
        saveReservation(owner, equipment, "2027-04-12", "2027-04-15", ReservationStatus.RETURNED);
        saveReservation(owner, equipment, "2027-04-17", "2027-04-18", ReservationStatus.CANCELED);

        mockMvc.perform(get("/equipment/{id}/reserved-periods", equipment.getId())
                        .param("startDate", "2027-04-01")
                        .param("endDate", "2027-04-30")
                        .with(user("viewer@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].startDate").value("2027-04-05"))
                .andExpect(jsonPath("$[0].endDate").value("2027-04-07"))
                .andExpect(jsonPath("$[1].startDate").value("2027-04-09"))
                .andExpect(jsonPath("$[1].endDate").value("2027-04-10"))
                .andExpect(jsonPath("$[2].startDate").value("2027-04-12"))
                .andExpect(jsonPath("$[2].endDate").value("2027-04-15"))
                .andExpect(jsonPath("$[0].accountEmail").doesNotExist())
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].status").doesNotExist());
    }

    @Test
    void filtersByEquipmentAndIncludesPeriodBoundary() throws Exception {
        Account owner = saveAccount();
        Equipment target = saveEquipment();
        Equipment other = saveEquipment();

        saveReservation(owner, target, "2027-05-10", "2027-05-12", ReservationStatus.ACTIVE);
        saveReservation(owner, target, "2027-05-20", "2027-05-22", ReservationStatus.ACTIVE);
        saveReservation(owner, other, "2027-05-15", "2027-05-17", ReservationStatus.ACTIVE);

        mockMvc.perform(get("/equipment/{id}/reserved-periods", target.getId())
                        .param("startDate", "2027-05-12")
                        .param("endDate", "2027-05-19")
                        .with(user("viewer@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].startDate").value("2027-05-10"))
                .andExpect(jsonPath("$[0].endDate").value("2027-05-12"));
    }

    @Test
    void returnsEmptyListWhenEquipmentHasNoReservations() throws Exception {
        Equipment equipment = saveEquipment();

        mockMvc.perform(get("/equipment/{id}/reserved-periods", equipment.getId())
                        .param("startDate", "2027-05-01")
                        .param("endDate", "2027-05-31")
                        .with(user("viewer@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void missingEquipmentReturns404() throws Exception {
        int missingId = ThreadLocalRandom.current().nextInt(100_000_000, Integer.MAX_VALUE);

        mockMvc.perform(get("/equipment/{id}/reserved-periods", missingId)
                        .param("startDate", "2027-05-01")
                        .param("endDate", "2027-05-31")
                        .with(user("viewer@example.com").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void reversedDatesReturn400() throws Exception {
        Equipment equipment = saveEquipment();

        mockMvc.perform(get("/equipment/{id}/reserved-periods", equipment.getId())
                        .param("startDate", "2027-05-31")
                        .param("endDate", "2027-05-01")
                        .with(user("viewer@example.com").roles("USER")))
                .andExpect(status().isBadRequest());
    }

    private Account saveAccount() {
        return accountRepository.save(new Account(
                "calendar-" + UUID.randomUUID() + "@example.com",
                "test-password-hash"));
    }

    private Equipment saveEquipment() {
        int id = ThreadLocalRandom.current().nextInt(100_000_000, Integer.MAX_VALUE);
        return equipmentRepository.save(new Equipment(id, "TEST-CALENDAR"));
    }

    private void saveReservation(
            Account owner,
            Equipment equipment,
            String startDate,
            String endDate,
            ReservationStatus status) {
        Reservation reservation = new Reservation(
                owner, equipment, LocalDate.parse(startDate), LocalDate.parse(endDate));

        switch (status) {
            case ACTIVE -> { }
            case CANCELED -> reservation.cancel();
            case RENTED -> reservation.rent();
            case RETURNED -> {
                reservation.rent();
                reservation.returnEquipment();
            }
        }

        reservationRepository.save(reservation);
    }
}
