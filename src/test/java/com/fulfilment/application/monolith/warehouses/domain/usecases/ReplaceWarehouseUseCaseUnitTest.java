package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

/**
 * Unit tests for ReplaceWarehouseUseCase using mocks
 * These tests properly instrument for JaCoCo code coverage
 */
public class ReplaceWarehouseUseCaseUnitTest {

    private WarehouseStore warehouseStore;
    private LocationResolver locationResolver;
    private ReplaceWarehouseUseCase replaceWarehouseUseCase;

    @BeforeEach
    void setup() {
        warehouseStore = mock(WarehouseStore.class);
        locationResolver = mock(LocationResolver.class);
        replaceWarehouseUseCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
    }

    @Test
    void testReplaceSuccessfully() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "REPLACE-001";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.stock = 50;

        Location newLocation = new Location("ZWOLLE-001", 1, 40);

        when(warehouseStore.findByBusinessUnitCode("REPLACE-001"))
                .thenReturn(warehouse);
        when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
                .thenReturn(newLocation);

        replaceWarehouseUseCase.replace("REPLACE-001", "ZWOLLE-001", 30, 15);

        verify(warehouseStore).update(warehouse);
        assertEquals("ZWOLLE-001", warehouse.location);
        assertEquals(30, warehouse.capacity);
        assertEquals(15, warehouse.stock);
    }

    @Test
    void testReplaceThrowsWhenBusinessUnitCodeIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace(null, "AMSTERDAM-001", 50, 25)
        );
        assertTrue(ex.getMessage().contains("Business unit code is required"));
    }

    @Test
    void testReplaceThrowsWhenBusinessUnitCodeIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("   ", "AMSTERDAM-001", 50, 25)
        );
        assertTrue(ex.getMessage().contains("Business unit code is required"));
    }

    @Test
    void testReplaceThrowsWhenWarehouseDoesNotExist() {
        when(warehouseStore.findByBusinessUnitCode("NON-EXISTENT"))
                .thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("NON-EXISTENT", "AMSTERDAM-001", 50, 25)
        );
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void testReplaceThrowsWhenWarehouseIsArchived() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "ARCHIVED-001";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.archivedAt = java.time.LocalDateTime.now();

        when(warehouseStore.findByBusinessUnitCode("ARCHIVED-001"))
                .thenReturn(warehouse);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("ARCHIVED-001", "ZWOLLE-001", 30, 15)
        );
        assertTrue(ex.getMessage().contains("archived"));
    }



    @Test
    void testReplaceThrowsWhenLocationIsInvalid() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "LOC-INVALID-001";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;

        when(warehouseStore.findByBusinessUnitCode("LOC-INVALID-001"))
                .thenReturn(warehouse);
        when(locationResolver.resolveByIdentifier("INVALID-LOC"))
                .thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("LOC-INVALID-001", "INVALID-LOC", 50, 25)
        );
        assertTrue(ex.getMessage().contains("Location is not valid"));
    }

    @Test
    void testReplaceThrowsWhenCapacityExceedsLocationMax() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CAP-EXCEED-001";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;

        Location newLocation = new Location("ZWOLLE-001", 1, 40);  // max 40

        when(warehouseStore.findByBusinessUnitCode("CAP-EXCEED-001"))
                .thenReturn(warehouse);
        when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
                .thenReturn(newLocation);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("CAP-EXCEED-001", "ZWOLLE-001", 50, 10)
        );
        assertTrue(ex.getMessage().contains("exceeds location max capacity"));
    }

    @Test
    void testReplaceThrowsWhenStockExceedsCapacity() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "STOCK-EXCEED-001";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;

        Location newLocation = new Location("AMSTERDAM-001", 5, 100);

        when(warehouseStore.findByBusinessUnitCode("STOCK-EXCEED-001"))
                .thenReturn(warehouse);
        when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
                .thenReturn(newLocation);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> replaceWarehouseUseCase.replace("STOCK-EXCEED-001", "AMSTERDAM-001", 40, 50)
        );
        assertTrue(ex.getMessage().contains("Stock exceeds warehouse capacity"));
    }

    @Test
    void testReplaceSuccessfullyWithWarehouseObject() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "REPLACE-OBJ-001";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.stock = 50;

        Location newLocation = new Location("TILBURG-001", 1, 40);

        when(warehouseStore.findByBusinessUnitCode("REPLACE-OBJ-001"))
                .thenReturn(warehouse);
        when(locationResolver.resolveByIdentifier("TILBURG-001"))
                .thenReturn(newLocation);

        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "REPLACE-OBJ-001";
        replacement.location = "TILBURG-001";
        replacement.capacity = 30;
        replacement.stock = 15;

        replaceWarehouseUseCase.replace(replacement);

        verify(warehouseStore).update(warehouse);
        assertEquals("TILBURG-001", warehouse.location);
        assertEquals(30, warehouse.capacity);
        assertEquals(15, warehouse.stock);
    }
}
