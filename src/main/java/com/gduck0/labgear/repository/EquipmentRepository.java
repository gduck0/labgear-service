package com.gduck0.labgear.repository;

import java.util.List;
import java.util.Optional;

import com.gduck0.labgear.entity.EquipmentItem;

public interface EquipmentRepository{

    void save(EquipmentItem item);

    List<EquipmentItem> getAll();

    Optional<EquipmentItem> findById(int id);

    void deleteById(int id);

}