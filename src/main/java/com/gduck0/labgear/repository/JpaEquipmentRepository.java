package com.gduck0.labgear.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.gduck0.labgear.entity.EquipmentItem;

@Repository
class JpaEquipmentRepository implements EquipmentRepository {

    private SpringDataEquipmentRepository springDataEquipmentRepository;

    public JpaEquipmentRepository(SpringDataEquipmentRepository springDataEquipmentRepository) {
        this.springDataEquipmentRepository = springDataEquipmentRepository;
    }

    @Override
    public void save(EquipmentItem item) {
        springDataEquipmentRepository.save(item);
    }

    @Override
    public List<EquipmentItem> getAll() {
        return springDataEquipmentRepository.findAll();
    }

    @Override
    public Optional<EquipmentItem> findById(int id) {
        return springDataEquipmentRepository.findById(id);
    }

    @Override
    public void deleteById(int id) {
        springDataEquipmentRepository.deleteById(id);
    }
}