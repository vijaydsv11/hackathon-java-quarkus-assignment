package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

/**
 * Port interface for warehouse persistence operations.
 * 
 * This interface defines the contract for warehouse storage operations,
 * allowing the domain layer to remain independent of persistence implementation.
 * Implementations may use various storage backends (SQL, NoSQL, etc.).
 */
public interface WarehouseStore {

    /**
     * Retrieves all warehouses from storage.
     * 
     * @return List of all warehouses
     */
    List<Warehouse> getAll();

    /**
     * Creates a new warehouse in storage.
     * 
     * @param warehouse the warehouse to create
     * @throws IllegalArgumentException if warehouse already exists or violates constraints
     */
    void create(Warehouse warehouse);

    /**
     * Updates an existing warehouse in storage.
     * 
     * @param warehouse the warehouse to update
     * @throws IllegalStateException if warehouse not found
     */
    void update(Warehouse warehouse);

    /**
     * Removes a warehouse from storage.
     * 
     * @param warehouse the warehouse to remove
     */
    void remove(Warehouse warehouse);

    /**
     * Finds a warehouse by its business unit code.
     * 
     * @param buCode the business unit code
     * @return the Warehouse if found, null otherwise
     */
    Warehouse findByBusinessUnitCode(String buCode);

    /**
     * Checks if a warehouse exists by business unit code.
     * 
     * @param buCode the business unit code
     * @return true if warehouse exists, false otherwise
     */
    boolean existsByBusinessUnitCode(String buCode);  
    
    /**
     * Searches warehouses by specified criteria.
     * 
     * @param location the location to filter by (nullable)
     * @param minCapacity the minimum capacity (nullable)
     * @param maxCapacity the maximum capacity (nullable)
     * @param page the page number (0-indexed)
     * @param pageSize the number of results per page
     * @param sortBy the field to sort by
     * @param sortOrder the sort order ("asc" or "desc")
     * @return List of warehouses matching criteria
     * @throws IllegalArgumentException if parameters are invalid
     */
    public List<Warehouse> search(
            String location,
            Integer minCapacity,
            Integer maxCapacity,
            int page,
            int pageSize,
            String sortBy,
            String sortOrder);
}