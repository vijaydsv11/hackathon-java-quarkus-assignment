package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.usecases.GetWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.SearchWarehouseUseCase;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for WarehouseResourceImpl (REST API adapter layer)
 * Tests parameter translation, DTO conversion, and endpoint handling
 */
public class WarehouseResourceImplUnitTest {

    @Mock
    private CreateWarehouseOperation createWarehouseOperation;

    @Mock
    private ReplaceWarehouseOperation replaceWarehouseOperation;

    @Mock
    private ArchiveWarehouseOperation archiveWarehouseOperation;

    @Mock
    private GetWarehouseUseCase getWarehouseUseCase;

    @Mock
    private SearchWarehouseUseCase searchWarehouseUseCase;

    @InjectMocks
    private WarehouseResourceImpl warehouseResourceImpl;

    private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse testDomainWarehouse;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        testDomainWarehouse = com.fulfilment.application.monolith.warehouses.domain.models.Warehouse.reconstruct(
                "TEST-WH-001",
                "AMSTERDAM-001",
                100,
                50,
                LocalDateTime.now(),
                null
        );
    }

    // ============ Search Endpoint Tests ============

    @Test
    void testSearchWarehousesWithAllFilters() {
        // Setup
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> mockResults = new ArrayList<>();
        mockResults.add(testDomainWarehouse);
        
        when(searchWarehouseUseCase.search(
                "AMSTERDAM-001",
                50,
                150,
                0,
                10,
                "createdAt",
                "asc"
        )).thenReturn(mockResults);

        // Execute
        List<com.warehouse.api.beans.Warehouse> result = warehouseResourceImpl.searchWarehouses(
                "AMSTERDAM-001",
                BigInteger.valueOf(50),
                BigInteger.valueOf(150),
                "createdAt",
                "asc",
                BigInteger.ZERO,
                BigInteger.TEN
        );

        // Assert
        assertNotNull(result, "Search result should not be null");
        assertTrue(result.size() > 0, "Search should return results");
        verify(searchWarehouseUseCase, times(1)).search(
                "AMSTERDAM-001",
                50,
                150,
                0,
                10,
                "createdAt",
                "asc"
        );
    }

    @Test
    void testSearchWarehousesWithNullFilters() {
        // Setup
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> mockResults = new ArrayList<>();
        mockResults.add(testDomainWarehouse);
        
        when(searchWarehouseUseCase.search(
                null,
                null,
                null,
                0,
                10,
                null,
                null
        )).thenReturn(mockResults);

        // Execute
        List<com.warehouse.api.beans.Warehouse> result = warehouseResourceImpl.searchWarehouses(
                null,
                null,
                null,
                null,
                null,
                BigInteger.ZERO,
                BigInteger.TEN
        );

        // Assert
        assertNotNull(result, "Search result with null filters should work");
        assertEquals(1, result.size(), "Should return results");
    }

    @Test
    void testSearchWarehousesReturnsEmpty() {
        // Setup
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> mockResults = new ArrayList<>();
        
        when(searchWarehouseUseCase.search(
                any(),
                any(),
                any(),
                anyInt(),
                anyInt(),
                any(),
                any()
        )).thenReturn(mockResults);

        // Execute
        List<com.warehouse.api.beans.Warehouse> result = warehouseResourceImpl.searchWarehouses(
                "UNKNOWN-LOCATION",
                BigInteger.ZERO,
                BigInteger.valueOf(1000),
                "createdAt",
                "asc",
                BigInteger.ZERO,
                BigInteger.TEN
        );

        // Assert
        assertNotNull(result, "Empty result should return empty list");
        assertEquals(0, result.size(), "No results for unknown location");
    }

    // ============ List All Warehouses Endpoint Tests ============

    @Test
    void testListAllWarehouses() {
        // Setup
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> mockResults = new ArrayList<>();
        mockResults.add(testDomainWarehouse);
        com.fulfilment.application.monolith.warehouses.domain.models.Warehouse wh2 = 
            com.fulfilment.application.monolith.warehouses.domain.models.Warehouse.reconstruct("TEST-WH-002", "ZWOLLE-001", 80, 40, LocalDateTime.now(), null);
        mockResults.add(wh2);

        when(getWarehouseUseCase.findAll()).thenReturn(mockResults);

        // Execute
        List<com.warehouse.api.beans.Warehouse> result = warehouseResourceImpl.listAllWarehousesUnits();

        // Assert
        assertNotNull(result, "List all result should not be null");
        assertEquals(2, result.size(), "Should return 2 warehouses");
        verify(getWarehouseUseCase, times(1)).findAll();
    }

    @Test
    void testListAllWarehousesEmpty() {
        // Setup
        when(getWarehouseUseCase.findAll()).thenReturn(new ArrayList<>());

        // Execute
        List<com.warehouse.api.beans.Warehouse> result = warehouseResourceImpl.listAllWarehousesUnits();

        // Assert
        assertNotNull(result, "Result should be empty list");
        assertEquals(0, result.size(), "Should return empty list");
        verify(getWarehouseUseCase, times(1)).findAll();
    }

    // ============ Search Parameter Translation Tests ============

    @Test
    void testSearchParameterTranslation() {
        // Setup - Verify BigInteger to int conversion
        when(searchWarehouseUseCase.search(
                "TILBURG-001",
                100,
                200,
                2,
                25,
                "location",
                "desc"
        )).thenReturn(new ArrayList<>());

        // Execute
        warehouseResourceImpl.searchWarehouses(
                "TILBURG-001",
                BigInteger.valueOf(100),
                BigInteger.valueOf(200),
                "location",
                "desc",
                BigInteger.valueOf(2),
                BigInteger.valueOf(25)
        );

        // Assert - Verify parameter translation happened correctly
        verify(searchWarehouseUseCase, times(1)).search(
                "TILBURG-001",
                100,
                200,
                2,
                25,
                "location",
                "desc"
        );
    }

    @Test
    void testMultipleSearchCallsWithDifferentParameters() {
        // Setup
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> mockResults = new ArrayList<>();
        mockResults.add(testDomainWarehouse);

        when(searchWarehouseUseCase.search(
                anyString(),
                any(),
                any(),
                anyInt(),
                anyInt(),
                nullable(String.class),
                nullable(String.class)
        )).thenReturn(mockResults);

        // Execute - Multiple API calls
        List<com.warehouse.api.beans.Warehouse> result1 = warehouseResourceImpl.searchWarehouses(
                "AMSTERDAM-001",
                BigInteger.valueOf(50),
                BigInteger.valueOf(150),
                "createdAt",
                "asc",
                BigInteger.ZERO,
                BigInteger.TEN
        );

        List<com.warehouse.api.beans.Warehouse> result2 = warehouseResourceImpl.searchWarehouses(
                "ZWOLLE-001",
                null,
                null,
                null,
                null,
                BigInteger.ONE,
                BigInteger.valueOf(20)
        );

        // Assert
        assertNotNull(result1, "First search result should not be null");
        assertNotNull(result2, "Second search result should not be null");
        verify(searchWarehouseUseCase, times(2)).search(
                anyString(),
                any(),
                any(),
                anyInt(),
                anyInt(),
                nullable(String.class),
                nullable(String.class)
        );
    }

    // ============ Get Warehouse Tests ============

    @Test
    void testGetWarehouseByIdSuccess() {
        // Setup
        when(getWarehouseUseCase.findByBusinessUnitCode("TEST-WH-001"))
                .thenReturn(testDomainWarehouse);

        // Execute
        com.warehouse.api.beans.Warehouse result = warehouseResourceImpl.getAWarehouseUnitByID("TEST-WH-001");

        // Assert
        assertNotNull(result, "Result should not be null");
        verify(getWarehouseUseCase, times(1)).findByBusinessUnitCode("TEST-WH-001");
    }

    @Test
    void testGetWarehouseByIdNotFound() {
        // Setup
        when(getWarehouseUseCase.findByBusinessUnitCode("UNKNOWN"))
                .thenReturn(null);

        // Execute & Assert
        assertThrows(
                Exception.class,
                () -> warehouseResourceImpl.getAWarehouseUnitByID("UNKNOWN"),
                "Should throw exception for warehouse not found"
        );
    }

    // ============ REST API Adapter Coverage Tests ============

    @Test
    void testCreateWarehouseCallsOperationWithCorrectParameters() {
        // Setup
        com.warehouse.api.beans.Warehouse createRequest = new com.warehouse.api.beans.Warehouse();
        createRequest.setBusinessUnitCode("NEW-WH-001");
        createRequest.setLocation("AMSTERDAM-001");
        createRequest.setCapacity(100);

        when(getWarehouseUseCase.findByBusinessUnitCode("NEW-WH-001"))
                .thenReturn(testDomainWarehouse);
        doNothing().when(createWarehouseOperation).create(anyString(), anyString(), anyInt());

        // Execute
        com.warehouse.api.beans.Warehouse result = warehouseResourceImpl.createANewWarehouseUnit(createRequest);

        // Assert
        assertNotNull(result, "Create result should not be null");
        verify(createWarehouseOperation, times(1)).create(anyString(), anyString(), anyInt());
    }

    @Test
    void testArchiveWarehouseCallsOperation() {
        // Setup
        doNothing().when(archiveWarehouseOperation).archive("TEST-WH-001");

        // Execute
        warehouseResourceImpl.archiveAWarehouseUnitByID("TEST-WH-001");

        // Assert
        verify(archiveWarehouseOperation, times(1)).archive("TEST-WH-001");
    }

    @Test
    void testReplaceWarehouseCallsOperation() {
        // Setup
        com.warehouse.api.beans.Warehouse updateRequest = new com.warehouse.api.beans.Warehouse();
        updateRequest.setLocation("ZWOLLE-001");
        updateRequest.setCapacity(150);
        updateRequest.setStock(50);

        com.fulfilment.application.monolith.warehouses.domain.models.Warehouse updatedWarehouse = 
            com.fulfilment.application.monolith.warehouses.domain.models.Warehouse.reconstruct(
                "TEST-WH-001",
                "ZWOLLE-001",
                150,
                50,
                LocalDateTime.now(),
                null
        );

        doNothing().when(replaceWarehouseOperation).replace("TEST-WH-001", "ZWOLLE-001", 150, 50);
        when(getWarehouseUseCase.findByBusinessUnitCode("TEST-WH-001"))
                .thenReturn(updatedWarehouse);

        // Execute
        com.warehouse.api.beans.Warehouse result = warehouseResourceImpl.replaceTheCurrentActiveWarehouse("TEST-WH-001", updateRequest);

        // Assert
        assertNotNull(result, "Replace result should not be null");
        verify(replaceWarehouseOperation, times(1)).replace(anyString(), anyString(), anyInt(), anyInt());
    }

    // ============ Error Handling Tests ============

    @Test
    void testSearchWithInvalidPageThrowsException() {
        // Setup
        when(searchWarehouseUseCase.search(
                any(),
                any(),
                any(),
                eq(-1),
                anyInt(),
                any(),
                any()
        )).thenThrow(new IllegalArgumentException("Invalid page"));

        // Execute & Assert
        assertThrows(
                Exception.class,
                () -> warehouseResourceImpl.searchWarehouses(
                        "AMSTERDAM-001",
                        BigInteger.ZERO,
                        BigInteger.valueOf(1000),
                        "createdAt",
                        "asc",
                        BigInteger.valueOf(-1),
                        BigInteger.TEN
                ),
                "Negative page should throw exception"
        );
    }

    @Test
    void testCreateWithInvalidDataThrowsException() {
        // Setup
        com.warehouse.api.beans.Warehouse createRequest = new com.warehouse.api.beans.Warehouse();
        createRequest.setBusinessUnitCode("BAD");

        doThrow(new IllegalArgumentException("Invalid warehouse data"))
                .when(createWarehouseOperation).create(anyString(), anyString(), anyInt());

        // Execute & Assert
        assertThrows(
                Exception.class,
                () -> warehouseResourceImpl.createANewWarehouseUnit(createRequest),
                "Create with invalid data should throw exception"
        );
    }

    @Test
    void testArchiveNonExistentWarehouseThrowsException() {
        // Setup
        doThrow(new IllegalArgumentException("Warehouse not found"))
                .when(archiveWarehouseOperation).archive("UNKNOWN");

        // Execute & Assert
        assertThrows(
                Exception.class,
                () -> warehouseResourceImpl.archiveAWarehouseUnitByID("UNKNOWN"),
                "Archive non-existent warehouse should throw exception"
        );
    }
}
