package com.gduck0.labgear.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gduck0.labgear.entity.EquipmentItem;

public interface SpringDataEquipmentRepository
        extends JpaRepository<EquipmentItem, Integer> {

}