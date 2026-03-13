package com.fulfilment.application.monolith.warehouses.domain.ports;

/**
 * Port interface for warehouse creation operations.
 * 
 * Defines the contract for creating new warehouses with specified parameters.
 * Logger: Implementing classes log info and error levels for creation operations.
 */
public interface CreateWarehouseOperation {
    
    /**
     * Creates a new warehouse with the specified parameters.
     * 
     * @param businessUnitCode the unique business unit code for the warehouse
     * @param locationIdentifier the location where the warehouse operates
     * @param capacity the total storage capacity of the warehouse
     * @throws IllegalArgumentException if parameters are invalid or warehouse already exists
     */
    void create(String businessUnitCode,
          String locationIdentifier,
          int capacity);
}
