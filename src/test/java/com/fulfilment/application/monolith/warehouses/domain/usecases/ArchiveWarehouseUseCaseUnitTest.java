package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

/**
 * Unit tests for ArchiveWarehouseUseCase using mocks
 * These tests properly instrument for JaCoCo code coverage
 */
public class ArchiveWarehouseUseCaseUnitTest {

    private WarehouseStore warehouseStore;
    private ArchiveWarehouseUseCase archiveWarehouseUseCase;

    @BeforeEach
    void setup() {
        warehouseStore = mock(WarehouseStore.class);
        archiveWarehouseUseCase = new ArchiveWarehouseUseCase(warehouseStore);
    }

    @Test
    void testArchiveSuccessfully() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "TEST-WH-001";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.stock = 50;

        when(warehouseStore.findByBusinessUnitCode("TEST-WH-001"))
                .thenReturn(warehouse);

        archiveWarehouseUseCase.archive("TEST-WH-001");

        verify(warehouseStore).findByBusinessUnitCode("TEST-WH-001");
        verify(warehouseStore).update(warehouse);
        assertNotNull(warehouse.archivedAt);
    }

    @Test
    void testArchiveThrowsWhenBusinessUnitCodeIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> archiveWarehouseUseCase.archive(null));
        assertTrue(ex.getMessage().contains("Business unit code is required"));
    }

    @Test
    void testArchiveThrowsWhenBusinessUnitCodeIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> archiveWarehouseUseCase.archive("   "));
        assertTrue(ex.getMessage().contains("Business unit code is required"));
    }

    @Test
    void testArchiveThrowsWhenBusinessUnitCodeIsEmpty() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> archiveWarehouseUseCase.archive(""));
        assertTrue(ex.getMessage().contains("Business unit code is required"));
    }

    @Test
    void testArchiveThrowsWhenWarehouseDoesNotExist() {
        when(warehouseStore.findByBusinessUnitCode("NON-EXISTENT"))
                .thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> archiveWarehouseUseCase.archive("NON-EXISTENT"));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void testArchiveThrowsWhenAlreadyArchived() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "ALREADY-ARCHIVED";
        warehouse.archivedAt = java.time.LocalDateTime.now();

        when(warehouseStore.findByBusinessUnitCode("ALREADY-ARCHIVED"))
                .thenReturn(warehouse);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> archiveWarehouseUseCase.archive("ALREADY-ARCHIVED"));
        assertTrue(ex.getMessage().contains("already archived"));
    }
}
