package com.fulfilment.application.monolith.warehouses.adapters;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
public class WarehouseRepositoryIT {

    @Inject
    WarehouseRepository warehouseRepository;

//    @Test
//    @Transactional
//    void shouldGetAllWarehouses() {
//
//        Warehouse warehouse = new Warehouse();
//        warehouse.businessUnitCode = "TEST-WH-004";
//        warehouse.location = "AMSTERDAM-001";
//        warehouse.capacity = 50;
//        warehouse.stock = 10;
//        warehouse.createdAt = LocalDateTime.now();
//
//        warehouseRepository.create(warehouse);
//
//        var list = warehouseRepository.getAll();
//
//        assertFalse(list.isEmpty());
//    }
    
    @Test
    @Transactional
    void shouldCreateAndFindWarehouse() {

        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "TEST-WH-001";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.stock = 50;
        warehouse.createdAt = LocalDateTime.now();

        warehouseRepository.create(warehouse);

        Warehouse result =
                warehouseRepository.findByBusinessUnitCode("TEST-WH-001");

        assertNotNull(result);
        assertEquals("AMSTERDAM-001", result.location);
    }
    
    @Test
    @Transactional
    void shouldUpdateWarehouse() {

        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "TEST-WH-002";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.stock = 50;
        warehouse.createdAt = LocalDateTime.now();

        warehouseRepository.create(warehouse);

        warehouse.stock = 80;

        warehouseRepository.update(warehouse);

        Warehouse updated =
            warehouseRepository.findByBusinessUnitCode("TEST-WH-002");

        assertEquals(80, updated.stock);
    }
    
    @Test
    @Transactional
    void shouldCheckWarehouseExists() {

        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "TEST-WH-003";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.stock = 20;
        warehouse.createdAt = LocalDateTime.now();

        warehouseRepository.create(warehouse);

        boolean exists =
                warehouseRepository.existsByBusinessUnitCode("TEST-WH-003");

        assertEquals(true, exists);
    }
}