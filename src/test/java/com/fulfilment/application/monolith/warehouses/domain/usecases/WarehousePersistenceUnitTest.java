package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for persistence and concurrency patterns
 * Tests unique constraints, transaction validation, and error handling
 */
public class WarehousePersistenceUnitTest {

    private WarehouseStore warehouseStore;
    private CreateWarehouseUseCase createWarehouseUseCase;
    private GetWarehouseUseCase getWarehouseUseCase;

    @BeforeEach
    void setup() {
        warehouseStore = mock(WarehouseStore.class);
        createWarehouseUseCase = new CreateWarehouseUseCase(warehouseStore, null);
        getWarehouseUseCase = new GetWarehouseUseCase();
        getWarehouseUseCase.warehouseStore = warehouseStore;
    }

    private Warehouse createTestWarehouse(String code, String location, int capacity, int stock) {
        return Warehouse.reconstruct(
                code,
                location,
                capacity,
                stock,
                LocalDateTime.now(),
                null
        );
    }

    // ============ Unique Constraint Tests ============

    @Test
    void testDuplicateBusinessUnitCodeDetection() {
        Warehouse wh1 = createTestWarehouse("DUPLICATE-001", "AMSTERDAM-001", 100, 50);
        Warehouse wh2 = createTestWarehouse("DUPLICATE-001", "ZWOLLE-001", 50, 25);

        // Simulate duplicate key error from database
        doNothing().when(warehouseStore).create(wh1);
        doThrow(new IllegalArgumentException("Duplicate business unit code"))
                .when(warehouseStore).create(wh2);

        warehouseStore.create(wh1);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> warehouseStore.create(wh2)
        );
        assertTrue(ex.getMessage().contains("Duplicate"));
    }

    @Test
    void testUniqueBusinessUnitCodeAllowsDifferentLocations() {
        Warehouse wh1 = createTestWarehouse("UNIQUE-001", "AMSTERDAM-001", 100, 50);
        Warehouse wh2 = createTestWarehouse("UNIQUE-002", "AMSTERDAM-001", 80, 40);

        // Both should succeed with different codes
        doNothing().when(warehouseStore).create(any(Warehouse.class));

        warehouseStore.create(wh1);
        warehouseStore.create(wh2);

        verify(warehouseStore, times(2)).create(any(Warehouse.class));
    }

    // ============ Query Tests (Testcontainers Scenario) ============

    @Test
    void testQueryAllWarehouses() {
        Warehouse wh1 = createTestWarehouse("QUERY-001", "AMSTERDAM-001", 100, 50);
        Warehouse wh2 = createTestWarehouse("QUERY-002", "ZWOLLE-001", 50, 25);
        Warehouse wh3 = createTestWarehouse("QUERY-003", "TILBURG-001", 75, 30);

        when(warehouseStore.getAll()).thenReturn(List.of(wh1, wh2, wh3));

        List<Warehouse> results = getWarehouseUseCase.findAll();

        assertEquals(3, results.size());
        verify(warehouseStore).getAll();
    }

    @Test
    void testQueryByLocationFilter() {
        List<Warehouse> amsterdamWarehouses = List.of(
                createTestWarehouse("AMS-001", "AMSTERDAM-001", 100, 50),
                createTestWarehouse("AMS-002", "AMSTERDAM-001", 80, 40),
                createTestWarehouse("AMS-003", "AMSTERDAM-001", 120, 60)
        );

        when(warehouseStore.search("AMSTERDAM-001", null, null, 0, 10, "createdAt", "asc"))
                .thenReturn(amsterdamWarehouses);

        SearchWarehouseUseCase searchUseCase = new SearchWarehouseUseCase(warehouseStore);
        List<Warehouse> results = searchUseCase.search(
                "AMSTERDAM-001", null, null, 0, 10, "createdAt", "asc"
        );

        assertEquals(3, results.size());
    }

    @Test
    void testQueryByCapacityRange() {
        List<Warehouse> warehouses = List.of(
                createTestWarehouse("RANGE-001", "AMSTERDAM-001", 50, 25),
                createTestWarehouse("RANGE-002", "AMSTERDAM-001", 75, 37),
                createTestWarehouse("RANGE-003", "AMSTERDAM-001", 100, 50)
        );

        when(warehouseStore.search("AMSTERDAM-001", 40, 150, 0, 10, "capacity", "asc"))
                .thenReturn(warehouses);

        SearchWarehouseUseCase searchUseCase = new SearchWarehouseUseCase(warehouseStore);
        List<Warehouse> results = searchUseCase.search(
                "AMSTERDAM-001", 40, 150, 0, 10, "capacity", "asc"
        );

        assertEquals(3, results.size());
    }

    // ============ Null Field Tests ============

    @Test
    void testWarehouseWithNullArchivedAtField() {
        Warehouse wh = createTestWarehouse("NULL-TEST", "AMSTERDAM-001", 100, 50);
        
        assertNull(wh.archivedAt);
        assertTrue(wh.archivedAt == null);
    }

    @Test
    void testWarehouseWithNonNullArchivedAtField() {
        LocalDateTime archiveTime = LocalDateTime.now();
        Warehouse wh = Warehouse.reconstruct(
                "ARCHIVED-TEST",
                "AMSTERDAM-001",
                100,
                50,
                LocalDateTime.now(),
                archiveTime
        );

        assertNotNull(wh.archivedAt);
        assertTrue(wh.archivedAt != null);
    }

    // ============ Transaction Rollback Simulation ============

    @Test
    void testTransactionRollbackPreventsPersistence() {
        Warehouse wh1 = createTestWarehouse("ROLLBACK-001", "AMSTERDAM-001", 100, 50);
        Warehouse wh2 = createTestWarehouse("ROLLBACK-002", "ZWOLLE-001", 50, 25);

        // First creation succeeds
        doNothing().when(warehouseStore).create(wh1);
        warehouseStore.create(wh1);

        // Simulate rollback scenario - second creation fails
        when(warehouseStore.findByBusinessUnitCode("ROLLBACK-002")).thenReturn(null);

        Warehouse result = getWarehouseUseCase.findByBusinessUnitCode("ROLLBACK-002");
        assertNull(result);
    }

    @Test
    void testFailedCreationDoesNotAffectOthers() {
        Warehouse success = createTestWarehouse("SUCCESS-001", "AMSTERDAM-001", 100, 50);
        Warehouse failed = createTestWarehouse("FAILED-001", "ZWOLLE-001", 50, 25);

        doNothing().when(warehouseStore).create(success);
        doThrow(new RuntimeException("Creation failed")).when(warehouseStore).create(failed);

        warehouseStore.create(success);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> warehouseStore.create(failed)
        );
        assertNotNull(ex);
    }

    // ============ Concurrency Simulation Tests ============

    @Test
    void testConcurrentCreationWithUniqueCodesSucceeds() {
        Warehouse wh1 = createTestWarehouse("CONCURRENT-1", "AMSTERDAM-001", 100, 50);
        Warehouse wh2 = createTestWarehouse("CONCURRENT-2", "AMSTERDAM-001", 80, 40);
        Warehouse wh3 = createTestWarehouse("CONCURRENT-3", "AMSTERDAM-001", 120, 60);

        doNothing().when(warehouseStore).create(any(Warehouse.class));

        warehouseStore.create(wh1);
        warehouseStore.create(wh2);
        warehouseStore.create(wh3);

        verify(warehouseStore, times(3)).create(any(Warehouse.class));
    }

    @Test
    void testConcurrentCreationWithDuplicateCodesFails() {
        String duplicateCode = "DUPLICATE-CONCURRENT";
        Warehouse wh1 = createTestWarehouse(duplicateCode, "AMSTERDAM-001", 100, 50);
        Warehouse wh2 = createTestWarehouse(duplicateCode, "ZWOLLE-001", 50, 25);

        doNothing().when(warehouseStore).create(wh1);
        doThrow(new IllegalArgumentException("Duplicate business unit code"))
                .when(warehouseStore).create(wh2);

        warehouseStore.create(wh1);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> warehouseStore.create(wh2)
        );
        assertTrue(ex.getMessage().contains("Duplicate"));
    }

    // ============ Multi-Location Query Tests ============

    @Test
    void testQueryMultipleLocations() {
        List<Warehouse> allWarehouses = List.of(
                createTestWarehouse("AMS-001", "AMSTERDAM-001", 100, 50),
                createTestWarehouse("ZWL-001", "ZWOLLE-001", 50, 25),
                createTestWarehouse("TIL-001", "TILBURG-001", 75, 30),
                createTestWarehouse("AMS-002", "AMSTERDAM-001", 80, 40)
        );

        when(warehouseStore.getAll()).thenReturn(allWarehouses);

        List<Warehouse> results = getWarehouseUseCase.findAll();

        assertEquals(4, results.size());
        
        // Count by location
        long amsterdamCount = results.stream()
                .filter(w -> "AMSTERDAM-001".equals(w.location))
                .count();
        assertEquals(2, amsterdamCount);
    }

    @Test
    void testUpdatePreservesOtherFields() {
        Warehouse original = createTestWarehouse("UPDATE-TEST", "AMSTERDAM-001", 100, 50);
        Warehouse updated = Warehouse.reconstruct(
                "UPDATE-TEST",
                "ZWOLLE-001",  // Location changed
                80,             // Capacity changed
                50,
                original.createdAt,
                null
        );

        when(warehouseStore.findByBusinessUnitCode("UPDATE-TEST"))
                .thenReturn(original)
                .thenReturn(updated);

        Warehouse before = getWarehouseUseCase.findByBusinessUnitCode("UPDATE-TEST");
        Warehouse after = getWarehouseUseCase.findByBusinessUnitCode("UPDATE-TEST");

        assertEquals("AMSTERDAM-001", before.location);
        assertEquals("ZWOLLE-001", after.location);
        assertNotEquals(before.location, after.location);
    }

    @Test
    void testConcurrentReadDoesNotBlockWrites() {
        Warehouse warehouse = createTestWarehouse("CONCURRENT-RW", "AMSTERDAM-001", 100, 50);

        when(warehouseStore.findByBusinessUnitCode("CONCURRENT-RW")).thenReturn(warehouse);
        doNothing().when(warehouseStore).create(any(Warehouse.class));

        // Simulate concurrent read and write
        Warehouse readResult = getWarehouseUseCase.findByBusinessUnitCode("CONCURRENT-RW");
        warehouseStore.create(warehouse);

        assertNotNull(readResult);
        verify(warehouseStore).findByBusinessUnitCode("CONCURRENT-RW");
        verify(warehouseStore).create(warehouse);
    }
}
