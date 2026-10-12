package com.gduck0.labgear.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
@Table(name = "students")
public class Student {
    @Id
    private int id;
    @Column(nullable = false, length = 100)
    private String name;

    public Student(int id, String name) {
        this.id = id;
        this.name = name;
    }

    protected Student() {
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }
}