package com.fulfilment.application.monolith.warehouses.domain.models;

/**
 * Domain value object representing a warehouse location.
 * 
 * Encapsulates location-specific constraints such as maximum number of warehouses
 * and maximum capacity allowed at that location.
 * 
 * @param identifier the unique location identifier
 * @param maxNumberOfWarehouses the maximum number of warehouses allowed at this location
 * @param maxCapacity the maximum total capacity allowed at this location
 */
public record Location(String identifier, int maxNumberOfWarehouses, int maxCapacity) {}
