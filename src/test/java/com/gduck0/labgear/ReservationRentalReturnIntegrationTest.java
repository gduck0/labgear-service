
package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.entity.ReservationStatus;
import com.gduck0.labgear.exception.ResourceConflictException;
import com.gduck0.labgear.repository.AccountRepository;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.ReservationRepository;
import com.gduck0.labgear.service.ReservationService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationRentalReturnIntegrationTest {

    private static final LocalDate START = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(30);
    private static final LocalDate END = START.plusDays(2);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private final List<Long> accountIds = new ArrayList<>();
    private final List<Integer> equipmentIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long accountId : accountIds) {
            reservationRepository.deleteAll(
                    reservationRepository.findByAccount_IdOrderByIdDesc(accountId));
        }
        for (Integer equipmentId : equipmentIds) {
            equipmentRepository.deleteById(equipmentId);
        }
        for (Long accountId : accountIds) {
            accountRepository.deleteById(accountId);
        }
    }

    @Test
    void adminCanRentAndReturnReservation() throws Exception {
        Account owner = saveAccount();
        Reservation reservation = reserve(owner, saveEquipment());

        mockMvc.perform(
                patch("/reservations/{id}/rent", reservation.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertStatus(reservation.getId(), ReservationStatus.RENTED);

        mockMvc.perform(
                get("/reservations/{id}", reservation.getId())
                        .with(user(owner.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RENTED"));

        mockMvc.perform(
                patch("/reservations/{id}/return", reservation.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertStatus(reservation.getId(), ReservationStatus.RETURNED);
    }

    @Test
    void normalUserCannotRentOrReturn() throws Exception {
        Account owner = saveAccount();
        Reservation reservation = reserve(owner, saveEquipment());

        mockMvc.perform(
                patch("/reservations/{id}/rent", reservation.getId())
                        .with(user(owner.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                patch("/reservations/{id}/return", reservation.getId())
                        .with(user(owner.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertStatus(reservation.getId(), ReservationStatus.ACTIVE);
    }

    @Test
    void invalidTransitionsReturn409() throws Exception {
        Account owner = saveAccount();
        Reservation reservation = reserve(owner, saveEquipment());
        Long id = reservation.getId();

        adminPatch(id, "return", 409);
        assertStatus(id, ReservationStatus.ACTIVE);

        adminPatch(id, "rent", 204);
        adminPatch(id, "rent", 409);

        mockMvc.perform(
                patch("/reservations/{id}/cancel", id)
                        .with(user(owner.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isConflict());

        assertStatus(id, ReservationStatus.RENTED);

        adminPatch(id, "return", 204);
        adminPatch(id, "return", 409);
        adminPatch(id, "rent", 409);

        assertStatus(id, ReservationStatus.RETURNED);
    }

    @Test
    void missingReservationReturns404() throws Exception {
        adminPatch(Long.MAX_VALUE, "rent", 404);
        adminPatch(Long.MAX_VALUE, "return", 404);
    }

    @Test
    void rentedAndReturnedDatesRemainBlocked() throws Exception {
        Account owner = saveAccount();
        Account other = saveAccount();
        Equipment equipment = saveEquipment();
        Reservation reservation = reserve(owner, equipment);

        adminPatch(reservation.getId(), "rent", 204);

        assertThrows(ResourceConflictException.class,
                () -> reserve(other, equipment));

        adminPatch(reservation.getId(), "return", 204);

        assertThrows(ResourceConflictException.class,
                () -> reserve(other, equipment));

        assertStatus(reservation.getId(), ReservationStatus.RETURNED);
    }

    @Test
    void simultaneousRentRequestsAllowOnlyOne() throws Exception {
        Account owner = saveAccount();
        Reservation reservation = reserve(owner, saveEquipment());

        List<Integer> results = runConcurrentAdminRequests(reservation.getId(), "rent");

        assertEquals(List.of(204, 409), results);
        assertStatus(reservation.getId(), ReservationStatus.RENTED);
    }

    @Test
    void simultaneousReturnRequestsAllowOnlyOne() throws Exception {
        Account owner = saveAccount();
        Reservation reservation = reserve(owner, saveEquipment());
        Long id = reservation.getId();

        adminPatch(id, "rent", 204);

        List<Integer> results = runConcurrentAdminRequests(id, "return");

        assertEquals(List.of(204, 409), results);
        assertStatus(id, ReservationStatus.RETURNED);
    }

    @Test
    void onlyAdminCanReadAllReservations() throws Exception {
        Account first = saveAccount();
        Account second = saveAccount();

        Reservation firstReservation = reserve(first, saveEquipment());
        Reservation secondReservation = reserve(second, saveEquipment());

        mockMvc.perform(
                get("/reservations")
                        .with(user(first.getEmail()).roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                get("/reservations")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.id == " + firstReservation.getId() + ")].status")
                        .value(org.hamcrest.Matchers.hasItem("ACTIVE")))
                .andExpect(jsonPath(
                        "$[?(@.id == " + secondReservation.getId() + ")].status")
                        .value(org.hamcrest.Matchers.hasItem("ACTIVE")));
    }

    private List<Integer> runConcurrentAdminRequests(Long id, String action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Integer> request = () -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("동시 요청 준비 시간 초과");
                }

                return mockMvc.perform(
                        patch("/reservations/{id}/{action}", id, action)
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            };

            Future<Integer> first = executor.submit(request);
            Future<Integer> second = executor.submit(request);

            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("요청 두 개가 준비되지 않음");
            }

            start.countDown();

            return List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS))
                    .stream()
                    .sorted()
                    .toList();

        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(20, TimeUnit.SECONDS);
        }
    }

    private void adminPatch(Long id, String action, int expectedStatus) throws Exception {
        int actualStatus = mockMvc.perform(
                patch("/reservations/{id}/{action}", id, action)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andReturn()
                .getResponse()
                .getStatus();

        assertEquals(expectedStatus, actualStatus);
    }

    private void assertStatus(Long id, ReservationStatus expected) {
        Reservation reservation = reservationRepository.findById(id).orElseThrow();
        assertEquals(expected, reservation.getStatus());
    }

    private Account saveAccount() {
        Account account = accountRepository.save(
                new Account(UUID.randomUUID() + "@example.com", "test-password-hash"));
        accountIds.add(account.getId());
        return account;
    }

    private Equipment saveEquipment() {
        int id;
        do {
            id = ThreadLocalRandom.current().nextInt(100_000_000, Integer.MAX_VALUE);
        } while (equipmentRepository.existsById(id));

        Equipment equipment = equipmentRepository.save(new Equipment(id, "TEST-CAMERA"));
        equipmentIds.add(equipment.getId());
        return equipment;
    }

    private Reservation reserve(Account account, Equipment equipment) {
        return reservationService.createReservation(
                account.getEmail(), equipment.getId(), START, END);
    }
}
