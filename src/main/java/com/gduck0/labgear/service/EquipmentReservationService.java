package com.gduck0.labgear.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.gduck0.labgear.entity.EquipmentItem;
import com.gduck0.labgear.entity.Student;
import com.gduck0.labgear.entity.EquipmentReservation;
import com.gduck0.labgear.repository.SpringDataEquipmentRepository;
import com.gduck0.labgear.repository.SpringDataStudentRepository;
import com.gduck0.labgear.repository.SpringDataEquipmentReservationRepository;
import com.gduck0.labgear.exception.ResourceNotFoundException;
import com.gduck0.labgear.exception.ReservationConflictException;
import com.gduck0.labgear.exception.DuplicateResourceException;

@Service
public class EquipmentReservationService {
    private SpringDataStudentRepository studentRepository;
    private SpringDataEquipmentRepository equipmentRepository;
    private SpringDataEquipmentReservationRepository reservationRepository;

    public EquipmentReservationService(SpringDataStudentRepository studentRepository, SpringDataEquipmentRepository equipmentRepository, SpringDataEquipmentReservationRepository reservationRepository) {
        this.studentRepository = studentRepository;
        this.equipmentRepository = equipmentRepository;
        this.reservationRepository = reservationRepository;
    }

    public void addReservation(int id, int studentId, int equipmentItemId, LocalDate startDate, LocalDate endDate) {
        if (reservationRepository.existsById(id)) {
            throw new DuplicateResourceException("이미 존재하는 예약 ID입니다.");
        }
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다. ID: " + studentId));
        EquipmentItem item = equipmentRepository.findById(equipmentItemId).orElseThrow(() -> new ResourceNotFoundException("기자재를 찾을 수 없습니다. ID: " + equipmentItemId));
        boolean overlapping = reservationRepository.existsOverlappingReservation(equipmentItemId, startDate, endDate);
        if (overlapping) {
            throw new ReservationConflictException("이미 예약된 기간입니다.");
        }
        EquipmentReservation reservation = new EquipmentReservation(id, student, item, startDate, endDate);
        reservationRepository.save(reservation);
    }

    public EquipmentReservation getReservation(int id) {
        EquipmentReservation reservation = reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("예약을 찾을 수 없습니다. ID: " + id));
        return reservation;
    }
}
