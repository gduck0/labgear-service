package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.service.EquipmentService;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.exception.ResourceConflictException;

// Service에서 기자재 CRUD와 중복 ID 충돌을 검증한다.
@SpringBootTest
@Transactional
class EquipmentServiceIntegrationTest {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Test
    void equipmentCrudWorks() {
        int id = ThreadLocalRandom.current()
                .nextInt(100_000_000, Integer.MAX_VALUE);

        String originalName = "TEST-CAMERA";
        String changedName = "TEST-CAMERA-UPDATED";

        assertFalse(equipmentRepository.findById(id).isPresent());

        Equipment item = new Equipment(id, originalName);
        equipmentService.createEquipment(item);

        Equipment savedItem =
                equipmentService.getEquipment(id);

        assertEquals(id, savedItem.getId());
        assertEquals(originalName, savedItem.getName());

        equipmentService.updateEquipmentName(id, changedName);

        Equipment updatedItem =
                equipmentService.getEquipment(id);

        assertEquals(changedName, updatedItem.getName());

        equipmentService.deleteEquipment(id);

        assertFalse(equipmentRepository.findById(id).isPresent());
    }

    @Test
    void duplicateEquipmentIdDoesNotOverwriteExistingItem() {
        int id = ThreadLocalRandom.current()
                .nextInt(100_000_000, Integer.MAX_VALUE);

        assertFalse(equipmentRepository.findById(id).isPresent());

        equipmentService.createEquipment(
                new Equipment(id, "TEST-CAMERA")
        );

        assertThrows(ResourceConflictException.class, () -> {
            equipmentService.createEquipment(
                    new Equipment(id, "TEST-LAPTOP")
            );
        });

        Equipment unchanged =
                equipmentService.getEquipment(id);

        assertEquals("TEST-CAMERA", unchanged.getName());
    }
}
