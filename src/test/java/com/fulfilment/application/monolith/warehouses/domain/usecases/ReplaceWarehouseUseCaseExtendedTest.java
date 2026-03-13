package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Extended test suite for ReplaceWarehouseUseCase
 * Covers all validation paths and edge cases
 */
@QuarkusTest
public class ReplaceWarehouseUseCaseExtendedTest {

    @Inject
    WarehouseRepository warehouseRepository;

    @Inject
    LocationResolver locationResolver;

    @Inject
    EntityManager em;

    private ReplaceWarehouseUseCase replaceWarehouseUseCase;

    @BeforeEach
    @Transactional
    public void setup() {
        em.createQuery("DELETE FROM DbWarehouse").executeUpdate();
        replaceWarehouseUseCase = new ReplaceWarehouseUseCase(warehouseRepository, locationResolver);
    }

    // ============ Business Unit Code Validation Tests ============

    @Test
    @Transactional
    public void testReplaceThrowsWhenBusinessUnitCodeIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace(null, "AMSTERDAM-001", 50, 25)
        );
        assertTrue(ex.getMessage().contains("Business unit code is required"));
    }

    @Test
    @Transactional
    public void testReplaceThrowsWhenBusinessUnitCodeIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("   ", "AMSTERDAM-001", 50, 25)
        );
        assertTrue(ex.getMessage().contains("Business unit code is required"));
    }

    @Test
    @Transactional
    public void testReplaceThrowsWhenBusinessUnitCodeIsEmpty() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("", "AMSTERDAM-001", 50, 25)
        );
        assertTrue(ex.getMessage().contains("Business unit code is required"));
    }

    // ============ Warehouse Existence Tests ============

    @Test
    @Transactional
    public void testReplaceThrowsWhenWarehouseDoesNotExist() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("NON-EXISTENT", "AMSTERDAM-001", 50, 25)
        );
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    // ============ Archived Warehouse Tests ============

    @Test
    @Transactional
    public void testReplaceThrowsWhenWarehouseIsArchived() {
        Warehouse archived = createArchivedWarehouse("REPLACE-ARCHIVED-001", "AMSTERDAM-001");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("REPLACE-ARCHIVED-001", "ZWOLLE-001", 30, 15)
        );
        assertTrue(ex.getMessage().contains("archived"));
    }

    // ============ Capacity Validation Tests ============

    @Test
    @Transactional
    public void testReplaceThrowsWhenCapacityIsNegative() {
        createWarehouse("REPLACE-CAP-NEG-001", "AMSTERDAM-001", 50, 25);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("REPLACE-CAP-NEG-001", "AMSTERDAM-001", -10, 0)
        );
        assertTrue(ex.getMessage().contains("Capacity cannot be negative"));
    }

    @Test
    @Transactional
    public void testReplaceThrowsWhenCapacityExceedsLocationMax() {
        createWarehouse("REPLACE-CAP-EXC-001", "AMSTERDAM-001", 50, 25);

        // ZWOLLE-001 has max capacity of 40
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("REPLACE-CAP-EXC-001", "ZWOLLE-001", 50, 10)
        );
        assertTrue(ex.getMessage().contains("exceeds location max capacity"));
    }

    // ============ Stock Validation Tests ============

    @Test
    @Transactional
    public void testReplaceThrowsWhenStockIsNegative() {
        createWarehouse("REPLACE-STOCK-NEG-001", "AMSTERDAM-001", 50, 25);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("REPLACE-STOCK-NEG-001", "AMSTERDAM-001", 50, -5)
        );
        assertTrue(ex.getMessage().contains("Stock cannot be negative"));
    }

    @Test
    @Transactional
    public void testReplaceThrowsWhenStockExceedsCapacity() {
        createWarehouse("REPLACE-STOCK-EXC-001", "AMSTERDAM-001", 50, 25);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("REPLACE-STOCK-EXC-001", "AMSTERDAM-001", 40, 50)
        );
        assertTrue(ex.getMessage().contains("Stock exceeds warehouse capacity"));
    }

    // ============ Location Validation Tests ============

    @Test
    @Transactional
    public void testReplaceThrowsWhenLocationIsInvalid() {
        createWarehouse("REPLACE-LOC-INVALID-001", "AMSTERDAM-001", 50, 25);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("REPLACE-LOC-INVALID-001", "INVALID-LOCATION", 50, 25)
        );
        assertTrue(ex.getMessage().contains("Location is not valid"));
    }

    // ============ Successful Replace Tests ============

    @Test
    @Transactional
    public void testReplaceSuccessfullyUpdatesAllFields() {
        createWarehouse("REPLACE-SUCCESS-001", "AMSTERDAM-001", 100, 50);

        replaceWarehouseUseCase.replace("REPLACE-SUCCESS-001", "ZWOLLE-001", 30, 15);

        Warehouse updated = warehouseRepository.findByBusinessUnitCode("REPLACE-SUCCESS-001");
        assertEquals("ZWOLLE-001", updated.location);
        assertEquals(30, updated.capacity);
        assertEquals(15, updated.stock);
    }

    @Test
    @Transactional
    public void testReplaceSuccessfullyWithMaxCapacity() {
        createWarehouse("REPLACE-MAX-CAP-001", "AMSTERDAM-001", 50, 25);

        // ZWOLLE-001 has max capacity of 40, so replace with 40
        replaceWarehouseUseCase.replace("REPLACE-MAX-CAP-001", "ZWOLLE-001", 40, 20);

        Warehouse updated = warehouseRepository.findByBusinessUnitCode("REPLACE-MAX-CAP-001");
        assertEquals(40, updated.capacity);
        assertEquals("ZWOLLE-001", updated.location);
    }

    @Test
    @Transactional
    public void testReplaceSuccessfullyWithZeroStock() {
        createWarehouse("REPLACE-ZERO-STOCK-001", "AMSTERDAM-001", 50, 25);

        replaceWarehouseUseCase.replace("REPLACE-ZERO-STOCK-001", "AMSTERDAM-001", 50, 0);

        Warehouse updated = warehouseRepository.findByBusinessUnitCode("REPLACE-ZERO-STOCK-001");
        assertEquals(0, updated.stock);
    }

    @Test
    @Transactional
    public void testReplaceSuccessfullyWithMaxStock() {
        createWarehouse("REPLACE-MAX-STOCK-001", "AMSTERDAM-001", 50, 25);

        replaceWarehouseUseCase.replace("REPLACE-MAX-STOCK-001", "AMSTERDAM-001", 50, 50);

        Warehouse updated = warehouseRepository.findByBusinessUnitCode("REPLACE-MAX-STOCK-001");
        assertEquals(50, updated.stock);
        assertEquals(50, updated.capacity);
    }

    @Test
    @Transactional
    public void testReplaceSuccessfullyUsingWarehouseObject() {
        createWarehouse("REPLACE-OBJECT-001", "AMSTERDAM-001", 100, 50);

        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "REPLACE-OBJECT-001";
        replacement.location = "TILBURG-001";
        replacement.capacity = 30;
        replacement.stock = 10;

        replaceWarehouseUseCase.replace(replacement);

        Warehouse updated = warehouseRepository.findByBusinessUnitCode("REPLACE-OBJECT-001");
        assertEquals("TILBURG-001", updated.location);
        assertEquals(30, updated.capacity);
        assertEquals(10, updated.stock);
    }

    // ============ Helper Methods ============

    private Warehouse createWarehouse(String code, String location, int capacity, int stock) {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = code;
        warehouse.location = location;
        warehouse.capacity = capacity;
        warehouse.stock = stock;
        warehouse.createdAt = LocalDateTime.now();

        warehouseRepository.create(warehouse);
        return warehouse;
    }

    private Warehouse createArchivedWarehouse(String code, String location) {
        Warehouse warehouse = createWarehouse(code, location, 50, 25);
        warehouse.archivedAt = LocalDateTime.now();
        warehouseRepository.update(warehouse);
        return warehouse;
    }
}
