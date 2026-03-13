package com.fulfilment.application.monolith.warehouses.domain;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TestTransactionHelper {

    @Inject
    EntityManager entityManager;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void updateStockInNewTransaction(Long warehouseId, int newStock) {
        DbWarehouse warehouse = findWarehouseOrThrow(warehouseId);
        warehouse.stock = newStock;
        entityManager.flush();
    }

    private DbWarehouse findWarehouseOrThrow(Long warehouseId) {

        DbWarehouse warehouse = entityManager.find(DbWarehouse.class, warehouseId);

        if (warehouse == null) {
            throw new EntityNotFoundException(
                "Warehouse not found for id: " + warehouseId
            );
        }

        return warehouse;
    }
}
