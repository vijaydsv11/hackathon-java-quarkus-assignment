package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Use case for creating new warehouses.
 * Handles validation of warehouse parameters and delegates to storage.
 * 
 * Logger: Logs info for successful creation, warns for validation failures,
 * and logs errors for database-related issues.
 */
@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

    private static final Logger LOGGER =
            Logger.getLogger(CreateWarehouseUseCase.class);

    private final WarehouseStore warehouseStore;
    private final LocationResolver locationResolver;

    public CreateWarehouseUseCase(WarehouseStore warehouseStore,
                                  LocationResolver locationResolver) {
        this.warehouseStore = warehouseStore;
        this.locationResolver = locationResolver;
    }

    @Transactional
    public void create(Warehouse warehouse) {

        // Validate location
        Location location =
                locationResolver.resolveByIdentifier(warehouse.location);

        if (location == null) {
            throw new IllegalArgumentException(
                    "Location '" + warehouse.location + "' is not valid");
        }

        // Capacity validation
        if (warehouse.capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }

        if (warehouse.capacity > location.maxCapacity()) {
            throw new IllegalArgumentException(
                    "Warehouse capacity exceeds location max capacity");
        }

        if (warehouse.stock > warehouse.capacity) {
            throw new IllegalArgumentException(
                    "Warehouse stock exceeds warehouse capacity");
        }

        warehouse.createdAt = java.time.LocalDateTime.now();

        try {
            warehouseStore.create(warehouse);
        } catch (Exception e) {

            // DB uniqueness protection for concurrent inserts
            throw new IllegalArgumentException(
                    "Warehouse with business unit code '" +
                    warehouse.businessUnitCode + "' already exists", e);
        }
    }

    @Override
    @Transactional
    public void create(String businessUnitCode,
                       String locationIdentifier,
                       int capacity) {

        LOGGER.infof("Creating warehouse with BU code: %s at location: %s",
                businessUnitCode, locationIdentifier);

        if (businessUnitCode == null || businessUnitCode.isBlank()) {
            LOGGER.warn("Attempt to create warehouse with null/blank business unit code");
            throw new IllegalArgumentException("Business unit code cannot be null or blank");
        }

        if (locationIdentifier == null || locationIdentifier.isBlank()) {
            LOGGER.warnf("Attempt to create warehouse with null/blank location: %s", businessUnitCode);
            throw new IllegalArgumentException("Location identifier cannot be null or blank");
        }

        Location location =
                locationResolver.resolveByIdentifier(locationIdentifier);

        if (location == null) {
            LOGGER.warnf("Location '%s' is not valid for warehouse %s", locationIdentifier, businessUnitCode);
            throw new IllegalArgumentException(
                    "Location '" + locationIdentifier + "' is not valid");
        }

        Warehouse warehouse = Warehouse.create(
                businessUnitCode,
                locationIdentifier,
                capacity,
                location.maxCapacity()
        );

        try {
            warehouseStore.create(warehouse);
            LOGGER.infof("Warehouse successfully created: %s", businessUnitCode);
        } catch (Exception e) {
            LOGGER.errorf(e, "Error creating warehouse %s: %s", businessUnitCode, e.getMessage());
            throw new IllegalArgumentException(
                    "Warehouse with business unit code '" +
                    businessUnitCode + "' already exists", e);
        }
    }
}