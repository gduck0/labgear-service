package com.gduck0.labgear.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "equipment")
public class Equipment {
    @Id
    private int id;
    @Column(nullable = false, length = 100)
    private String name;

    public Equipment(int id, String name) {
        this.id = id;
        this.name = name;
    }

    protected Equipment() {
    }

    public int getId() {
        return this.id;    
    }

    public String getName() {
        return this.name;
    }

    public void updateName(String name) {
        this.name = name;
    }
}