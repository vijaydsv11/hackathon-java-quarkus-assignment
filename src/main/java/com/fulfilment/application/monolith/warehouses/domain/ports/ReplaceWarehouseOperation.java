package com.fulfilment.application.monolith.warehouses.domain.ports;

/**
 * Port interface for warehouse replacement operations.
 * 
 * Defines the contract for replacing warehouse attributes such as location,
 * capacity, and stock levels.
 * Logger: Implementing classes log info and error levels for replacement operations.
 */
public interface ReplaceWarehouseOperation {
    
    /**
     * Replaces warehouse attributes with new values.
     * 
     * @param businessUnitCode the warehouse identifier
     * @param newLocation the new location for the warehouse
     * @param capacity the new total capacity
     * @param stock the new current stock level
     * @throws IllegalArgumentException if parameters are invalid or warehouse not found
     * @throws IllegalArgumentException if warehouse is archived
     */
    public void replace(String businessUnitCode,
            String newLocation,
            int capacity,
            int stock);
}
