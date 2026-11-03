package com.gduck0.labgear.repository;

import com.gduck0.labgear.entity.Reservation;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            WHERE r.equipment.id = :equipmentId
                AND r.status <> com.gduck0.labgear.entity.ReservationStatus.CANCELED
                AND r.startDate <= :endDate
                AND r.endDate >= :startDate
            """)
    boolean existsOverlappingReservation(
            @Param("equipmentId") int equipmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.equipment.id = :equipmentId
                AND r.status <> com.gduck0.labgear.entity.ReservationStatus.CANCELED
                AND r.startDate <= :endDate
                AND r.endDate >= :startDate
            ORDER BY r.startDate ASC, r.id ASC
            """)
    List<Reservation> findReservedPeriods(
            @Param("equipmentId") int equipmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByEquipment_Id(int equipmentId);

    List<Reservation> findByAccount_IdOrderByIdDesc(Long accountId);

    List<Reservation> findAllByOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);
}
