package com.gduck0.labgear.service;

import java.util.Optional;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.gduck0.labgear.entity.EquipmentItem;
import com.gduck0.labgear.entity.Student;
import com.gduck0.labgear.entity.EquipmentReservation;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.SpringDataStudentRepository;
import com.gduck0.labgear.repository.SpringDataEquipmentReservationRepository;

@Service
public class EquipmentReservationService {
    private SpringDataStudentRepository studentRepository;
    private EquipmentRepository equipmentRepository;
    private SpringDataEquipmentReservationRepository reservationRepository;

    public EquipmentReservationService(SpringDataStudentRepository studentRepository, EquipmentRepository equipmentRepository, SpringDataEquipmentReservationRepository reservationRepository) {
        this.studentRepository = studentRepository;
        this.equipmentRepository = equipmentRepository;
        this.reservationRepository = reservationRepository;
    }

    public void addReservation(int id, int studentId, int equipmentItemId, LocalDate startDate, LocalDate endDate) {
        Student student = studentRepository.findById(studentId).orElseThrow();
        EquipmentItem item = equipmentRepository.findById(equipmentItemId).orElseThrow();
        EquipmentReservation reservation = new EquipmentReservation(id, student, item, startDate, endDate);
        reservationRepository.save(reservation);
    }

    public Optional<EquipmentReservation> getReservation(int id) {
        return reservationRepository.findById(id);
    }
}
