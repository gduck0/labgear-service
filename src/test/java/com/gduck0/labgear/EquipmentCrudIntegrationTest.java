package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.gduck0.labgear.entity.EquipmentItem;
import com.gduck0.labgear.service.EquipmentService;

@SpringBootTest
class EquipmentCrudIntegrationTest {

    @Autowired
    private EquipmentService equipmentService;

    @Test
    void equipmentCrudWorks() {
        int id = 9001;
        String originalName = "TEST-CAMERA";
        String changedName = "TEST-CAMERA-UPDATED";

        equipmentService.getEquipmentById(id)
                .ifPresent(item -> equipmentService.deleteEquipment(id));

        EquipmentItem item = new EquipmentItem(id, originalName);
        equipmentService.addEquipment(item);

        EquipmentItem savedItem =
                equipmentService.getEquipmentById(id).orElseThrow();

        assertEquals(id, savedItem.getId());
        assertEquals(originalName, savedItem.getName());

        equipmentService.changeEquipmentName(id, changedName);

        EquipmentItem updatedItem =
                equipmentService.getEquipmentById(id).orElseThrow();

        assertEquals(changedName, updatedItem.getName());

        equipmentService.deleteEquipment(id);

        assertFalse(equipmentService.getEquipmentById(id).isPresent());
    }
}