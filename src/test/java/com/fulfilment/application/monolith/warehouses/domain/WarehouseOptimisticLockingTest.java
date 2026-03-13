package com.fulfilment.application.monolith.warehouses.domain;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WarehouseOptimisticLockingTest {

    private static final String LOCATION = "ZWOLLE-001";
    private static final String BUSINESS_UNIT = "OPT-LOCK-001";

    private static final int INITIAL_CAPACITY = 100;
    private static final int INITIAL_STOCK = 50;

    private static final int CONCURRENT_STOCK = 80;
    private static final int STALE_STOCK = 99;
    private static final int UPDATED_STOCK = 60;

    @Inject
    WarehouseRepository warehouseRepository;

    @Inject
    EntityManager entityManager;

    @Inject
    TestTransactionHelper transactionHelper;

    private Long warehouseId;

    @BeforeEach
    @Transactional
    void setup() {

        entityManager.createQuery("DELETE FROM DbWarehouse").executeUpdate();

        DbWarehouse warehouse = new DbWarehouse();
        warehouse.businessUnitCode = BUSINESS_UNIT;
        warehouse.location = LOCATION;
        warehouse.capacity = INITIAL_CAPACITY;
        warehouse.stock = INITIAL_STOCK;
        warehouse.createdAt = LocalDateTime.now();

        entityManager.persist(warehouse);
        entityManager.flush();

        warehouseId = warehouse.id;
        entityManager.clear();
    }

    @Test
    @DisplayName("Should throw OptimisticLockException when updating stale entity")
    @Transactional
    void shouldPreventLostUpdatesWhenVersionIsStale() {

        DbWarehouse staleWarehouse = getWarehouse();
        Long initialVersion = staleWarehouse.version;

        // simulate concurrent update
        transactionHelper.updateStockInNewTransaction(warehouseId, CONCURRENT_STOCK);

        staleWarehouse.stock = STALE_STOCK;

        assertThrows(
                OptimisticLockException.class,
                () -> updateWarehouse(staleWarehouse),
                "Stale update must throw OptimisticLockException"
        );

        verifyConcurrentUpdate(initialVersion);
    }

    @Test
    @DisplayName("Should increment version on successful update")
    @Transactional
    void shouldIncrementVersionOnSuccessfulUpdate() {

        DbWarehouse warehouse = getWarehouse();
        Long initialVersion = warehouse.version;

        warehouse.stock = UPDATED_STOCK;
        updateWarehouse(warehouse);

        assertEquals(initialVersion + 1, warehouse.version);
    }

    private void updateWarehouse(DbWarehouse warehouse) {
        entityManager.merge(warehouse);
        entityManager.flush();
    }

    private void verifyConcurrentUpdate(Long initialVersion) {

        entityManager.clear();

        DbWarehouse currentWarehouse = getWarehouse();

        assertEquals(CONCURRENT_STOCK, currentWarehouse.stock);
        assertEquals(initialVersion + 1, currentWarehouse.version);
    }

    private DbWarehouse getWarehouse() {
        DbWarehouse warehouse = entityManager.find(DbWarehouse.class, warehouseId);
        assertNotNull(warehouse);
        return warehouse;
    }
}

