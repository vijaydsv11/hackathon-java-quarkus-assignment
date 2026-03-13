package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetWarehouseUseCaseTest {

    private WarehouseStore warehouseStore;
    private GetWarehouseUseCase getWarehouseUseCase;

    @BeforeEach
    void setup() {
        warehouseStore = mock(WarehouseStore.class);

        getWarehouseUseCase = new GetWarehouseUseCase();
        getWarehouseUseCase.warehouseStore = warehouseStore;
    }

    @Test
    void shouldReturnAllWarehouses() {

        Warehouse w1 = new Warehouse();
        w1.businessUnitCode = "BU-001";

        Warehouse w2 = new Warehouse();
        w2.businessUnitCode = "BU-002";

        when(warehouseStore.getAll()).thenReturn(List.of(w1, w2));

        List<Warehouse> result = getWarehouseUseCase.findAll();

        assertEquals(2, result.size());
        verify(warehouseStore).getAll();
    }

    @Test
    void shouldReturnWarehouseByBusinessUnitCode() {

        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "BU-123";

        when(warehouseStore.findByBusinessUnitCode("BU-123"))
                .thenReturn(warehouse);

        Warehouse result = getWarehouseUseCase.findByBusinessUnitCode("BU-123");

        assertNotNull(result);
        assertEquals("BU-123", result.businessUnitCode);
        verify(warehouseStore).findByBusinessUnitCode("BU-123");
    }
}

