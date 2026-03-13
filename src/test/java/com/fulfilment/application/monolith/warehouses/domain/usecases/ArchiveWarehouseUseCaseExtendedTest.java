package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Extended test suite for ArchiveWarehouseUseCase
 * Covers edge cases and validation paths not covered in main test class
 */
@QuarkusTest
public class ArchiveWarehouseUseCaseExtendedTest {

    @Inject
    WarehouseRepository warehouseRepository;

    @Inject
    ArchiveWarehouseUseCase archiveWarehouseUseCase;

    @Inject
    EntityManager em;

    @BeforeEach
    @Transactional
    public void setup() {
        em.createQuery("DELETE FROM DbWarehouse").executeUpdate();
    }

    @Test
    @Transactional
    public void testArchiveThrowsWhenBusinessUnitCodeIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> archiveWarehouseUseCase.archive(null)
        );
        assertTrue(ex.getMessage().contains("Business unit code is required"));
    }

    @Test
    @Transactional
    public void testArchiveThrowsWhenBusinessUnitCodeIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> archiveWarehouseUseCase.archive("   ")
        );
        assertTrue(ex.getMessage().contains("Business unit code is required"));
    }

    @Test
    @Transactional
    public void testArchiveThrowsWhenBusinessUnitCodeIsEmpty() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> archiveWarehouseUseCase.archive("")
        );
        assertTrue(ex.getMessage().contains("Business unit code is required"));
    }

    @Test
    @Transactional
    public void testArchiveSuccessfullyWithValidCode() {
        Warehouse warehouse = createWarehouse("ARCHIVE-VALID-001", "AMSTERDAM-001");

        archiveWarehouseUseCase.archive("ARCHIVE-VALID-001");

        Warehouse archived = warehouseRepository.findByBusinessUnitCode("ARCHIVE-VALID-001");
        assertNotNull(archived);
        assertNotNull(archived.archivedAt);
    }

    @Test
    @Transactional
    public void testArchiveSetsPreciseDatetime() {
        Warehouse warehouse = createWarehouse("ARCHIVE-TIME-001", "ZWOLLE-001");
        
        LocalDateTime beforeArchive = LocalDateTime.now();
        archiveWarehouseUseCase.archive("ARCHIVE-TIME-001");
        LocalDateTime afterArchive = LocalDateTime.now();

        Warehouse archived = warehouseRepository.findByBusinessUnitCode("ARCHIVE-TIME-001");
        
        assertNotNull(archived.archivedAt);
        assertTrue(archived.archivedAt.isAfter(beforeArchive) || archived.archivedAt.isEqual(beforeArchive));
        assertTrue(archived.archivedAt.isBefore(afterArchive) || archived.archivedAt.isEqual(afterArchive));
    }

    // ============ Helper Methods ============

    private Warehouse createWarehouse(String code, String location) {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = code;
        warehouse.location = location;
        warehouse.capacity = 100;
        warehouse.stock = 50;
        warehouse.createdAt = LocalDateTime.now();

        warehouseRepository.create(warehouse);
        return warehouse;
    }
}
