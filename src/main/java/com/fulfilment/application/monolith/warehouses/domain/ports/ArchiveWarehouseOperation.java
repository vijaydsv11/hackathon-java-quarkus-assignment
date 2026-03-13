package com.fulfilment.application.monolith.warehouses.domain.ports;

/**
 * Port interface for warehouse archival operations.
 * 
 * Defines the contract for archiving warehouses to mark them as inactive.
 * Logger: Implementing classes log info and error levels for archival operations.
 */
public interface ArchiveWarehouseOperation {
    
    /**
     * Archives a warehouse by marking it as inactive.
     * 
     * @param businessUnitCode the unique identifier of the warehouse to archive
     * @throws IllegalArgumentException if warehouse not found
     * @throws IllegalArgumentException if warehouse is already archived
     * @throws IllegalArgumentException if businessUnitCode is null or blank
     */
    void archive(String businessUnitCode);
}
