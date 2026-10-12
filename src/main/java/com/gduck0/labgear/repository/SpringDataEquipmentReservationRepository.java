package com.gduck0.labgear.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gduck0.labgear.entity.EquipmentReservation;

public interface SpringDataEquipmentReservationRepository
        extends JpaRepository<EquipmentReservation, Integer> {

}
