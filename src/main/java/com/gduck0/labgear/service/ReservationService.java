package com.gduck0.labgear.service;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.entity.ReservationStatus;
import com.gduck0.labgear.exception.ResourceConflictException;
import com.gduck0.labgear.exception.ResourceNotFoundException;
import com.gduck0.labgear.repository.AccountRepository;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.ReservationRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {

    private final AccountRepository accountRepository;
    private final EquipmentRepository equipmentRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(
            AccountRepository accountRepository,
            EquipmentRepository equipmentRepository,
            ReservationRepository reservationRepository) {
        this.accountRepository = accountRepository;
        this.equipmentRepository = equipmentRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reservation createReservation(
            String email, int equipmentId, LocalDate startDate, LocalDate endDate) {

        Equipment equipment =
                equipmentRepository
                        .findByIdForUpdate(equipmentId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "기자재를 찾을 수 없습니다. ID: " + equipmentId));

        Account account =
                accountRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("계정을 찾을 수 없습니다."));

        if (reservationRepository.existsOverlappingReservation(equipmentId, startDate, endDate)) {
            throw new ResourceConflictException("이미 예약된 기간입니다.");
        }

        Reservation reservation = new Reservation(account, equipment, startDate, endDate);

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id, String email) {
        Reservation reservation =
                reservationRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("예약을 찾을 수 없습니다. ID: " + id));

        if (!reservation.getAccount().getEmail().equals(email)) {
            throw new AccessDeniedException("다른 사용자의 예약은 조회할 수 없습니다.");
        }

        return reservation;
    }

    public List<Reservation> getMyReservations(String email) {
        Account account =
                accountRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("계정을 찾을 수 없습니다."));

        return reservationRepository.findByAccount_IdOrderByIdDesc(account.getId());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelReservation(Long id, String email) {
        Reservation reservation =
                reservationRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("예약을 찾을 수 없습니다. ID: " + id));
        if (!reservation.getAccount().getEmail().equals(email)) {
            throw new AccessDeniedException("다른 사용자의 예약은 취소할 수 없습니다.");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELED) {
            throw new ResourceConflictException("이미 취소된 예약입니다.");
        }
        reservation.cancel();
    }
}
