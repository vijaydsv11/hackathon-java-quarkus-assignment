package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Use case for archiving warehouses.
 * Marks a warehouse as inactive by setting the archive timestamp.
 * 
 * Logger: Logs info for successful archival, warns for validation failures,
 * and logs errors for database-related issues or invalid warehouse states.
 */
@ApplicationScoped
@Transactional
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

    private static final Logger LOGGER =
            Logger.getLogger(ArchiveWarehouseUseCase.class);

    private final WarehouseStore warehouseStore;

    public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
        this.warehouseStore = warehouseStore;
    }
    
    @Override
    public void archive(String businessUnitCode) {

    	LOGGER.infof("Archiving warehouse with BU code: %s", businessUnitCode);
    	
    	if (businessUnitCode == null || businessUnitCode.isBlank()) {
    	    LOGGER.warn("Attempt to archive warehouse with null/blank business unit code");
    	    throw new IllegalArgumentException("Business unit code is required");
    	}

        Warehouse existing =
                warehouseStore.findByBusinessUnitCode(businessUnitCode);

        if (existing == null) {
            LOGGER.warnf("Warehouse not found for archival: %s", businessUnitCode);
            throw new IllegalArgumentException(
                    "Warehouse with business unit code '" +
                            businessUnitCode + "' does not exist");
        }

        if (existing.archivedAt != null) {
            LOGGER.warnf("Warehouse already archived: %s", businessUnitCode);
            throw new IllegalArgumentException("Warehouse already archived");
        }

        // Delegate business rule to aggregate
        existing.archive();

        try {
            warehouseStore.update(existing);
            LOGGER.infof("Warehouse successfully archived: %s", businessUnitCode);
        } catch (Exception e) {
            LOGGER.errorf(e, "Error archiving warehouse %s: %s", businessUnitCode, e.getMessage());
            throw new RuntimeException("Failed to archive warehouse", e);
        }
    }
}