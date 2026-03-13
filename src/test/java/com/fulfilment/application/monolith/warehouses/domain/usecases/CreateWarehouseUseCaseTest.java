package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CreateWarehouseUseCaseTest {

    private WarehouseStore warehouseStore;
    private LocationResolver locationResolver;
    private CreateWarehouseUseCase createWarehouseUseCase;

    @BeforeEach
    void setup() {

        warehouseStore = mock(WarehouseStore.class);
        locationResolver = mock(LocationResolver.class);

        Location location = new Location("AMSTERDAM-001", 5, 100);

        when(locationResolver.resolveByIdentifier(anyString()))
                .thenReturn(location);

        createWarehouseUseCase =
                new CreateWarehouseUseCase(warehouseStore, locationResolver);
    }

    @Test
    void shouldCreateWarehouseSuccessfully() {

        Warehouse warehouse = buildWarehouse("BU-001", "AMSTERDAM-001");

        createWarehouseUseCase.create(warehouse);

        verify(warehouseStore).create(warehouse);
    }

    @Test
    void shouldThrowExceptionWhenLocationInvalid() {

        Warehouse warehouse = buildWarehouse("BU-002", "INVALID");

        when(locationResolver.resolveByIdentifier("INVALID"))
                .thenReturn(null);

        assertThrows(IllegalArgumentException.class, () ->
                createWarehouseUseCase.create(warehouse));
    }

    @Test
    void shouldThrowExceptionWhenCapacityExceedsLocationMaxCapacity() {

        Warehouse warehouse = buildWarehouse("BU-003", "AMSTERDAM-001");
        warehouse.capacity = 200;

        assertThrows(IllegalArgumentException.class, () ->
                createWarehouseUseCase.create(warehouse));
    }

    @Test
    void shouldThrowExceptionWhenStockExceedsCapacity() {

        Warehouse warehouse = buildWarehouse("BU-004", "AMSTERDAM-001");
        warehouse.capacity = 50;
        warehouse.stock = 60;

        assertThrows(IllegalArgumentException.class, () ->
                createWarehouseUseCase.create(warehouse));
    }

    private Warehouse buildWarehouse(String code, String location) {

        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = code;
        warehouse.location = location;
        warehouse.capacity = 100;
        warehouse.stock = 10;
        warehouse.createdAt = LocalDateTime.now();

        return warehouse;
    }
}
