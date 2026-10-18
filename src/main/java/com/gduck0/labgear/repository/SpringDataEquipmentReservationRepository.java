package com.gduck0.labgear.repository;

import com.gduck0.labgear.entity.EquipmentReservation;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataEquipmentReservationRepository extends JpaRepository<EquipmentReservation, Integer> {

        @Query("""
                SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
                FROM EquipmentReservation r
                WHERE r.equipmentItem.id = :equipmentItemId
                  AND r.startDate <= :endDate
                  AND r.endDate >= :startDate
                """)
        boolean existsOverlappingReservation(
                @Param("equipmentItemId") int equipmentItemId,
                @Param("startDate") LocalDate startDate,
                @Param("endDate") LocalDate endDate);

        boolean existsByEquipmentItem_Id(int equipmentItemId);

}
