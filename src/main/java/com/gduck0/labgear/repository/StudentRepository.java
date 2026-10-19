package com.gduck0.labgear.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gduck0.labgear.entity.Student;

public interface StudentRepository
        extends JpaRepository<Student, Integer>{

}
