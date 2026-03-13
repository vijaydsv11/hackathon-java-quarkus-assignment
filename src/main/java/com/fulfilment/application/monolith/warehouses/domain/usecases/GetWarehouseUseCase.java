package com.fulfilment.application.monolith.warehouses.domain.usecases;

import java.util.List;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Use case for retrieving warehouse information.
 * Provides methods to fetch all warehouses and retrieve warehouses by their business unit code.
 * 
 * Logger: Logs debug info when fetching warehouses, and errors when database access fails.
 */
@ApplicationScoped
public class GetWarehouseUseCase {

    private static final Logger LOGGER =
            Logger.getLogger(GetWarehouseUseCase.class);

    @Inject
    WarehouseStore warehouseStore;   // depends on PORT, not adapter

    /**
     * Retrieves all non-archived warehouses from the system.
     * 
     * @return List of all available warehouses
     * @throws RuntimeException if database access fails
     */
    public List<Warehouse> findAll() {
        LOGGER.debug("Fetching all warehouses");
        try {
            List<Warehouse> warehouses = warehouseStore.getAll();
            LOGGER.infof("Retrieved %d warehouses", warehouses.size());
            return warehouses;
        } catch (Exception e) {
            LOGGER.errorf(e, "Error fetching all warehouses: %s", e.getMessage());
            throw new RuntimeException("Failed to fetch warehouses", e);
        }
    }

    /**
     * Retrieves a warehouse by its unique business unit code.
     * 
     * @param code the business unit code to search for
     * @return the Warehouse if found, null otherwise
     * @throws IllegalArgumentException if code is null or blank
     * @throws RuntimeException if database access fails
     */
    @Transactional
    public Warehouse findByBusinessUnitCode(String code) {
        if (code == null || code.isBlank()) {
            LOGGER.warn("Attempted to find warehouse with null or blank business unit code");
            throw new IllegalArgumentException("Business unit code cannot be null or blank");
        }
        
        LOGGER.debugf("Finding warehouse with business unit code: %s", code);
        try {
            Warehouse warehouse = warehouseStore.findByBusinessUnitCode(code);
            if (warehouse == null) {
                LOGGER.debugf("Warehouse not found with business unit code: %s", code);
            } else {
                LOGGER.infof("Warehouse found with business unit code: %s", code);
            }
            return warehouse;
        } catch (Exception e) {
            LOGGER.errorf(e, "Error finding warehouse with code %s: %s", code, e.getMessage());
            throw new RuntimeException("Failed to find warehouse by business unit code", e);
        }
    }
}