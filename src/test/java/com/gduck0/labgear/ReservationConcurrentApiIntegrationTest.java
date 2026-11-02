package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.repository.AccountRepository;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.ReservationRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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

@SpringBootTest
@AutoConfigureMockMvc
class ReservationConcurrentApiIntegrationTest {

    private static final int ATTEMPTS = 30;

    @Autowired private MockMvc mockMvc;

    @Autowired private AccountRepository accountRepository;

    @Autowired private EquipmentRepository equipmentRepository;

    @Autowired private ReservationRepository reservationRepository;

    @Test
    void concurrentRequestsMustNotCreateOverlappingReservations() throws Exception {

        String emailA = UUID.randomUUID() + "@example.com";
        String emailB = UUID.randomUUID() + "@example.com";

        Account accountA = accountRepository.save(new Account(emailA, "test-password-hash"));

        Account accountB = accountRepository.save(new Account(emailB, "test-password-hash"));

        LocalDate startDate = LocalDate.of(2026, 11, 10);
        LocalDate endDate = LocalDate.of(2026, 11, 12);

        List<Integer> equipmentIds = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        int violations = 0;
        int duplicateRounds = 0;

        try {
            for (int round = 1; round <= ATTEMPTS; round++) {

                int equipmentId =
                        ThreadLocalRandom.current().nextInt(100_000_000, Integer.MAX_VALUE);

                equipmentRepository.save(new Equipment(equipmentId, "CONCURRENT-TEST"));

                equipmentIds.add(equipmentId);

                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);

                Future<Integer> futureA =
                        executor.submit(
                                () ->
                                        sendReservationRequest(
                                                emailA,
                                                equipmentId,
                                                startDate,
                                                endDate,
                                                ready,
                                                start));

                Future<Integer> futureB =
                        executor.submit(
                                () ->
                                        sendReservationRequest(
                                                emailB,
                                                equipmentId,
                                                startDate,
                                                endDate,
                                                ready,
                                                start));

                if (!ready.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("두 요청이 준비되지 않았습니다.");
                }

                start.countDown();

                int statusA = futureA.get(20, TimeUnit.SECONDS);
                int statusB = futureB.get(20, TimeUnit.SECONDS);

                List<Reservation> saved =
                        reservationRepository.findAll().stream()
                                .filter(
                                        reservation ->
                                                reservation.getEquipment().getId() == equipmentId)
                                .toList();

                int savedCount = saved.size();

                boolean validStatuses =
                        (statusA == 201 && statusB == 409) || (statusA == 409 && statusB == 201);

                if (!validStatuses || savedCount != 1) {
                    violations++;

                    System.out.printf(
                            "이상 발견 - 회차 %d: A=%d, B=%d, 저장=%d건%n",
                            round, statusA, statusB, savedCount);
                }

                if (savedCount > 1) {
                    duplicateRounds++;
                }
            }

            System.out.printf(
                    "검증 결과: 총 %d회, 정합성 위반 %d회, 중복 저장 %d회%n", ATTEMPTS, violations, duplicateRounds);

        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            for (Integer equipmentId : equipmentIds) {
                List<Reservation> reservations =
                        reservationRepository.findAll().stream()
                                .filter(
                                        reservation ->
                                                reservation.getEquipment().getId() == equipmentId)
                                .toList();

                for (Reservation reservation : reservations) {
                    reservationRepository.deleteById(reservation.getId());
                }

                equipmentRepository.deleteById(equipmentId);
            }

            accountRepository.deleteById(accountA.getId());
            accountRepository.deleteById(accountB.getId());
        }

        assertEquals(0, violations, "동시 예약 요청에서 정합성 위반이 발생했습니다.");
    }

    private int sendReservationRequest(
            String email,
            int equipmentId,
            LocalDate startDate,
            LocalDate endDate,
            CountDownLatch ready,
            CountDownLatch start)
            throws Exception {

        String requestBody =
                """
            {
                "equipmentId": %d,
                "startDate": "%s",
                "endDate": "%s"
            }
            """
                        .formatted(equipmentId, startDate, endDate);

        ready.countDown();

        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 요청 시작 신호를 받지 못했습니다.");
        }

        return mockMvc.perform(
                        post("/reservations")
                                .with(user(email).roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andReturn()
                .getResponse()
                .getStatus();
    }
}
