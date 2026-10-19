package com.gduck0.labgear.controller;

import com.gduck0.labgear.dto.ReservationCreateRequest;
import com.gduck0.labgear.dto.ReservationResponse;
import com.gduck0.labgear.service.ReservationService;
import com.gduck0.labgear.entity.Reservation;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

@RestController
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public void createReservation(@Valid @RequestBody ReservationCreateRequest request) {
        reservationService.createReservation(request.getId(), request.getStudentId(), request.getEquipmentId(),
                request.getStartDate(), request.getEndDate());
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse getReservation(@PathVariable int id) {
        Reservation reservation = reservationService.getReservation(id);
        ReservationResponse response = new ReservationResponse(
            reservation.getId(),
            reservation.getStudent().getId(),
            reservation.getEquipment().getId(),
            reservation.getStartDate(),
            reservation.getEndDate()
        );
        return response;
    }
}
