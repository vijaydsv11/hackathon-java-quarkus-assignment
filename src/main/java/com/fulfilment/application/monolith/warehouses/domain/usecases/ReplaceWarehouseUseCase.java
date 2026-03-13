package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.Objects;

/**
 * Use case for replacing/updating warehouse attributes.
 *
 * <p>This class implements the {@link ReplaceWarehouseOperation} interface and handles
 * all business logic for updating an existing warehouse's properties. It validates:
 * <ul>
 *   <li>Warehouse existence and active state (not archived)</li>
 *   <li>Location validity through the location resolver</li>
 *   <li>Capacity constraints (non-negative, within location max capacity)</li>
 *   <li>Stock level constraints (non-negative, within warehouse capacity)</li>
 * </ul>
 *
 * <p>All operations are transactional to ensure data consistency. The use case applies
 * optimistic locking through the warehouse store to detect concurrent modifications.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Validate all input parameters before applying updates</li>
 *   <li>Enforce business rules (capacity > stock, capacity ≤ location max)</li>
 *   <li>Persist warehouse updates through the store</li>
 *   <li>Provide detailed logging for auditing and debugging</li>
 * </ul>
 *
 * @see ReplaceWarehouseOperation
 * @see WarehouseStore
 * @see LocationResolver
 * @see Warehouse
 */
@ApplicationScoped
@Transactional
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

    private static final Logger LOGGER =
            Logger.getLogger(ReplaceWarehouseUseCase.class);

    private final WarehouseStore warehouseStore;
    private final LocationResolver locationResolver;

    /**
     * Constructs a ReplaceWarehouseUseCase with required dependencies.
     *
     * @param warehouseStore the warehouse store for persistence operations (must not be null)
     * @param locationResolver the location resolver for validating locations (must not be null)
     */
    public ReplaceWarehouseUseCase(WarehouseStore warehouseStore,
                                   LocationResolver locationResolver) {
        this.warehouseStore = warehouseStore;
        this.locationResolver = locationResolver;
    }

    /**
     * Replaces an entire warehouse with new values.
     *
     * <p>Convenience method that delegates to the parameterized replace method,
     * extracting fields from the provided warehouse domain object.
     *
     * @param warehouse the warehouse object with updated values (must not be null)
     * @throws IllegalArgumentException if warehouse validation fails (see {@link #replace(String, String, int, int)})
     */
    public void replace(Warehouse warehouse) {
        replace(
                warehouse.businessUnitCode,
                warehouse.location,
                warehouse.capacity,
                warehouse.stock
        );
    }

    /**
     * Replaces warehouse attributes with detailed validation.
     *
     * <p>Updates the location, capacity, and stock of an existing warehouse. Performs comprehensive
     * validation to ensure:
     * <ul>
     *   <li>Business unit code is valid and warehouse exists</li>
     *   <li>Warehouse is active (not archived)</li>
     *   <li>New location is valid and supported</li>
     *   <li>Capacity is non-negative and within location limits</li>
     *   <li>Stock is non-negative and does not exceed capacity</li>
     * </ul>
     *
     * <p><strong>Optimistic Locking:</strong> May throw an exception if another transaction
     * concurrently modified the warehouse (version mismatch).
     *
     * @param businessUnitCode the unique identifier of the warehouse to update (must not be null or blank)
     * @param newLocation the new warehouse location identifier (must be resolvable by LocationResolver)
     * @param capacity the new warehouse capacity (must be non-negative and ≤ location max capacity)
     * @param stock the new stock level (must be non-negative and ≤ capacity)
     * @throws IllegalArgumentException if any of the following validation checks fail:
     *         <ul>
     *           <li>Business unit code is null or blank</li>
     *           <li>Warehouse with the code does not exist</li>
     *           <li>Warehouse is archived</li>
     *           <li>Location is invalid or not resolvable</li>
     *           <li>Capacity is negative</li>
     *           <li>Stock is negative</li>
     *           <li>Capacity exceeds location maximum capacity</li>
     *           <li>Stock exceeds warehouse capacity</li>
     *         </ul>
     * @throws RuntimeException if database update operation fails
     * @see #replace(Warehouse)
     */
    @Override
    public void replace(String businessUnitCode,
                        String newLocation,
                        int capacity,
                        int stock) {

        LOGGER.infof("Replacing warehouse: %s", businessUnitCode);

        if (businessUnitCode == null || businessUnitCode.isBlank()) {
            throw new IllegalArgumentException("Business unit code is required");
        }
        
        Warehouse existing =
                warehouseStore.findByBusinessUnitCode(businessUnitCode);

        if (existing == null) {
            throw new IllegalArgumentException(
                    "Warehouse with business unit code '" +
                            businessUnitCode + "' does not exist");
        }

        if (existing.archivedAt != null) {
            throw new IllegalArgumentException("Warehouse is archived");
        }

        Location location =
                locationResolver.resolveByIdentifier(newLocation);

        if (location == null) {
            throw new IllegalArgumentException("Location is not valid");
        }

        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }

        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }

        if (capacity > location.maxCapacity()) {
            throw new IllegalArgumentException("Capacity exceeds location max capacity");
        }

        if (stock > capacity) {
            throw new IllegalArgumentException("Stock exceeds warehouse capacity");
        }

        existing.location = newLocation;
        existing.capacity = capacity;
        existing.stock = stock;

        warehouseStore.update(existing);

        LOGGER.infof("Warehouse successfully replaced: %s", businessUnitCode);
    }
}