package com.fulfilment.application.monolith.warehouses.domain.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class WarehouseTest {

    private Warehouse warehouse;

    @BeforeEach
    void setup() {
        warehouse = Warehouse.reconstruct(
                "TEST-WH-001",
                "AMSTERDAM-001",
                100,
                50,
                LocalDateTime.now(),
                null
        );
    }

    // ============ addStock() Tests ============

    @Test
    void testAddStockSuccessfully() {
        warehouse.addStock(10);
        assertEquals(60, warehouse.stock);
    }

    @Test
    void testAddStockMaxCapacity() {
        warehouse.stock = 90;
        warehouse.addStock(10);
        assertEquals(100, warehouse.stock);
    }

    @Test
    void testAddStockThrowsWhenQuantityIsZero() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> warehouse.addStock(0)
        );
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    void testAddStockThrowsWhenQuantityIsNegative() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> warehouse.addStock(-5)
        );
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    void testAddStockThrowsWhenExceedsCapacity() {
        warehouse.stock = 95;
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> warehouse.addStock(10)  // 95 + 10 = 105 > 100
        );
        assertTrue(ex.getMessage().contains("Stock cannot exceed capacity"));
    }

    @Test
    void testAddStockThrowsWhenWarehouseIsArchived() {
        warehouse.archivedAt = LocalDateTime.now();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> warehouse.addStock(10)
        );
        assertTrue(ex.getMessage().contains("Archived warehouse cannot be modified"));
    }

    // ============ archive() Tests ============

    @Test
    void testArchiveSuccessfully() {
        assertNull(warehouse.archivedAt);
        warehouse.archive();
        assertNotNull(warehouse.archivedAt);
    }

    @Test
    void testArchiveThrowsWhenAlreadyArchived() {
        warehouse.archivedAt = LocalDateTime.now();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> warehouse.archive()
        );
        assertTrue(ex.getMessage().contains("already archived"));
    }

    // ============ create() Factory Method Tests ============

    @Test
    void testCreateWarehouseSuccessfully() {
        Warehouse created = Warehouse.create("NEW-WH-001", "ZWOLLE-001", 30, 40);

        assertEquals("NEW-WH-001", created.businessUnitCode);
        assertEquals("ZWOLLE-001", created.location);
        assertEquals(30, created.capacity);
        assertEquals(0, created.stock);
        assertNull(created.archivedAt);
        assertNotNull(created.createdAt);
    }

    @Test
    void testCreateThrowsWhenBusinessUnitCodeIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Warehouse.create(null, "AMSTERDAM-001", 50, 100)
        );
        assertTrue(ex.getMessage().contains("Business unit code must not be blank"));
    }

    @Test
    void testCreateThrowsWhenBusinessUnitCodeIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Warehouse.create("   ", "AMSTERDAM-001", 50, 100)
        );
        assertTrue(ex.getMessage().contains("Business unit code must not be blank"));
    }

    @Test
    void testCreateThrowsWhenLocationIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Warehouse.create("WH-001", null, 50, 100)
        );
        assertTrue(ex.getMessage().contains("Location must not be blank"));
    }

    @Test
    void testCreateThrowsWhenLocationIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Warehouse.create("WH-001", "   ", 50, 100)
        );
        assertTrue(ex.getMessage().contains("Location must not be blank"));
    }

    @Test
    void testCreateThrowsWhenCapacityIsZero() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Warehouse.create("WH-001", "AMSTERDAM-001", 0, 100)
        );
        assertTrue(ex.getMessage().contains("Capacity must be positive"));
    }

    @Test
    void testCreateThrowsWhenCapacityIsNegative() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Warehouse.create("WH-001", "AMSTERDAM-001", -10, 100)
        );
        assertTrue(ex.getMessage().contains("Capacity must be positive"));
    }

    @Test
    void testCreateThrowsWhenCapacityExceedsLocationMax() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Warehouse.create("WH-001", "ZWOLLE-001", 50, 40)  // 50 > 40
        );
        assertTrue(ex.getMessage().contains("exceeds location max capacity"));
    }

    // ============ reconstruct() Factory Method Tests ============

    @Test
    void testReconstructSuccessfully() {
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime archivedAt = LocalDateTime.now().plusDays(1);

        Warehouse reconstructed = Warehouse.reconstruct(
                "RECONSTRUCT-001",
                "TILBURG-001",
                40,
                20,
                createdAt,
                archivedAt
        );

        assertEquals("RECONSTRUCT-001", reconstructed.businessUnitCode);
        assertEquals("TILBURG-001", reconstructed.location);
        assertEquals(40, reconstructed.capacity);
        assertEquals(20, reconstructed.stock);
        assertEquals(createdAt, reconstructed.createdAt);
        assertEquals(archivedAt, reconstructed.archivedAt);
    }

    @Test
    void testReconstructWithNullArchivedAt() {
        Warehouse reconstructed = Warehouse.reconstruct(
                "RECONSTRUCT-002",
                "AMSTERDAM-001",
                100,
                50,
                LocalDateTime.now(),
                null
        );

        assertNull(reconstructed.archivedAt);
    }
}
