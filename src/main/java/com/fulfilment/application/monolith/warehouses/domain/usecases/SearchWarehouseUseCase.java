package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.SearchWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Use case for searching warehouses with advanced filtering and pagination.
 * Supports searching by location, capacity range, and custom sorting.
 * 
 * Logger: Logs parameter validation, search operations, and database errors.
 */
@ApplicationScoped
public class SearchWarehouseUseCase implements SearchWarehouseOperation {

    private static final Logger LOGGER =
            Logger.getLogger(SearchWarehouseUseCase.class);

    @Inject
    WarehouseStore warehouseStore;
    
    @Inject
    public SearchWarehouseUseCase(WarehouseStore warehouseStore) {
        this.warehouseStore = warehouseStore;
    }

    /**
     * Searches for warehouses based on specified criteria.
     * Validates all input parameters before executing the search.
     * 
     * @param location the location identifier to filter by (optional)
     * @param minCapacity minimum warehouse capacity (optional)
     * @param maxCapacity maximum warehouse capacity (optional)
     * @param page the page number (0-indexed)
     * @param pageSize the number of results per page (max 100)
     * @param sortBy the field to sort by (e.g., "createdAt", "capacity")
     * @param sortOrder the sort order ("asc" or "desc")
     * @return List of warehouses matching the search criteria
     * @throws IllegalArgumentException if page or pageSize are invalid
     * @throws RuntimeException if database search fails
     */
    public List<Warehouse> search(
            String location,
            Integer minCapacity,
            Integer maxCapacity,
            int page,
            int pageSize,
            String sortBy,
            String sortOrder) {

        if (page < 0) {
            LOGGER.warnf("Invalid page number: %d. Page must be >= 0", page);
            throw new IllegalArgumentException("Page number must be non-negative");
        }
        
        if (pageSize <= 0 || pageSize > 100) {
            LOGGER.warnf("Invalid page size: %d. Page size must be between 1 and 100", pageSize);
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        
        if (minCapacity != null && maxCapacity != null && minCapacity > maxCapacity) {
            LOGGER.warnf("Invalid capacity range: minCapacity(%d) > maxCapacity(%d)", minCapacity, maxCapacity);
            throw new IllegalArgumentException("Minimum capacity cannot exceed maximum capacity");
        }

        LOGGER.debugf("Searching warehouses - Location: %s, MinCap: %s, MaxCap: %s, Page: %d, PageSize: %d, SortBy: %s, SortOrder: %s",
                location, minCapacity, maxCapacity, page, pageSize, sortBy, sortOrder);
        
        try {
            List<Warehouse> results = warehouseStore.search(
                    location,
                    minCapacity,
                    maxCapacity,
                    page,
                    pageSize,
                    sortBy,
                    sortOrder
            );
            LOGGER.infof("Search completed - Found %d warehouses", results.size());
            return results;
        } catch (Exception e) {
            LOGGER.errorf(e, "Error during warehouse search: %s", e.getMessage());
            throw new RuntimeException("Failed to search warehouses", e);
        }
    }
}