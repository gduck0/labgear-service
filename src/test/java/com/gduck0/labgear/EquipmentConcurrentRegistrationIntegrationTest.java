package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.repository.EquipmentRepository;

/**
 * 기존 EquipmentService의 구현을 변경하지 않고 동시에 같은 ID로 등록하는
 * 두 관리자 요청의 결과와 기존 데이터 보존 여부를 관찰하는 회귀 테스트.
 *
 * 테스트 클래스에 @Transactional을 붙이지 않아 요청별 DB 트랜잭션이 독립적이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EquipmentConcurrentRegistrationIntegrationTest {

    private static final int ATTEMPTS = 60;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Test
    void simultaneousRegistrationMustReturnOne201AndOne409WithoutOverwriting()
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<String> unexpectedSamples = new ArrayList<>();

        int validRounds = 0;
        int bothSucceeded = 0;
        int exceptions = 0;
        int dataChangedOrMissing = 0;

        try {
            for (int round = 1; round <= ATTEMPTS; round++) {
                int id;
                do {
                    id = ThreadLocalRandom.current().nextInt(100_000_000, Integer.MAX_VALUE);
                } while (equipmentRepository.existsById(id));

                final int equipmentId = id;
                String firstName = "CAMERA-" + round;
                String secondName = "LAPTOP-" + round;
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);

                Future<Attempt> first = executor.submit(() ->
                        register(equipmentId, firstName, "admin-a@example.com", ready, start));
                Future<Attempt> second = executor.submit(() ->
                        register(equipmentId, secondName, "admin-b@example.com", ready, start));

                try {
                    if (!ready.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("두 등록 요청 준비 시간 초과");
                    }
                    start.countDown();

                    Attempt a = first.get(20, TimeUnit.SECONDS);
                    Attempt b = second.get(20, TimeUnit.SECONDS);

                    Equipment saved = equipmentRepository.findById(equipmentId).orElse(null);
                    String storedName = saved == null ? "<MISSING>" : saved.getName();

                    boolean statusValid = (a.status() == 201 && b.status() == 409)
                            || (a.status() == 409 && b.status() == 201);
                    String winnerName = a.status() == 201 ? firstName
                            : b.status() == 201 ? secondName : "<NO WINNER>";
                    boolean dataValid = saved != null
                            && statusValid && winnerName.equals(storedName);

                    if (statusValid && dataValid) {
                        validRounds++;
                    } else if (unexpectedSamples.size() < 12) {
                        unexpectedSamples.add(
                                "round=" + round + ", a=" + a + ", b=" + b
                                + ", stored=" + storedName + ", expectedWinner=" + winnerName);
                    }

                    if (a.status() == 201 && b.status() == 201) {
                        bothSucceeded++;
                    }
                    if (a.status() < 0 || b.status() < 0) {
                        exceptions++;
                    }
                    if (!dataValid) {
                        dataChangedOrMissing++;
                    }
                } finally {
                    start.countDown();
                    equipmentRepository.deleteById(equipmentId);
                }
            }
        } finally {
            executor.shutdownNow();
            if (!executor.awaitTermination(25, TimeUnit.SECONDS)) {
                throw new IllegalStateException("테스트 스레드가 종료되지 않음");
            }
        }

        System.out.printf(
                "EQUIPMENT_CONCURRENT_RESULT attempts=%d valid=%d both201=%d thrown=%d invalidOrChanged=%d%n",
                ATTEMPTS, validRounds, bothSucceeded, exceptions, dataChangedOrMissing);
        unexpectedSamples.forEach(sample -> System.out.println("EQUIPMENT_CONCURRENT_SAMPLE " + sample));

        assertEquals(ATTEMPTS, validRounds,
                "같은 ID 동시 등록은 정확히 1건 201, 다른 1건 409여야 하며 "
                + "기존 데이터가 덮어써지면 안 된다. 오류 예시: " + unexpectedSamples);
    }

    private Attempt register(
            int equipmentId, String name, String admin,
            CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                return new Attempt(-1, "start-timeout");
            }

            String json = """
                    {"id": %d, "name": "%s"}
                    """.formatted(equipmentId, name);

            int status = mockMvc.perform(post("/equipment")
                    .with(user(admin).roles("ADMIN"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn().getResponse().getStatus();
            return new Attempt(status, "");
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message != null && message.length() > 180) {
                message = message.substring(0, 180);
            }
            return new Attempt(-1, ex.getClass().getSimpleName() + ": " + message);
        }
    }

    private record Attempt(int status, String error) {}
}
