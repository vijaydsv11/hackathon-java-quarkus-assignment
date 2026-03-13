package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Use case for replacing warehouse attributes.
 * Handles updates to warehouse location, capacity, and stock levels.
 * 
 * Logger: Logs info for successful replacement, warns for validation failures,
 * and logs errors for database-related issues or archived warehouse violations.
 */
@ApplicationScoped
@Transactional
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

    private static final Logger LOGGER =
            Logger.getLogger(ReplaceWarehouseUseCase.class);

    private final WarehouseStore warehouseStore;
    private final LocationResolver locationResolver;

    public ReplaceWarehouseUseCase(WarehouseStore warehouseStore,
                                   LocationResolver locationResolver) {
        this.warehouseStore = warehouseStore;
        this.locationResolver = locationResolver;
    }

    public void replace(Warehouse warehouse) {
        replace(
                warehouse.businessUnitCode,
                warehouse.location,
                warehouse.capacity,
                warehouse.stock
        );
    }

    @Override
    public void replace(String businessUnitCode,
                        String newLocation,
                        int capacity,
                        int stock) {

        LOGGER.infof("Replacing warehouse: %s", businessUnitCode);

        if (businessUnitCode == null || businessUnitCode.isBlank()) {
            throw new IllegalArgumentException("Business unit code is required");
        }
        
        Warehouse existing =
                warehouseStore.findByBusinessUnitCode(businessUnitCode);

        if (existing == null) {
            throw new IllegalArgumentException(
                    "Warehouse with business unit code '" +
                            businessUnitCode + "' does not exist");
        }

        if (existing.archivedAt != null) {
            throw new IllegalArgumentException("Warehouse is archived");
        }

        Location location =
                locationResolver.resolveByIdentifier(newLocation);

        if (location == null) {
            throw new IllegalArgumentException("Location is not valid");
        }

        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }

        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }

        if (capacity > location.maxCapacity()) {
            throw new IllegalArgumentException("Capacity exceeds location max capacity");
        }

        if (stock > capacity) {
            throw new IllegalArgumentException("Stock exceeds warehouse capacity");
        }

        existing.location = newLocation;
        existing.capacity = capacity;
        existing.stock = stock;

        warehouseStore.update(existing);

        LOGGER.infof("Warehouse successfully replaced: %s", businessUnitCode);
    }
}