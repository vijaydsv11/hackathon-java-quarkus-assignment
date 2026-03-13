package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended test suite for CreateWarehouseUseCase
 * Covers validation edge cases and successful creation scenarios
 */
@QuarkusTest
public class CreateWarehouseUseCaseExtendedTest {

    @Inject
    WarehouseRepository warehouseRepository;

    @Inject
    LocationResolver locationResolver;

    @Inject
    EntityManager em;

    private CreateWarehouseUseCase createWarehouseUseCase;

    @BeforeEach
    @Transactional
    public void setup() {
        em.createQuery("DELETE FROM DbWarehouse").executeUpdate();
        createWarehouseUseCase = new CreateWarehouseUseCase(warehouseRepository, locationResolver);
    }

    // ============ Successful Creation Tests ============

    @Test
    @Transactional
    public void testCreateWarehouseWithMinCapacity() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-MIN-CAP-001";
        warehouse.location = "ZWOLLE-001";  // max capacity 40
        warehouse.capacity = 1;  // Minimum positive capacity
        warehouse.stock = 0;

        createWarehouseUseCase.create(warehouse);

        Warehouse created = warehouseRepository.findByBusinessUnitCode("CREATE-MIN-CAP-001");
        assertNotNull(created);
        assertEquals(1, created.capacity);
        assertEquals("ZWOLLE-001", created.location);
    }

    @Test
    @Transactional
    public void testCreateWarehouseWithMaxCapacity() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-MAX-CAP-001";
        warehouse.location = "AMSTERDAM-001";  // max capacity 100
        warehouse.capacity = 100;  // Max capacity for this location
        warehouse.stock = 0;

        createWarehouseUseCase.create(warehouse);

        Warehouse created = warehouseRepository.findByBusinessUnitCode("CREATE-MAX-CAP-001");
        assertNotNull(created);
        assertEquals(100, created.capacity);
    }

    @Test
    @Transactional
    public void testCreateWarehouseWithInitialStock() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-STOCK-001";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.stock = 50;

        createWarehouseUseCase.create(warehouse);

        Warehouse created = warehouseRepository.findByBusinessUnitCode("CREATE-STOCK-001");
        assertNotNull(created);
        assertEquals(50, created.stock);
    }

    @Test
    @Transactional
    public void testCreateMultipleWarehousesAtSameLocation() {
        for (int i = 1; i <= 3; i++) {
            Warehouse warehouse = new Warehouse();
            warehouse.businessUnitCode = "CREATE-MULTI-" + i;
            warehouse.location = "AMSTERDAM-001";
            warehouse.capacity = 50 + i;
            warehouse.stock = 10 + i;

            createWarehouseUseCase.create(warehouse);
        }

        assertTrue(warehouseRepository.existsByBusinessUnitCode("CREATE-MULTI-1"));
        assertTrue(warehouseRepository.existsByBusinessUnitCode("CREATE-MULTI-2"));
        assertTrue(warehouseRepository.existsByBusinessUnitCode("CREATE-MULTI-3"));
    }

    // ============ Location Validation Tests ============

    @Test
    @Transactional
    public void testCreateThrowsWhenLocationIsUnknown() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-UNKNOWN-LOC-001";
        warehouse.location = "UNKNOWN-LOCATION";
        warehouse.capacity = 50;
        warehouse.stock = 0;

        assertThrows(IllegalArgumentException.class, () ->
                createWarehouseUseCase.create(warehouse)
        );
    }

    @Test
    @Transactional
    public void testCreateWithAllValidLocations() {
        String[] locations = {"ZWOLLE-001", "ZWOLLE-002", "AMSTERDAM-001", "AMSTERDAM-002", 
                              "TILBURG-001", "HELMOND-001", "EINDHOVEN-001", "VETSBY-001"};
        
        for (int i = 0; i < locations.length; i++) {
            Warehouse warehouse = new Warehouse();
            warehouse.businessUnitCode = "CREATE-LOC-" + i;
            warehouse.location = locations[i];
            warehouse.capacity = 30;
            warehouse.stock = 10;

            createWarehouseUseCase.create(warehouse);
        }

        for (int i = 0; i < locations.length; i++) {
            assertTrue(warehouseRepository.existsByBusinessUnitCode("CREATE-LOC-" + i));
        }
    }

    // ============ Capacity vs Location Max Capacity Tests ============

    @Test
    @Transactional
    public void testCreateThrowsWhenCapacityExceedsLocationMaxByOne() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-EXCEED-BY-ONE";
        warehouse.location = "ZWOLLE-001";  // max capacity 40
        warehouse.capacity = 41;  // One more than max

        assertThrows(IllegalArgumentException.class, () ->
                createWarehouseUseCase.create(warehouse)
        );
    }

    @Test
    @Transactional
    public void testCreateSucceedsAtLocationMaxCapacity() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-AT-MAX";
        warehouse.location = "TILBURG-001";  // max capacity 40
        warehouse.capacity = 40;  // Exactly at max

        createWarehouseUseCase.create(warehouse);

        Warehouse created = warehouseRepository.findByBusinessUnitCode("CREATE-AT-MAX");
        assertNotNull(created);
        assertEquals(40, created.capacity);
    }

    // ============ Stock vs Capacity Tests ============

    @Test
    @Transactional
    public void testCreateThrowsWhenStockExceedsCapacity() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-STOCK-EXCEED";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 50;
        warehouse.stock = 60;  // More than capacity

        assertThrows(IllegalArgumentException.class, () ->
                createWarehouseUseCase.create(warehouse)
        );
    }

    @Test
    @Transactional
    public void testCreateSucceedsWithMaxStock() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-MAX-STOCK";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.stock = 100;  // Stock equals capacity

        createWarehouseUseCase.create(warehouse);

        Warehouse created = warehouseRepository.findByBusinessUnitCode("CREATE-MAX-STOCK");
        assertNotNull(created);
        assertEquals(100, created.stock);
        assertEquals(100, created.capacity);
    }

    // ============ Business Unit Code Validation Tests ============

    @Test
    @Transactional
    public void testCreateSucceedsWithDifferentCodeFormats() {
        String[] codes = {"CODE-001", "BU_TEST", "WAREHOUSE_1", "UNITTEST"};

        for (String code : codes) {
            Warehouse warehouse = new Warehouse();
            warehouse.businessUnitCode = code;
            warehouse.location = "AMSTERDAM-001";
            warehouse.capacity = 50;
            warehouse.stock = 10;

            createWarehouseUseCase.create(warehouse);
            assertTrue(warehouseRepository.existsByBusinessUnitCode(code));
        }
    }

    @Test
    @Transactional
    public void testCreateThrowsWhenDuplicateBusinessUnitCode() {
        // Create first warehouse
        Warehouse warehouse1 = new Warehouse();
        warehouse1.businessUnitCode = "CREATE-DUPLICATE";
        warehouse1.location = "AMSTERDAM-001";
        warehouse1.capacity = 50;
        warehouse1.stock = 10;
        createWarehouseUseCase.create(warehouse1);

        // Try to create second with same code
        Warehouse warehouse2 = new Warehouse();
        warehouse2.businessUnitCode = "CREATE-DUPLICATE";
        warehouse2.location = "ZWOLLE-001";
        warehouse2.capacity = 30;
        warehouse2.stock = 5;

        assertThrows(IllegalArgumentException.class, () ->
                createWarehouseUseCase.create(warehouse2)
        );
    }

    // ============ Edge Case Tests ============

    @Test
    @Transactional
    public void testCreateWithZeroStock() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-ZERO-STOCK";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 50;
        warehouse.stock = 0;  // Zero stock is valid

        createWarehouseUseCase.create(warehouse);

        Warehouse created = warehouseRepository.findByBusinessUnitCode("CREATE-ZERO-STOCK");
        assertNotNull(created);
        assertEquals(0, created.stock);
    }

    @Test
    @Transactional
    public void testCreateSetsCreatedAtTimestamp() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-TIMESTAMP";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 50;
        warehouse.stock = 10;

        createWarehouseUseCase.create(warehouse);

        Warehouse created = warehouseRepository.findByBusinessUnitCode("CREATE-TIMESTAMP");
        assertNotNull(created.createdAt);
    }

    @Test
    @Transactional
    public void testCreateSetsArchivedAtToNull() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "CREATE-NOT-ARCHIVED";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 50;
        warehouse.stock = 10;

        createWarehouseUseCase.create(warehouse);

        Warehouse created = warehouseRepository.findByBusinessUnitCode("CREATE-NOT-ARCHIVED");
        assertNull(created.archivedAt);
    }
}
