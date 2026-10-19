package com.gduck0.labgear.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Student;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.StudentRepository;
import com.gduck0.labgear.repository.ReservationRepository;
import com.gduck0.labgear.exception.ResourceNotFoundException;
import com.gduck0.labgear.exception.ResourceConflictException;

@Service
public class ReservationService {
    private final StudentRepository studentRepository;
    private final EquipmentRepository equipmentRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(StudentRepository studentRepository, EquipmentRepository equipmentRepository, ReservationRepository reservationRepository) {
        this.studentRepository = studentRepository;
        this.equipmentRepository = equipmentRepository;
        this.reservationRepository = reservationRepository;
    }

    public void createReservation(int id, int studentId, int equipmentId, LocalDate startDate, LocalDate endDate) {
        if (reservationRepository.existsById(id)) {
            throw new ResourceConflictException("이미 존재하는 예약 ID입니다.");
        }
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다. ID: " + studentId));
        Equipment item = equipmentRepository.findById(equipmentId).orElseThrow(() -> new ResourceNotFoundException("기자재를 찾을 수 없습니다. ID: " + equipmentId));
        boolean hasOverlap = reservationRepository.existsOverlappingReservation(equipmentId, startDate, endDate);
        if (hasOverlap) {
            throw new ResourceConflictException("이미 예약된 기간입니다.");
        }
        Reservation reservation = new Reservation(id, student, item, startDate, endDate);
        reservationRepository.save(reservation);
    }

    public Reservation getReservation(int id) {
        Reservation reservation = reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("예약을 찾을 수 없습니다. ID: " + id));
        return reservation;
    }
}
