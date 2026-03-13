package com.fulfilment.application.monolith.warehouses.adapters;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@QuarkusTest
class WarehouseTestcontainersIT {

    private static final String AMSTERDAM = "AMSTERDAM-001";
    private static final String ZWOLLE = "ZWOLLE-001";
    private static final String TILBURG = "TILBURG-001";
    private static final String ROLLBACK_CODE = "ROLLBACK-TEST-001";

    @Inject
    WarehouseRepository warehouseRepository;

    @Inject
    LocationResolver locationResolver;

    @Inject
    CreateWarehouseUseCase createWarehouseUseCase;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    @Transactional
    void setup() {
        clearDatabase();
    }

    @Test
    @Transactional
    void shouldEnforceUniqueBusinessUnitCodeConstraint() {

        createWarehouse("DB-UNIQUE-001", AMSTERDAM, 50, 10);

        DbWarehouse duplicate =
                buildDbWarehouse("DB-UNIQUE-001", ZWOLLE, 30, 5);

        assertThrows(Exception.class, () -> {
            entityManager.persist(duplicate);
            entityManager.flush();
        }, "Database should reject duplicate businessUnitCode");
    }

    @Test
    @Transactional
    void shouldQueryMultipleWarehousesAtSameLocation() {

        for (int i = 0; i < 5; i++) {
            createWarehouse("QUERY-TEST-" + i, AMSTERDAM, 20 + (i * 10), 5 + i);
        }

        List<Warehouse> warehouses = warehouseRepository.getAll();

        long amsterdamCount = warehouses.stream()
                .filter(w -> AMSTERDAM.equals(w.location))
                .count();

        assertEquals(5, amsterdamCount,
                "Should find exactly 5 warehouses in Amsterdam");
    }

    @Test
    @Transactional
    void shouldHandleNullArchivedAtField() {

        DbWarehouse dbWarehouse =
                buildDbWarehouse("NULL-TEST-001", ZWOLLE, 50, 10);

        entityManager.persist(dbWarehouse);
        entityManager.flush();

        DbWarehouse found =
                entityManager.find(DbWarehouse.class, dbWarehouse.id);

        assertNotNull(found);
        assertNull(found.archivedAt, "archivedAt should be null");
    }

    @Test
    void shouldRollbackTransactionOnFailure() {

        assertThrows(RuntimeException.class, this::performFailingTransaction);

        Warehouse warehouse =
                warehouseRepository.findByBusinessUnitCode(ROLLBACK_CODE);

        assertNull(warehouse,
                "Warehouse should not exist because transaction rolled back");
    }

    @Test
    @Transactional
    void shouldFindWarehousesByLocationAndCapacityRange() {

        createWarehouse("COMPLEX-1", AMSTERDAM, 30, 10);
        createWarehouse("COMPLEX-2", AMSTERDAM, 50, 10);
        createWarehouse("COMPLEX-3", AMSTERDAM, 70, 10);
        createWarehouse("COMPLEX-4", ZWOLLE, 40, 10);

        List<DbWarehouse> results = entityManager.createQuery(
                """
                SELECT w FROM DbWarehouse w
                WHERE w.location = :location
                AND w.capacity BETWEEN :min AND :max
                """,
                DbWarehouse.class)
                .setParameter("location", AMSTERDAM)
                .setParameter("min", 40)
                .setParameter("max", 70)
                .getResultList();

        assertEquals(2, results.size(),
                "Should return warehouses with capacity between 40 and 70");
    }

    @Transactional
    void performFailingTransaction() {

        createWarehouse(ROLLBACK_CODE, TILBURG, 30, 10);

        throw new RuntimeException("Simulated failure");
    }

    private void createWarehouse(String code, String location, int capacity, int stock) {

        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = code;
        warehouse.location = location;
        warehouse.capacity = capacity;
        warehouse.stock = stock;
        warehouse.createdAt = LocalDateTime.now();

        createWarehouseUseCase.create(warehouse);
    }

    private DbWarehouse buildDbWarehouse(String code, String location, int capacity, int stock) {

        DbWarehouse warehouse = new DbWarehouse();
        warehouse.businessUnitCode = code;
        warehouse.location = location;
        warehouse.capacity = capacity;
        warehouse.stock = stock;
        warehouse.createdAt = LocalDateTime.now();
        warehouse.archivedAt = null;

        return warehouse;
    }

    private void clearDatabase() {
        entityManager.createQuery("DELETE FROM DbWarehouse").executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }
}
