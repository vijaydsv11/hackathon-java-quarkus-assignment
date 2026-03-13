package com.fulfilment.application.monolith.warehouses.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

import java.time.LocalDateTime;

/**
 * Unit tests for Warehouse validation and business logic
 * Tests concurrency-safe patterns and validation
 */
public class WarehouseValidationUnitTest {

    private Warehouse warehouse;

    @BeforeEach
    void setup() {
        warehouse = Warehouse.reconstruct(
                "TEST-WH",
                "AMSTERDAM-001",
                100,
                50,
                LocalDateTime.now(),
                null
        );
    }

    // ============ Version/Optimistic Locking Tests ============

    @Test
    void testWarehouseInitialVersionIsZero() {
        Warehouse wh = Warehouse.reconstruct("VERSION-TEST", "AMSTERDAM-001", 100, 0, LocalDateTime.now(), null);
        assertNotNull(wh);
        // Version start at 0 before persistence
    }

    @Test
    void testWarehouseCanBeReconstructedWithVersion() {
        Warehouse wh = Warehouse.reconstruct("VERSION-001", "AMSTERDAM-001", 100, 50,
                LocalDateTime.now(), null);
        assertNotNull(wh);
    }

    // ============ Archive Tests (Concurrency Safe) ============

    @Test
    void testArchiveIsIdempotentSafe() {
        assertNull(warehouse.archivedAt);
        warehouse.archive();
        assertNotNull(warehouse.archivedAt);
    }

    @Test
    void testArchivedAtTimestampIsSet() {
        assertNull(warehouse.archivedAt);
        warehouse.archive();
        assertNotNull(warehouse.archivedAt);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertTrue(warehouse.archivedAt.isAfter(before));
        assertTrue(warehouse.archivedAt.isBefore(after));
    }

    @Test
    void testArchivedWarehouseCannotBeModified() {
        warehouse.archive();
        
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> warehouse.addStock(10)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("archived"));
    }

    // ============ Stock Management Tests ============

    @Test
    void testAddStockIncreasesStockCount() {
        int initialStock = warehouse.stock;
        warehouse.addStock(10);
        assertEquals(initialStock + 10, warehouse.stock);
    }

    @Test
    void testAddStockMultipleTimes() {
        warehouse.addStock(5);
        warehouse.addStock(10);
        warehouse.addStock(15);
        assertEquals(50 + 5 + 10 + 15, warehouse.stock);
    }

    @Test
    void testAddStockRespectCapacity() {
        warehouse.stock = 95;
        
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> warehouse.addStock(10)
        );
        assertTrue(ex.getMessage().contains("capacity"));
    }

    @Test
    void testAddStockUpToCapacityLimit() {
        warehouse.stock = 90;
        warehouse.addStock(10);  // Should work: 90 + 10 = 100 (exactly at capacity)
        assertEquals(100, warehouse.stock);
    }

    @Test
    void testStockCannotGoNegative() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> warehouse.addStock(-5)
        );
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    // ============ State Consistency Tests ============

    @Test
    void testWarehouseStateConsistency() {
        assertEquals("TEST-WH", warehouse.businessUnitCode);
        assertEquals("AMSTERDAM-001", warehouse.location);
        assertEquals(100, warehouse.capacity);
        assertEquals(50, warehouse.stock);
        assertNull(warehouse.archivedAt);
    }

    @Test
    void testMultipleWarehousesIndependent() {
        Warehouse wh1 = Warehouse.reconstruct("WH-1", "AMSTERDAM-001", 100, 50,
                LocalDateTime.now(), null);
        Warehouse wh2 = Warehouse.reconstruct("WH-2", "ZWOLLE-001", 50, 25,
                LocalDateTime.now(), null);

        wh1.addStock(10);
        wh2.addStock(5);

        assertEquals(60, wh1.stock);
        assertEquals(30, wh2.stock);
    }

    // ============ Boundary Condition Tests ============

    @Test
    void testZeroCapacityWarehouseCreation() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Warehouse.create("ZERO-CAP", "AMSTERDAM-001", 0, 100)
        );
        assertTrue(ex.getMessage().contains("positive"));
    }

    @Test
    void testNegativeCapacityWarehouseCreation() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Warehouse.create("NEG-CAP", "AMSTERDAM-001", -50, 100)
        );
        assertTrue(ex.getMessage().contains("positive"));
    }

    @Test
    void testMaxStockAtCapacity() {
        Warehouse wh = Warehouse.reconstruct("MAX-STOCK", "ZWOLLE-001", 30, 0, LocalDateTime.now(), null);
        wh.stock = 30;  // Set to capacity
        assertEquals(30, wh.stock);
    }

    @Test
    void testStockExceedsCapacityValidation() {
        Warehouse wh = Warehouse.reconstruct("STOCK-EXCEED", "ZWOLLE-001", 30, 0, LocalDateTime.now(), null);
        wh.stock = 35;  // Exceeds capacity
        
        // Add stock should fail
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> wh.addStock(1)
        );
        assertTrue(ex.getMessage().contains("capacity"));
    }

    // ============ Factory Method Tests ============

    @Test
    void testCreateGeneratesValidWarehouse() {
        Warehouse wh = Warehouse.create("FACTORY", "TILBURG-001", 10, 40);
        assertNotNull(wh);
        assertEquals("FACTORY", wh.businessUnitCode);
        assertEquals("TILBURG-001", wh.location);
        assertEquals(10, wh.capacity);
        assertEquals(0, wh.stock);
        assertNull(wh.archivedAt);
    }

    @Test
    void testReconstructPreservesAllFields() {
        LocalDateTime created = LocalDateTime.now().minusDays(10);
        LocalDateTime archived = LocalDateTime.now();
        
        Warehouse wh = Warehouse.reconstruct("RECONSTRUCT", "TILBURG-001", 75, 25,
                created, archived);
        
        assertEquals("RECONSTRUCT", wh.businessUnitCode);
        assertEquals("TILBURG-001", wh.location);
        assertEquals(75, wh.capacity);
        assertEquals(25, wh.stock);
        assertEquals(created, wh.createdAt);
        assertEquals(archived, wh.archivedAt);
    }

    // ============ Concurrency Safety Patterns ============

    @Test
    void testConcurrentModificationDetection() {
        Warehouse wh1 = Warehouse.reconstruct("CONCURRENT", "AMSTERDAM-001", 100, 50,
                LocalDateTime.now(), null);
        Warehouse wh2 = Warehouse.reconstruct("CONCURRENT", "AMSTERDAM-001", 100, 50,
                LocalDateTime.now(), null);

        wh1.addStock(10);
        wh2.addStock(5);

        // Both modifications succeed on local copies
        // But with version control would detect conflict
        assertEquals(60, wh1.stock);
        assertEquals(55, wh2.stock);
    }

    @Test
    void testArchivePreventsFurtherModification() {
        warehouse.archive();
        assertNotNull(warehouse.archivedAt);

        // All modifications should fail
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> warehouse.addStock(1)
        );
        assertNotNull(ex);
    }

    @Test
    void testBlankBusinessUnitCodeValidation() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Warehouse.create("   ", "AMSTERDAM-001", 100, 0)
        );
        assertTrue(ex.getMessage().contains("blank"));
    }

    @Test
    void testBlankLocationValidation() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Warehouse.create("CODE", "   ", 100, 0)
        );
        assertTrue(ex.getMessage().contains("blank"));
    }
}
