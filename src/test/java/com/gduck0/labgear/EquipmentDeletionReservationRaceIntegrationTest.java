package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 실제 MySQL 트랜잭션을 사용해 동일 기자재의 삭제/예약 요청 간 경쟁을 검증한다.
 * 각 MockMvc 호출은 별도 스레드에서 수행하고 테스트 메서드에는 @Transactional을 붙이지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EquipmentDeletionReservationRaceIntegrationTest {

    private static final int ATTEMPTS = 20;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void simultaneousDeleteAndReserveMustHaveOneConsistentWinner() throws Exception {
        Account account = accountRepository.save(new Account(
                "delete-race-" + UUID.randomUUID() + "@example.com",
                "test-password-hash"));
        List<Integer> equipmentIds = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        LocalDate startDate = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(30);
        LocalDate endDate = startDate.plusDays(2);

        try {
            for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                int equipmentId;
                do {
                    equipmentId = ThreadLocalRandom.current().nextInt(100_000_000, Integer.MAX_VALUE);
                } while (equipmentRepository.existsById(equipmentId));
                equipmentIds.add(equipmentId);
                equipmentRepository.save(new Equipment(equipmentId, "RACE-EQUIPMENT"));

                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                int currentId = equipmentId;

                Callable<Integer> deletion = () -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("삭제 요청 대기 시간 초과");
                    return mockMvc.perform(delete("/equipment/{id}", currentId)
                                    .with(user("admin@example.com").roles("ADMIN"))
                                    .with(csrf()))
                            .andReturn().getResponse().getStatus();
                };
                Callable<Integer> booking = () -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("예약 요청 대기 시간 초과");
                    String json = """
                            {"equipmentId": %d, "startDate": "%s", "endDate": "%s"}
                            """.formatted(currentId, startDate, endDate);
                    return mockMvc.perform(post("/reservations")
                                    .with(user(account.getEmail()).roles("USER"))
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                            .andReturn().getResponse().getStatus();
                };

                Future<Integer> deleteResult = executor.submit(deletion);
                Future<Integer> bookingResult = executor.submit(booking);
                try {
                    assertTrue(ready.await(10, TimeUnit.SECONDS));
                    start.countDown();

                    int deleteStatus = deleteResult.get(20, TimeUnit.SECONDS);
                    int bookingStatus = bookingResult.get(20, TimeUnit.SECONDS);

                    List<Reservation> reservations = reservationRepository
                            .findByAccount_IdOrderByIdDesc(account.getId()).stream()
                            .filter(r -> r.getEquipment().getId() == currentId)
                            .toList();
                    boolean equipmentExists = equipmentRepository.existsById(currentId);

                    boolean deletedFirst = deleteStatus == 200
                            && bookingStatus == 404
                            && !equipmentExists
                            && reservations.isEmpty();
                    boolean reservedFirst = deleteStatus == 409
                            && bookingStatus == 201
                            && equipmentExists
                            && reservations.size() == 1;

                    assertTrue(deletedFirst || reservedFirst,
                            "시도 " + attempt + "에서 예상하지 못한 결과: 삭제=" + deleteStatus
                                    + ", 예약=" + bookingStatus
                                    + ", 기자재존재=" + equipmentExists
                                    + ", 예약수=" + reservations.size());
                } finally {
                    start.countDown();
                }
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(20, TimeUnit.SECONDS);
            reservationRepository.deleteAll(reservationRepository.findByAccount_IdOrderByIdDesc(account.getId()));
            for (int equipmentId : equipmentIds) {
                if (equipmentRepository.existsById(equipmentId)) equipmentRepository.deleteById(equipmentId);
            }
            accountRepository.deleteById(account.getId());
        }
    }
}
