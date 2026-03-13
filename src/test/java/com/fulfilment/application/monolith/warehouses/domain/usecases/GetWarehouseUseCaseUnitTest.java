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
 * Unit tests for GetWarehouseUseCase using mocks
 * These tests properly instrument for JaCoCo code coverage
 */
public class GetWarehouseUseCaseUnitTest {

    private WarehouseStore warehouseStore;
    private GetWarehouseUseCase getWarehouseUseCase;

    @BeforeEach
    void setup() {
        warehouseStore = mock(WarehouseStore.class);
        getWarehouseUseCase = new GetWarehouseUseCase();
        getWarehouseUseCase.warehouseStore = warehouseStore;
    }

    private Warehouse createWarehouse(String code, String location, int capacity, int stock) {
        return Warehouse.reconstruct(
                code,
                location,
                capacity,
                stock,
                LocalDateTime.now(),
                null
        );
    }

    @Test
    void testFindAllReturnsEmptyListWhenNoWarehouses() {
        when(warehouseStore.getAll()).thenReturn(new ArrayList<>());

        List<Warehouse> result = getWarehouseUseCase.findAll();

        assertTrue(result.isEmpty());
        verify(warehouseStore).getAll();
    }

    @Test
    void testFindAllReturnsSingleWarehouse() {
        Warehouse warehouse = createWarehouse("WH-001", "AMSTERDAM-001", 100, 50);
        when(warehouseStore.getAll()).thenReturn(List.of(warehouse));

        List<Warehouse> result = getWarehouseUseCase.findAll();

        assertEquals(1, result.size());
        assertEquals("WH-001", result.get(0).businessUnitCode);
    }

    @Test
    void testFindAllReturnsMultipleWarehouses() {
        Warehouse wh1 = createWarehouse("WH-001", "AMSTERDAM-001", 100, 50);
        Warehouse wh2 = createWarehouse("WH-002", "ZWOLLE-001", 50, 25);
        Warehouse wh3 = createWarehouse("WH-003", "TILBURG-001", 80, 40);

        when(warehouseStore.getAll()).thenReturn(List.of(wh1, wh2, wh3));

        List<Warehouse> result = getWarehouseUseCase.findAll();

        assertEquals(3, result.size());
        assertEquals("WH-001", result.get(0).businessUnitCode);
        assertEquals("WH-002", result.get(1).businessUnitCode);
        assertEquals("WH-003", result.get(2).businessUnitCode);
    }

    @Test
    void testFindByBusinessUnitCodeReturnsWarehouse() {
        Warehouse warehouse = createWarehouse("WH-FIND", "AMSTERDAM-001", 100, 50);
        when(warehouseStore.findByBusinessUnitCode("WH-FIND")).thenReturn(warehouse);

        Warehouse result = getWarehouseUseCase.findByBusinessUnitCode("WH-FIND");

        assertNotNull(result);
        assertEquals("WH-FIND", result.businessUnitCode);
        assertEquals("AMSTERDAM-001", result.location);
        verify(warehouseStore).findByBusinessUnitCode("WH-FIND");
    }

    @Test
    void testFindByBusinessUnitCodeReturnsNullWhenNotFound() {
        when(warehouseStore.findByBusinessUnitCode("NON-EXISTENT")).thenReturn(null);

        Warehouse result = getWarehouseUseCase.findByBusinessUnitCode("NON-EXISTENT");

        assertNull(result);
        verify(warehouseStore).findByBusinessUnitCode("NON-EXISTENT");
    }

    @Test
    void testFindByBusinessUnitCodeWithDifferentLocations() {
        Warehouse whAmsterdam = createWarehouse("WH-AMS", "AMSTERDAM-001", 100, 50);
        Warehouse whZwolle = createWarehouse("WH-ZWL", "ZWOLLE-001", 50, 25);

        when(warehouseStore.findByBusinessUnitCode("WH-AMS")).thenReturn(whAmsterdam);
        when(warehouseStore.findByBusinessUnitCode("WH-ZWL")).thenReturn(whZwolle);

        Warehouse resultAms = getWarehouseUseCase.findByBusinessUnitCode("WH-AMS");
        Warehouse resultZwl = getWarehouseUseCase.findByBusinessUnitCode("WH-ZWL");

        assertEquals("AMSTERDAM-001", resultAms.location);
        assertEquals("ZWOLLE-001", resultZwl.location);
    }

    @Test
    void testFindByBusinessUnitCodeWithArchivedWarehouse() {
        LocalDateTime archivedAt = LocalDateTime.now().minusDays(1);
        Warehouse warehouse = Warehouse.reconstruct(
                "WH-ARCHIVED",
                "AMSTERDAM-001",
                100,
                50,
                LocalDateTime.now().minusDays(10),
                archivedAt
        );
        
        when(warehouseStore.findByBusinessUnitCode("WH-ARCHIVED")).thenReturn(warehouse);

        Warehouse result = getWarehouseUseCase.findByBusinessUnitCode("WH-ARCHIVED");

        assertNotNull(result);
        assertNotNull(result.archivedAt);
        assertEquals("WH-ARCHIVED", result.businessUnitCode);
    }

    @Test
    void testFindByBusinessUnitCodeWithVaryingCapacities() {
        Warehouse smallWh = createWarehouse("SMALL", "ZWOLLE-001", 30, 10);
        Warehouse largeWh = createWarehouse("LARGE", "AMSTERDAM-001", 200, 100);

        when(warehouseStore.findByBusinessUnitCode("SMALL")).thenReturn(smallWh);
        when(warehouseStore.findByBusinessUnitCode("LARGE")).thenReturn(largeWh);

        Warehouse resultSmall = getWarehouseUseCase.findByBusinessUnitCode("SMALL");
        Warehouse resultLarge = getWarehouseUseCase.findByBusinessUnitCode("LARGE");

        assertEquals(30, resultSmall.capacity);
        assertEquals(200, resultLarge.capacity);
    }

    @Test
    void testFindAllCallsWarehouseStoreGetAll() {
        when(warehouseStore.getAll()).thenReturn(new ArrayList<>());

        getWarehouseUseCase.findAll();

        verify(warehouseStore, times(1)).getAll();
    }

    @Test
    void testFindByBusinessUnitCodeWithSpecialCharactersInCode() {
        Warehouse warehouse = createWarehouse("WH-2024-Q1", "AMSTERDAM-001", 100, 50);
        when(warehouseStore.findByBusinessUnitCode("WH-2024-Q1")).thenReturn(warehouse);

        Warehouse result = getWarehouseUseCase.findByBusinessUnitCode("WH-2024-Q1");

        assertEquals("WH-2024-Q1", result.businessUnitCode);
    }
}
