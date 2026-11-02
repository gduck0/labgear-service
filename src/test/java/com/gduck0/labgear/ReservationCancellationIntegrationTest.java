package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 예약 취소 기능의 API·DB 회귀 테스트.
 * 테스트 클래스에 @Transactional을 붙이지 않아 요청별 트랜잭션이 독립적이다.
 * 이미 사용 중인 테스트 DB만 사용하며, 테스트로 생성한 데이터는 종료 후 정리한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservationCancellationIntegrationTest {

    private static final LocalDate START = LocalDate.of(2027, 2, 10);
    private static final LocalDate END = LocalDate.of(2027, 2, 12);

    @Autowired private MockMvc mockMvc;

    @Autowired private ReservationService reservationService;

    @Autowired private AccountRepository accountRepository;

    @Autowired private EquipmentRepository equipmentRepository;

    @Autowired private ReservationRepository reservationRepository;

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
    void ownerCanCancelAndReadHistoryThenRebookSameDates() throws Exception {
        Account owner = saveAccount();
        Equipment equipment = saveEquipment();
        Reservation original = reserve(owner, equipment);

        mockMvc.perform(
                        patch("/reservations/{id}/cancel", original.getId())
                                .with(user(owner.getEmail()).roles("USER"))
                                .with(csrf()))
                .andExpect(status().isNoContent());

        assertEquals(
                ReservationStatus.CANCELED,
                reservationRepository.findById(original.getId()).orElseThrow().getStatus());

        mockMvc.perform(
                        get("/reservations/{id}", original.getId())
                                .with(user(owner.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        Reservation replacement = reserve(owner, equipment);
        assertEquals(ReservationStatus.ACTIVE, replacement.getStatus());

        mockMvc.perform(get("/reservations/my").with(user(owner.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(replacement.getId()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].id").value(original.getId()))
                .andExpect(jsonPath("$[1].status").value("CANCELED"));
    }

    @Test
    void secondCancellationReturns409AndPreservesCanceledStatus() throws Exception {
        Account owner = saveAccount();
        Reservation reservation = reserve(owner, saveEquipment());

        assertEquals(204, cancelStatus(reservation.getId(), owner.getEmail()));
        mockMvc.perform(
                        patch("/reservations/{id}/cancel", reservation.getId())
                                .with(user(owner.getEmail()).roles("USER"))
                                .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertEquals(
                ReservationStatus.CANCELED,
                reservationRepository.findById(reservation.getId()).orElseThrow().getStatus());
    }

    @Test
    void differentAccountCannotCancelAndMissingReservationReturns404() throws Exception {
        Account owner = saveAccount();
        Account other = saveAccount();
        Reservation reservation = reserve(owner, saveEquipment());

        mockMvc.perform(
                        patch("/reservations/{id}/cancel", reservation.getId())
                                .with(user(other.getEmail()).roles("USER"))
                                .with(csrf()))
                .andExpect(status().isForbidden());

        assertEquals(
                ReservationStatus.ACTIVE,
                reservationRepository.findById(reservation.getId()).orElseThrow().getStatus());

        mockMvc.perform(
                        patch("/reservations/{id}/cancel", Long.MAX_VALUE)
                                .with(user(owner.getEmail()).roles("USER"))
                                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void twoSimultaneousCancelRequestsReturnExactly204And409() throws Exception {
        Account owner = saveAccount();
        Equipment equipment = saveEquipment();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            for (int round = 0; round < 15; round++) {
                Reservation reservation = reserve(owner, equipment);
                Long reservationId = reservation.getId();
                String ownerEmail = owner.getEmail();
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);

                Future<Integer> first =
                        executor.submit(
                                () -> cancelAtSameTime(reservationId, ownerEmail, ready, start));
                Future<Integer> second =
                        executor.submit(
                                () -> cancelAtSameTime(reservationId, ownerEmail, ready, start));

                try {
                    if (!ready.await(10, TimeUnit.SECONDS)) {
                        throw new AssertionError("취소 요청 두 개가 준비되지 않음");
                    }
                    start.countDown();

                    int a = first.get(20, TimeUnit.SECONDS);
                    int b = second.get(20, TimeUnit.SECONDS);
                    boolean expected = (a == 204 && b == 409) || (a == 409 && b == 204);
                    assertEquals(
                            true, expected, "round=" + round + ", first=" + a + ", second=" + b);

                    assertEquals(
                            ReservationStatus.CANCELED,
                            reservationRepository
                                    .findById(reservationId)
                                    .orElseThrow()
                                    .getStatus());
                } finally {
                    start.countDown();
                }
            }
        } finally {
            executor.shutdownNow();
            if (!executor.awaitTermination(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("취소 요청 실행 스레드 종료 실패");
            }
        }
    }

    private int cancelAtSameTime(Long id, String email, CountDownLatch ready, CountDownLatch start)
            throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 시작 대기 시간 초과");
        }
        return cancelStatus(id, email);
    }

    private int cancelStatus(Long id, String email) throws Exception {
        return mockMvc.perform(
                        patch("/reservations/{id}/cancel", id)
                                .with(user(email).roles("USER"))
                                .with(csrf()))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private Account saveAccount() {
        Account account =
                accountRepository.save(
                        new Account(UUID.randomUUID() + "@example.com", "test-password-hash"));
        accountIds.add(account.getId());
        return account;
    }

    private Equipment saveEquipment() {
        int equipmentId;
        do {
            equipmentId = ThreadLocalRandom.current().nextInt(100_000_000, Integer.MAX_VALUE);
        } while (equipmentRepository.existsById(equipmentId));

        Equipment equipment = equipmentRepository.save(new Equipment(equipmentId, "TEST-CAMERA"));
        equipmentIds.add(equipment.getId());
        return equipment;
    }

    private Reservation reserve(Account account, Equipment equipment) {
        return reservationService.createReservation(
                account.getEmail(), equipment.getId(), START, END);
    }
}
