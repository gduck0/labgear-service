
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

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.repository.AccountRepository;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.ReservationRepository;
import com.gduck0.labgear.service.AccountService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void loggedInUserCanCreateAndReadOwnReservation() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String password = "password123";

        accountService.register(email, password);

        int equipmentId = saveEquipment();

        MvcResult login = mockMvc.perform(post("/login")
                .with(csrf())
                .param("email", email)
                .param("password", password))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session =
                (MockHttpSession) login.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(post("/reservations")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                        equipmentId, "2026-11-10", "2026-11-12")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.equipmentId").value(equipmentId));

        Long accountId = accountRepository.findByEmail(email)
                .orElseThrow().getId();

        Reservation saved = reservationRepository
                .findByAccount_IdOrderByIdDesc(accountId)
                .get(0);

        mockMvc.perform(get("/reservations/{id}", saved.getId())
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(saved.getId()))
            .andExpect(jsonPath("$.startDate").value("2026-11-10"))
            .andExpect(jsonPath("$.endDate").value("2026-11-12"));

        mockMvc.perform(get("/reservations/my")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(saved.getId()))
            .andExpect(jsonPath("$[0].equipmentId").value(equipmentId));
    }

    @Test
    void anotherUserCannotReadReservation() throws Exception {
        String ownerEmail = UUID.randomUUID() + "@example.com";
        String otherEmail = UUID.randomUUID() + "@example.com";

        Account owner = accountRepository.save(
                new Account(ownerEmail, "test-password-hash"));

        accountRepository.save(
                new Account(otherEmail, "test-password-hash"));

        int equipmentId = saveEquipment();

        mockMvc.perform(post("/reservations")
                .with(user(ownerEmail).roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                        equipmentId, "2026-11-10", "2026-11-12")))
            .andExpect(status().isCreated());

        Reservation saved = reservationRepository
                .findByAccount_IdOrderByIdDesc(owner.getId())
                .get(0);

        mockMvc.perform(get("/reservations/{id}", saved.getId())
                .with(user(otherEmail).roles("USER")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/reservations/my")
                .with(user(otherEmail).roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void overlappingReservationReturns409AndAdjacentDatesWork()
            throws Exception {
        String email = saveAccount();
        int equipmentId = saveEquipment();

        mockMvc.perform(post("/reservations")
                .with(user(email).roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                        equipmentId, "2026-11-10", "2026-11-12")))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/reservations")
                .with(user(email).roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                        equipmentId, "2026-11-12", "2026-11-13")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(post("/reservations")
                .with(user(email).roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                        equipmentId, "2026-11-13", "2026-11-14")))
            .andExpect(status().isCreated());

        Long accountId = accountRepository.findByEmail(email)
                .orElseThrow().getId();

        assertEquals(2, reservationRepository
                .findByAccount_IdOrderByIdDesc(accountId).size());
    }

    @Test
    void missingEquipmentReturns404AndDoesNotCreateReservation()
            throws Exception {
        String email = saveAccount();
        int equipmentId = ThreadLocalRandom.current()
                .nextInt(100_000_000, Integer.MAX_VALUE);

        assertFalse(equipmentRepository.existsById(equipmentId));

        mockMvc.perform(post("/reservations")
                .with(user(email).roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                        equipmentId, "2026-11-10", "2026-11-12")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));

        Long accountId = accountRepository.findByEmail(email)
                .orElseThrow().getId();

        assertEquals(0, reservationRepository
                .findByAccount_IdOrderByIdDesc(accountId).size());
    }

    @Test
    void reversedDatesReturn400() throws Exception {
        String email = saveAccount();
        int equipmentId = saveEquipment();

        mockMvc.perform(post("/reservations")
                .with(user(email).roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                        equipmentId, "2026-11-15", "2026-11-10")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    private String saveAccount() {
        String email = UUID.randomUUID() + "@example.com";
        accountRepository.save(
                new Account(email, "test-password-hash"));
        return email;
    }

    private int saveEquipment() {
        int equipmentId = ThreadLocalRandom.current()
                .nextInt(100_000_000, Integer.MAX_VALUE);

        equipmentRepository.save(
                new Equipment(equipmentId, "TEST-CAMERA"));

        return equipmentId;
    }

    private String requestJson(
            int equipmentId, String startDate, String endDate) {
        return """
            {
                "equipmentId": %d,
                "startDate": "%s",
                "endDate": "%s"
            }
            """.formatted(equipmentId, startDate, endDate);
    }
}
