
package com.gduck0.labgear.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gduck0.labgear.dto.ReservationCreateRequest;
import com.gduck0.labgear.dto.ReservationResponse;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.service.ReservationService;

@RestController
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse createReservation(
            @Valid @RequestBody ReservationCreateRequest request,
            Authentication authentication) {

        Reservation reservation = reservationService.createReservation(
                authentication.getName(),
                request.getEquipmentId(),
                request.getStartDate(),
                request.getEndDate());

        return toResponse(reservation);
    }

    @GetMapping("/reservations/my")
    public List<ReservationResponse> getMyReservations(
            Authentication authentication) {

        return reservationService.getMyReservations(
                authentication.getName())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse getReservation(
            @PathVariable Long id,
            Authentication authentication) {

        Reservation reservation = reservationService.getReservation(
                id, authentication.getName());

        return toResponse(reservation);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getEquipment().getId(),
                reservation.getStartDate(),
                reservation.getEndDate());
    }
}
