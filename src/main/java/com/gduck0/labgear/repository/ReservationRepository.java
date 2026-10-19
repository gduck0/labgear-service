package com.gduck0.labgear.repository;

import com.gduck0.labgear.entity.Reservation;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

        @Query("""
                SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
                FROM Reservation r
                WHERE r.equipment.id = :equipmentId
                  AND r.startDate <= :endDate
                  AND r.endDate >= :startDate
                """)
        boolean existsOverlappingReservation(
                @Param("equipmentId") int equipmentId,
                @Param("startDate") LocalDate startDate,
                @Param("endDate") LocalDate endDate);

        boolean existsByEquipment_Id(int equipmentId);

}
