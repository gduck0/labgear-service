package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.repository.AccountRepository;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.ReservationRepository;
import com.gduck0.labgear.service.AccountService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationApiIntegrationTest {

    private static final LocalDate TEST_BASE = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(30);

    @Autowired private MockMvc mockMvc;

    @Autowired private AccountService accountService;

    @Autowired private AccountRepository accountRepository;

    @Autowired private EquipmentRepository equipmentRepository;

    @Autowired private ReservationRepository reservationRepository;

    @Test
    void loggedInUserCanCreateAndReadOwnReservation() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String password = "password123";

        accountService.register(email, password);

        int equipmentId = saveEquipment();

        MvcResult login =
                mockMvc.perform(
                                post("/login")
                                        .with(csrf())
                                        .param("email", email)
                                        .param("password", password))
                        .andExpect(status().isOk())
                        .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(
                        post("/reservations")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson(equipmentId, dateAfter(0), dateAfter(2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.equipmentId").value(equipmentId));

        Long accountId = accountRepository.findByEmail(email).orElseThrow().getId();

        Reservation saved = reservationRepository.findByAccount_IdOrderByIdDesc(accountId).get(0);

        mockMvc.perform(get("/reservations/{id}", saved.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.startDate").value(dateAfter(0)))
                .andExpect(jsonPath("$.endDate").value(dateAfter(2)));

        mockMvc.perform(get("/reservations/my").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(saved.getId()))
                .andExpect(jsonPath("$[0].equipmentId").value(equipmentId));
    }

    @Test
    void anotherUserCannotReadReservation() throws Exception {
        String ownerEmail = UUID.randomUUID() + "@example.com";
        String otherEmail = UUID.randomUUID() + "@example.com";

        Account owner = accountRepository.save(new Account(ownerEmail, "test-password-hash"));

        accountRepository.save(new Account(otherEmail, "test-password-hash"));

        int equipmentId = saveEquipment();

        mockMvc.perform(
                        post("/reservations")
                                .with(user(ownerEmail).roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson(equipmentId, dateAfter(0), dateAfter(2))))
                .andExpect(status().isCreated());

        Reservation saved =
                reservationRepository.findByAccount_IdOrderByIdDesc(owner.getId()).get(0);

        mockMvc.perform(
                        get("/reservations/{id}", saved.getId())
                                .with(user(otherEmail).roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/reservations/my").with(user(otherEmail).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void overlappingReservationReturns409AndAdjacentDatesWork() throws Exception {
        String email = saveAccount();
        int equipmentId = saveEquipment();

        mockMvc.perform(
                        post("/reservations")
                                .with(user(email).roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson(equipmentId, dateAfter(0), dateAfter(2))))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/reservations")
                                .with(user(email).roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson(equipmentId, dateAfter(2), dateAfter(3))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(
                        post("/reservations")
                                .with(user(email).roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson(equipmentId, dateAfter(3), dateAfter(4))))
                .andExpect(status().isCreated());

        Long accountId = accountRepository.findByEmail(email).orElseThrow().getId();

        assertEquals(2, reservationRepository.findByAccount_IdOrderByIdDesc(accountId).size());
    }

    @Test
    void missingEquipmentReturns404AndDoesNotCreateReservation() throws Exception {
        String email = saveAccount();
        int equipmentId = ThreadLocalRandom.current().nextInt(100_000_000, Integer.MAX_VALUE);

        assertFalse(equipmentRepository.existsById(equipmentId));

        mockMvc.perform(
                        post("/reservations")
                                .with(user(email).roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson(equipmentId, dateAfter(0), dateAfter(2))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        Long accountId = accountRepository.findByEmail(email).orElseThrow().getId();

        assertEquals(0, reservationRepository.findByAccount_IdOrderByIdDesc(accountId).size());
    }

    @Test
    void reversedDatesReturn400() throws Exception {
        String email = saveAccount();
        int equipmentId = saveEquipment();

        mockMvc.perform(
                        post("/reservations")
                                .with(user(email).roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson(equipmentId, dateAfter(5), dateAfter(0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void pastStartDateReturns400AndTodayIsAllowed() throws Exception {
        String email = saveAccount();
        int equipmentId = saveEquipment();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        mockMvc.perform(
                        post("/reservations")
                                .with(user(email).roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson(
                                        equipmentId, today.minusDays(1).toString(), today.plusDays(1).toString())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        post("/reservations")
                                .with(user(email).roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson(
                                        equipmentId, today.toString(), today.plusDays(1).toString())))
                .andExpect(status().isCreated());
    }

    private String dateAfter(int days) {
        return TEST_BASE.plusDays(days).toString();
    }

    private String saveAccount() {
        String email = UUID.randomUUID() + "@example.com";
        accountRepository.save(new Account(email, "test-password-hash"));
        return email;
    }

    private int saveEquipment() {
        int equipmentId = ThreadLocalRandom.current().nextInt(100_000_000, Integer.MAX_VALUE);

        equipmentRepository.save(new Equipment(equipmentId, "TEST-CAMERA"));

        return equipmentId;
    }

    private String requestJson(int equipmentId, String startDate, String endDate) {
        return """
            {
                "equipmentId": %d,
                "startDate": "%s",
                "endDate": "%s"
            }
            """
                .formatted(equipmentId, startDate, endDate);
    }
}
