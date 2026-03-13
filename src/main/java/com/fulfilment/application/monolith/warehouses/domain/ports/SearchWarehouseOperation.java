package com.fulfilment.application.monolith.warehouses.domain.ports;

import java.util.List;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

/**
 * Port interface for warehouse search operations.
 * 
 * Defines the contract for searching warehouses with advanced filtering,
 * pagination, and sorting capabilities.
 * Logger: Implementing classes log debug info and search operation results.
 */
public interface SearchWarehouseOperation {

	/**
	 * Searches for warehouses matching the specified criteria.
	 * 
	 * @param location the location identifier to filter by (nullable)
	 * @param minCapacity the minimum warehouse capacity (nullable)
	 * @param maxCapacity the maximum warehouse capacity (nullable)
	 * @param page the page number (0-indexed)
	 * @param pageSize the number of results per page (1-100)
	 * @param sortBy the field name to sort results by
	 * @param sortOrder the sort direction (\"asc\" or \"desc\")
	 * @return List of warehouses matching the search criteria
	 * @throws IllegalArgumentException if page or pageSize are invalid
	 */
	
	List<Warehouse> search(
            String location,
            Integer minCapacity,
            Integer maxCapacity,
            int page,
            int pageSize,
            String sortBy,
            String sortOrder);
}
