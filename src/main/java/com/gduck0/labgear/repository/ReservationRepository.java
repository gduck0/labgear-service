
package com.gduck0.labgear.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import com.gduck0.labgear.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            WHERE r.equipment.id = :equipmentId
                AND r.status = com.gduck0.labgear.entity.ReservationStatus.ACTIVE
                AND r.startDate <= :endDate
                AND r.endDate >= :startDate
            """)
    boolean existsOverlappingReservation(
            @Param("equipmentId") int equipmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByEquipment_Id(int equipmentId);

    List<Reservation> findByAccount_IdOrderByIdDesc(Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);
}
