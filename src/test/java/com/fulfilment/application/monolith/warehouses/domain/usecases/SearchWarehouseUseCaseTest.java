package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchWarehouseUseCaseTest {

    private WarehouseStore warehouseStore;
    private SearchWarehouseUseCase useCase;

    @BeforeEach
    void setup() {
        warehouseStore = mock(WarehouseStore.class);
        useCase = new SearchWarehouseUseCase(warehouseStore);
    }

    private Warehouse warehouse() {
        return Warehouse.reconstruct(
                "MWH.001",
                "AMSTERDAM-001",
                100,
                50,
                LocalDateTime.now(),
                null
        );
    }

    @Test
    void shouldSearchWithAllFilters() {

        when(warehouseStore.search("AMSTERDAM-001", 50, 200, 0, 10, "createdAt", "asc"))
                .thenReturn(List.of(warehouse()));

        List<Warehouse> result = useCase.search(
                "AMSTERDAM-001",
                50,
                200,
                0,
                10,
                "createdAt",
                "asc"
        );

        assertEquals(1, result.size());

        verify(warehouseStore)
                .search("AMSTERDAM-001", 50, 200, 0, 10, "createdAt", "asc");
    }

    @Test
    void shouldSearchByLocationOnly() {

        when(warehouseStore.search("AMSTERDAM-001", null, null, 0, 10, "createdAt", "asc"))
                .thenReturn(List.of(warehouse()));

        List<Warehouse> result = useCase.search(
                "AMSTERDAM-001",
                null,
                null,
                0,
                10,
                "createdAt",
                "asc"
        );

        assertFalse(result.isEmpty());
    }

    @Test
    void shouldSearchByCapacityRange() {

        when(warehouseStore.search(null, 50, 200, 0, 10, "createdAt", "asc"))
                .thenReturn(List.of(warehouse()));

        List<Warehouse> result = useCase.search(
                null,
                50,
                200,
                0,
                10,
                "createdAt",
                "asc"
        );

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnAllWarehousesWhenNoFilters() {

        when(warehouseStore.search(null, null, null, 0, 10, "createdAt", "asc"))
                .thenReturn(List.of(warehouse()));

        List<Warehouse> result = useCase.search(
                null,
                null,
                null,
                0,
                10,
                "createdAt",
                "asc"
        );

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoMatch() {

        when(warehouseStore.search("UNKNOWN", null, null, 0, 10, "createdAt", "asc"))
                .thenReturn(List.of());

        List<Warehouse> result = useCase.search(
                "UNKNOWN",
                null,
                null,
                0,
                10,
                "createdAt",
                "asc"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldPassSortingParameters() {

        when(warehouseStore.search(null, null, null, 0, 10, "capacity", "desc"))
                .thenReturn(List.of(warehouse()));

        useCase.search(
                null,
                null,
                null,
                0,
                10,
                "capacity",
                "desc"
        );

        verify(warehouseStore)
                .search(null, null, null, 0, 10, "capacity", "desc");
    }

    @Test
    void shouldPassPaginationParameters() {

        when(warehouseStore.search(null, null, null, 2, 20, "createdAt", "asc"))
                .thenReturn(List.of(warehouse()));

        useCase.search(
                null,
                null,
                null,
                2,
                20,
                "createdAt",
                "asc"
        );

        verify(warehouseStore)
                .search(null, null, null, 2, 20, "createdAt", "asc");
    }
}