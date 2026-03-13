package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;

/**
 * Port interface for resolving location information.
 * 
 * This interface allows the domain to look up location details by identifier,
 * enabling location validation and constraint checking.
 */
public interface LocationResolver {
    
    /**
     * Resolves a location by its identifier.
     * 
     * @param identifier the location identifier
     * @return the Location if found, null if location does not exist
     * @throws IllegalArgumentException if identifier is null or blank
     */
    Location resolveByIdentifier(String identifier);
}
