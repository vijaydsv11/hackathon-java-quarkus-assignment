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
 * Unit tests for CreateWarehouseUseCase using mocks
 * These tests properly instrument for JaCoCo code coverage
 */
public class CreateWarehouseUseCaseUnitTest {

    private WarehouseStore warehouseStore;
    private LocationResolver locationResolver;
    private CreateWarehouseUseCase createWarehouseUseCase;

    @BeforeEach
    void setup() {
        warehouseStore = mock(WarehouseStore.class);
        locationResolver = mock(LocationResolver.class);

        Location amsterdam = new Location("AMSTERDAM-001", 5, 100);
        when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
                .thenReturn(amsterdam);

        when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
                .thenReturn(new Location("ZWOLLE-001", 1, 40));

        createWarehouseUseCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
    }

    @Test
    void testCreateWarehouseSuccessfully() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-001";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.stock = 50;

        createWarehouseUseCase.create(warehouse);

        verify(warehouseStore).create(warehouse);
    }

    @Test
    void testCreateThrowsWhenLocationIsInvalid() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-LOC-INVALID";
        warehouse.location = "INVALID-LOCATION";
        warehouse.capacity = 50;

        when(locationResolver.resolveByIdentifier("INVALID-LOCATION"))
                .thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> createWarehouseUseCase.create(warehouse)
        );
        assertTrue(ex.getMessage().contains("not valid"));
    }

    @Test
    void testCreateThrowsWhenCapacityExceedsLocationMax() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-CAP-EXCEED";
        warehouse.location = "ZWOLLE-001";  // max 40
        warehouse.capacity = 50;  // exceeds max

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> createWarehouseUseCase.create(warehouse)
        );
        assertTrue(ex.getMessage().contains("exceeds location max capacity"));
    }

    @Test
    void testCreateThrowsWhenStockExceedsCapacity() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-STOCK-EXCEED";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 50;
        warehouse.stock = 60;  // exceeds capacity

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> createWarehouseUseCase.create(warehouse)
        );
        assertTrue(ex.getMessage().contains("exceeds warehouse capacity"));
    }

    @Test
    void testCreateSucceedsWithZeroStock() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-ZERO-STOCK";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.stock = 0;

        createWarehouseUseCase.create(warehouse);

        verify(warehouseStore).create(warehouse);
        assertEquals(0, warehouse.stock);
    }

    @Test
    void testCreateSucceedsWithMaxCapacity() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-MAX-CAP";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;  // exactly at max
        warehouse.stock = 50;

        createWarehouseUseCase.create(warehouse);

        verify(warehouseStore).create(warehouse);
        assertEquals(100, warehouse.capacity);
    }

    @Test
    void testCreateSucceedsWithMinCapacity() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-MIN-CAP";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 1;  // minimum positive
        warehouse.stock = 0;

        createWarehouseUseCase.create(warehouse);

        verify(warehouseStore).create(warehouse);
        assertEquals(1, warehouse.capacity);
    }

    // ============ Tests for create(String, String, int) method ============

    @Test
    void testCreateWithStringsSuccessfully() {
        createWarehouseUseCase.create("BU-STRING-001", "AMSTERDAM-001", 75);

        verify(warehouseStore).create(any(Warehouse.class));
    }

    @Test
    void testCreateWithStringsThrowsWhenBusinessUnitCodeIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> createWarehouseUseCase.create(null, "AMSTERDAM-001", 50)
        );
        assertTrue(ex.getMessage().contains("Business unit code cannot be null or blank"));
    }

    @Test
    void testCreateWithStringsThrowsWhenBusinessUnitCodeIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> createWarehouseUseCase.create("   ", "AMSTERDAM-001", 50)
        );
        assertTrue(ex.getMessage().contains("Business unit code cannot be null or blank"));
    }

    @Test
    void testCreateWithStringsThrowsWhenLocationIdentifierIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> createWarehouseUseCase.create("BU-STRING-NULL", null, 50)
        );
        assertTrue(ex.getMessage().contains("Location identifier cannot be null or blank"));
    }

    @Test
    void testCreateWithStringsThrowsWhenLocationIdentifierIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> createWarehouseUseCase.create("BU-STRING-BLANK", "   ", 50)
        );
        assertTrue(ex.getMessage().contains("Location identifier cannot be null or blank"));
    }

    @Test
    void testCreateWithStringsThrowsWhenLocationIsInvalid() {
        when(locationResolver.resolveByIdentifier("INVALID-LOC"))
                .thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> createWarehouseUseCase.create("BU-STRING-INVLOC", "INVALID-LOC", 50)
        );
        assertTrue(ex.getMessage().contains("not valid"));
    }

    @Test
    void testCreateWithStringsSucceedsWithMaxCapacity() {
        createWarehouseUseCase.create("BU-STRING-MAXCAP", "AMSTERDAM-001", 100);

        verify(warehouseStore).create(any(Warehouse.class));
    }

    @Test
    void testCreateWithStringsThrowsWhenDatabaseFails() {
        doThrow(new RuntimeException("Database error"))
                .when(warehouseStore).create(any());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> createWarehouseUseCase.create("BU-STRING-DBERROR", "AMSTERDAM-001", 50)
        );
        assertTrue(ex.getMessage().contains("already exists"));
    }
}
