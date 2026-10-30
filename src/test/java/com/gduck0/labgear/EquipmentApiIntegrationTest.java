
package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.repository.AccountRepository;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.service.EquipmentService;
import com.gduck0.labgear.service.ReservationService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EquipmentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void creatingEquipmentReturns201() throws Exception {
        int id = nextId();

        mockMvc.perform(post("/equipment")
                .with(user("admin@example.com").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson(id, "TEST-NEW-CAMERA")))
            .andExpect(status().isCreated());

        assertEquals("TEST-NEW-CAMERA",
                equipmentRepository.findById(id).orElseThrow().getName());
    }

    @Test
    void duplicateEquipmentReturns409AndPreservesData() throws Exception {
        int id = nextId();

        equipmentService.createEquipment(new Equipment(id, "TEST-CAMERA"));

        mockMvc.perform(post("/equipment")
                .with(user("admin@example.com").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson(id, "TEST-LAPTOP")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));

        assertEquals("TEST-CAMERA",
                equipmentRepository.findById(id).orElseThrow().getName());
    }

    @Test
    void invalidEquipmentRequestReturns400() throws Exception {
        mockMvc.perform(post("/equipment")
                .with(user("admin@example.com").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson(0, " ")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingEquipmentReturns404() throws Exception {
        int id = nextId();

        assertFalse(equipmentRepository.existsById(id));

        mockMvc.perform(get("/equipment/{id}", id)
                .with(user("user@example.com").roles("USER")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deletingReservedEquipmentReturns409AndPreservesData()
            throws Exception {
        int equipmentId = nextId();
        String email = UUID.randomUUID() + "@example.com";

        accountRepository.save(
                new Account(email, "test-password-hash"));

        equipmentRepository.save(
                new Equipment(equipmentId, "TEST-CAMERA"));

        Reservation reservation = reservationService.createReservation(
                email,
                equipmentId,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12));

        mockMvc.perform(delete("/equipment/{id}", equipmentId)
                .with(user("admin@example.com").roles("ADMIN"))
                .with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));

        assertEquals("TEST-CAMERA",
                equipmentRepository.findById(equipmentId)
                        .orElseThrow().getName());

        assertEquals(equipmentId,
                reservationService.getReservation(
                        reservation.getId(), email)
                        .getEquipment().getId());
    }

    @Test
    void deletingUnreservedEquipmentSucceeds() throws Exception {
        int equipmentId = nextId();

        equipmentRepository.save(
                new Equipment(equipmentId, "TEST-AVAILABLE-CAMERA"));

        mockMvc.perform(delete("/equipment/{id}", equipmentId)
                .with(user("admin@example.com").roles("ADMIN"))
                .with(csrf()))
            .andExpect(status().isOk());

        assertFalse(equipmentRepository.existsById(equipmentId));
    }

    @Test
    void normalUserCannotManageEquipment() throws Exception {
        int equipmentId = nextId();
        int newEquipmentId = nextId();

        equipmentRepository.save(
                new Equipment(equipmentId, "ORIGINAL-CAMERA"));

        mockMvc.perform(post("/equipment")
                .with(user("user@example.com").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson(newEquipmentId, "NEW-CAMERA")))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/equipment/{id}/name", equipmentId)
                .with(user("user@example.com").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"CHANGED-CAMERA\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/equipment/{id}", equipmentId)
                .with(user("user@example.com").roles("USER"))
                .with(csrf()))
            .andExpect(status().isForbidden());

        assertFalse(equipmentRepository.existsById(newEquipmentId));
        assertEquals("ORIGINAL-CAMERA",
                equipmentRepository.findById(equipmentId)
                        .orElseThrow().getName());
    }

    @Test
    void adminCanUpdateEquipmentName() throws Exception {
        int equipmentId = nextId();

        equipmentRepository.save(
                new Equipment(equipmentId, "OLD-CAMERA"));

        mockMvc.perform(patch("/equipment/{id}/name", equipmentId)
                .with(user("admin@example.com").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"NEW-CAMERA\"}"))
            .andExpect(status().isOk());

        assertEquals("NEW-CAMERA",
                equipmentRepository.findById(equipmentId)
                        .orElseThrow().getName());
    }

    private int nextId() {
        return ThreadLocalRandom.current()
                .nextInt(100_000_000, Integer.MAX_VALUE);
    }

    private String createJson(int id, String name) {
        return """
            {
                "id": %d,
                "name": "%s"
            }
            """.formatted(id, name);
    }
}
