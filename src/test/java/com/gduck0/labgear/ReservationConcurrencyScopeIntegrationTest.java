
package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.repository.AccountRepository;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.ReservationRepository;
import com.gduck0.labgear.service.ReservationService;

@SpringBootTest
class ReservationConcurrencyScopeIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<Long> accountIds = new ArrayList<>();
    private final List<Integer> equipmentIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
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
    void sameEquipmentWithDifferentPeriodsAllowsBothReservations()
            throws Exception {

        int equipmentId = createEquipment();
        String userA = createAccount();
        String userB = createAccount();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Reservation> first = submitReservation(
                    executor, ready, start, userA, equipmentId,
                    LocalDate.of(2026, 11, 10),
                    LocalDate.of(2026, 11, 12));

            Future<Reservation> second = submitReservation(
                    executor, ready, start, userB, equipmentId,
                    LocalDate.of(2026, 11, 13),
                    LocalDate.of(2026, 11, 15));

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            assertNotNull(first.get(15, TimeUnit.SECONDS).getId());
            assertNotNull(second.get(15, TimeUnit.SECONDS).getId());

            assertEquals(2, countReservations(equipmentId));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));
        }
    }

    @Test
    void otherEquipmentAndNormalReadProceedWhileOneEquipmentIsLocked()
            throws Exception {

        int lockedEquipmentId = createEquipment();
        int otherEquipmentId = createEquipment();
        String email = createAccount();

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            Future<?> lockHolder = executor.submit(() -> {
                TransactionTemplate transaction =
                        new TransactionTemplate(transactionManager);

                transaction.executeWithoutResult(status -> {
                    equipmentRepository
                            .findByIdForUpdate(lockedEquipmentId)
                            .orElseThrow();

                    locked.countDown();

                    try {
                        if (!release.await(20, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Lock hold timed out");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                });
            });

            assertTrue(locked.await(10, TimeUnit.SECONDS));

            Future<Reservation> otherReservation =
                    executor.submit(() ->
                            reservationService.createReservation(
                                    email,
                                    otherEquipmentId,
                                    LocalDate.of(2026, 11, 10),
                                    LocalDate.of(2026, 11, 12)));

            Future<Equipment> normalRead =
                    executor.submit(() ->
                            equipmentRepository
                                    .findById(lockedEquipmentId)
                                    .orElseThrow());

            Reservation saved = otherReservation.get(
                    10, TimeUnit.SECONDS);

            Equipment equipment = normalRead.get(
                    10, TimeUnit.SECONDS);

            assertNotNull(saved.getId());
            assertEquals(lockedEquipmentId, equipment.getId());
            assertEquals(1, countReservations(otherEquipmentId));

            release.countDown();
            lockHolder.get(10, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(25, TimeUnit.SECONDS));
        }
    }

    private Future<Reservation> submitReservation(
            ExecutorService executor,
            CountDownLatch ready,
            CountDownLatch start,
            String email,
            int equipmentId,
            LocalDate startDate,
            LocalDate endDate) {

        return executor.submit(() -> {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Start timed out");
            }

            return reservationService.createReservation(
                    email, equipmentId, startDate, endDate);
        });
    }

    private String createAccount() {
        String email = UUID.randomUUID() + "@example.com";

        Account account = accountRepository.save(
                new Account(email, "test-password-hash"));

        accountIds.add(account.getId());
        return email;
    }

    private int createEquipment() {
        int id;

        do {
            id = ThreadLocalRandom.current()
                    .nextInt(100_000_000, 2_000_000_000);
        } while (equipmentIds.contains(id)
                || equipmentRepository.existsById(id));

        equipmentRepository.save(
                new Equipment(id, "TEST-EQUIPMENT"));

        equipmentIds.add(id);
        return id;
    }

    private long countReservations(int equipmentId) {
        return reservationRepository.findAll().stream()
                .filter(r -> r.getEquipment().getId() == equipmentId)
                .count();
    }
}
