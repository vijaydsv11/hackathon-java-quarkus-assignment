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
 * Unit tests for SearchWarehouseUseCase using mocks
 * These tests properly instrument for JaCoCo code coverage
 */
public class SearchWarehouseUseCaseUnitTest {

    private WarehouseStore warehouseStore;
    private SearchWarehouseUseCase searchWarehouseUseCase;

    @BeforeEach
    void setup() {
        warehouseStore = mock(WarehouseStore.class);
        searchWarehouseUseCase = new SearchWarehouseUseCase(warehouseStore);
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
    void testSearchWithAllFiltersApplied() {
        Warehouse warehouse = createWarehouse("WH-001", "AMSTERDAM-001", 100, 50);
        when(warehouseStore.search("AMSTERDAM-001", 50, 200, 0, 10, "createdAt", "asc"))
                .thenReturn(List.of(warehouse));

        List<Warehouse> result = searchWarehouseUseCase.search(
                "AMSTERDAM-001", 50, 200, 0, 10, "createdAt", "asc"
        );

        assertEquals(1, result.size());
        assertEquals("WH-001", result.get(0).businessUnitCode);
        verify(warehouseStore).search("AMSTERDAM-001", 50, 200, 0, 10, "createdAt", "asc");
    }

    @Test
    void testSearchReturnsMultipleResults() {
        Warehouse wh1 = createWarehouse("WH-001", "AMSTERDAM-001", 100, 50);
        Warehouse wh2 = createWarehouse("WH-002", "AMSTERDAM-001", 80, 40);
        Warehouse wh3 = createWarehouse("WH-003", "AMSTERDAM-001", 120, 60);

        when(warehouseStore.search("AMSTERDAM-001", null, null, 0, 10, "createdAt", "asc"))
                .thenReturn(List.of(wh1, wh2, wh3));

        List<Warehouse> result = searchWarehouseUseCase.search(
                "AMSTERDAM-001", null, null, 0, 10, "createdAt", "asc"
        );

        assertEquals(3, result.size());
    }

    @Test
    void testSearchReturnsEmptyListWhenNoMatch() {
        when(warehouseStore.search("UNKNOWN-LOC", null, null, 0, 10, "createdAt", "asc"))
                .thenReturn(new ArrayList<>());

        List<Warehouse> result = searchWarehouseUseCase.search(
                "UNKNOWN-LOC", null, null, 0, 10, "createdAt", "asc"
        );

        assertTrue(result.isEmpty());
        verify(warehouseStore).search("UNKNOWN-LOC", null, null, 0, 10, "createdAt", "asc");
    }

    @Test
    void testSearchWithNullLocationFilter() {
        Warehouse wh1 = createWarehouse("WH-001", "AMSTERDAM-001", 100, 50);
        Warehouse wh2 = createWarehouse("WH-002", "ZWOLLE-001", 50, 25);

        when(warehouseStore.search(null, null, null, 0, 10, "createdAt", "asc"))
                .thenReturn(List.of(wh1, wh2));

        List<Warehouse> result = searchWarehouseUseCase.search(
                null, null, null, 0, 10, "createdAt", "asc"
        );

        assertEquals(2, result.size());
    }

    @Test
    void testSearchWithCapacityRangeFilter() {
        Warehouse wh1 = createWarehouse("WH-001", "AMSTERDAM-001", 50, 25);
        Warehouse wh2 = createWarehouse("WH-002", "AMSTERDAM-001", 100, 50);

        when(warehouseStore.search("AMSTERDAM-001", 40, 150, 0, 10, "capacity", "desc"))
                .thenReturn(List.of(wh2, wh1));

        List<Warehouse> result = searchWarehouseUseCase.search(
                "AMSTERDAM-001", 40, 150, 0, 10, "capacity", "desc"
        );

        assertEquals(2, result.size());
    }

    @Test
    void testSearchWithPaginationOffset() {
        Warehouse wh1 = createWarehouse("WH-001", "AMSTERDAM-001", 100, 50);
        Warehouse wh2 = createWarehouse("WH-002", "AMSTERDAM-001", 80, 40);

        when(warehouseStore.search("AMSTERDAM-001", null, null, 5, 10, "createdAt", "asc"))
                .thenReturn(List.of(wh2));

        List<Warehouse> result = searchWarehouseUseCase.search(
                "AMSTERDAM-001", null, null, 5, 10, "createdAt", "asc"
        );

        assertEquals(1, result.size());
        verify(warehouseStore).search("AMSTERDAM-001", null, null, 5, 10, "createdAt", "asc");
    }

    @Test
    void testSearchWithDifferentSortOrders() {
        Warehouse wh1 = createWarehouse("WH-001", "AMSTERDAM-001", 100, 50);
        Warehouse wh2 = createWarehouse("WH-002", "AMSTERDAM-001", 50, 25);

        // Ascending sort
        when(warehouseStore.search("AMSTERDAM-001", null, null, 0, 10, "capacity", "asc"))
                .thenReturn(List.of(wh2, wh1));

        List<Warehouse> resultAsc = searchWarehouseUseCase.search(
                "AMSTERDAM-001", null, null, 0, 10, "capacity", "asc"
        );

        // Descending sort
        when(warehouseStore.search("AMSTERDAM-001", null, null, 0, 10, "capacity", "desc"))
                .thenReturn(List.of(wh1, wh2));

        List<Warehouse> resultDesc = searchWarehouseUseCase.search(
                "AMSTERDAM-001", null, null, 0, 10, "capacity", "desc"
        );

        assertEquals(2, resultAsc.size());
        assertEquals(2, resultDesc.size());
    }

    @Test
    void testSearchPassesCorrectParametersToStore() {
        when(warehouseStore.search("TILBURG-001", 30, 80, 0, 25, "stock", "desc"))
                .thenReturn(new ArrayList<>());

        searchWarehouseUseCase.search("TILBURG-001", 30, 80, 0, 25, "stock", "desc");

        verify(warehouseStore).search("TILBURG-001", 30, 80, 0, 25, "stock", "desc");
    }

    @Test
    void testSearchWithMinMaxCapacityBoundaries() {
        Warehouse wh = createWarehouse("WH-MIN-MAX", "AMSTERDAM-001", 100, 50);
        
        // Test with min capacity
        when(warehouseStore.search("AMSTERDAM-001", 1, null, 0, 10, "createdAt", "asc"))
                .thenReturn(List.of(wh));

        List<Warehouse> result = searchWarehouseUseCase.search(
                "AMSTERDAM-001", 1, null, 0, 10, "createdAt", "asc"
        );

        assertEquals(1, result.size());
    }
}
