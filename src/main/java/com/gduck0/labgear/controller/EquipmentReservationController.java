package com.gduck0.labgear.controller;

import com.gduck0.labgear.dto.EquipmentReservationCreateRequest;
import com.gduck0.labgear.dto.EquipmentReservationResponse;
import com.gduck0.labgear.service.EquipmentReservationService;
import com.gduck0.labgear.entity.EquipmentReservation;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;

import java.util.Optional;

@RestController
public class EquipmentReservationController {

    private EquipmentReservationService reservationService;

    public EquipmentReservationController(EquipmentReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public void createReservation(@RequestBody EquipmentReservationCreateRequest request) {
        reservationService.addReservation(request.getId(), request.getStudentId(), request.getEquipmentItemId(),
                request.getStartDate(), request.getEndDate());
    }

    @GetMapping("/reservations/{id}")
    public Optional<EquipmentReservationResponse> getReservation(@PathVariable int id) {
        Optional<EquipmentReservation> result = reservationService.getReservation(id);
        Optional<EquipmentReservationResponse> response = result.map(reservation -> new EquipmentReservationResponse(
                reservation.getId(),
                reservation.getStudent().getId(),
                reservation.getEquipmentItem().getId(),
                reservation.getStartDate(),
                reservation.getEndDate()));

        return response;
    }
}
