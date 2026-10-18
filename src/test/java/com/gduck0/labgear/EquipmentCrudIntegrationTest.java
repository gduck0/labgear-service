
package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.gduck0.labgear.entity.EquipmentItem;
import com.gduck0.labgear.service.EquipmentService;
import com.gduck0.labgear.repository.SpringDataEquipmentRepository;
import com.gduck0.labgear.exception.DuplicateResourceException;

@SpringBootTest
@Transactional
class EquipmentCrudIntegrationTest {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private SpringDataEquipmentRepository equipmentRepository;

    @Test
    void equipmentCrudWorks() {
        int id = ThreadLocalRandom.current()
                .nextInt(100_000_000, Integer.MAX_VALUE);

        String originalName = "TEST-CAMERA";
        String changedName = "TEST-CAMERA-UPDATED";

        assertFalse(equipmentRepository.findById(id).isPresent());

        EquipmentItem item = new EquipmentItem(id, originalName);
        equipmentService.addEquipment(item);

        EquipmentItem savedItem =
                equipmentService.getEquipmentById(id);

        assertEquals(id, savedItem.getId());
        assertEquals(originalName, savedItem.getName());

        equipmentService.changeEquipmentName(id, changedName);

        EquipmentItem updatedItem =
                equipmentService.getEquipmentById(id);

        assertEquals(changedName, updatedItem.getName());

        equipmentService.deleteEquipment(id);

        assertFalse(equipmentRepository.findById(id).isPresent());
    }

    @Test
    void duplicateEquipmentIdDoesNotOverwriteExistingItem() {
        int id = ThreadLocalRandom.current()
                .nextInt(100_000_000, Integer.MAX_VALUE);

        assertFalse(equipmentRepository.findById(id).isPresent());

        equipmentService.addEquipment(
                new EquipmentItem(id, "TEST-CAMERA")
        );

        assertThrows(DuplicateResourceException.class, () -> {
            equipmentService.addEquipment(
                    new EquipmentItem(id, "TEST-LAPTOP")
            );
        });

        EquipmentItem unchanged =
                equipmentService.getEquipmentById(id);

        assertEquals("TEST-CAMERA", unchanged.getName());
    }
}
