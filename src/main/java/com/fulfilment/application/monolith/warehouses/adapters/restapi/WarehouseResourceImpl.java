package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.usecases.GetWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.SearchWarehouseUseCase;
import com.warehouse.api.WarehouseResource;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.validation.constraints.NotNull;

import org.jboss.logging.Logger;

import java.math.BigInteger;
import java.util.List;

/**
 * REST API adapter for warehouse operations.
 *
 * <p>This class implements the {@link WarehouseResource} interface and serves as the HTTP endpoint
 * handler for all warehouse-related REST operations. It acts as a bridge between the REST API layer
 * and the domain use cases, handling request/response conversion, error handling, and logging.
 *
 * <p>Supported operations:
 * <ul>
 *   <li><strong>Search:</strong> Query warehouses with filters (location, capacity range) and pagination</li>
 *   <li><strong>List:</strong> Retrieve all active warehouses</li>
 *   <li><strong>Create:</strong> Create a new warehouse unit</li>
 *   <li><strong>Retrieve:</strong> Get a specific warehouse by business unit code</li>
 *   <li><strong>Archive:</strong> Soft-delete a warehouse (mark as archived)</li>
 *   <li><strong>Replace:</strong> Update an existing warehouse's data</li>
 * </ul>
 *
 * <p>The class uses dependency injection to access use cases and operations. All methods are
 * request-scoped to ensure thread safety and clean resource management.
 *
 * @see WarehouseResource
 * @see GetWarehouseUseCase
 * @see SearchWarehouseUseCase
 * @see CreateWarehouseOperation
 * @see ReplaceWarehouseOperation
 * @see ArchiveWarehouseOperation
 */
@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

    private static final Logger LOGGER =
            Logger.getLogger(WarehouseResourceImpl.class);

    @Inject
    CreateWarehouseOperation createWarehouseOperation;

    @Inject
    ReplaceWarehouseOperation replaceWarehouseOperation;

    @Inject
    ArchiveWarehouseOperation archiveWarehouseOperation;

    @Inject
    GetWarehouseUseCase getWarehouseUseCase;
    
    @Inject
    SearchWarehouseUseCase searchWarehouseUseCase;

    /**
     * Searches for warehouses based on optional filters with pagination and sorting.
     *
     * <p>Allows flexible search with any combination of the following filters:
     * <ul>
     *   <li>Location: exact match filter for warehouse location</li>
     *   <li>Capacity range: minimum and/or maximum warehouse capacity</li>
     *   <li>Sorting: by field name with ascending/descending order</li>
     *   <li>Pagination: page number and page size for results</li>
     * </ul>
     *
     * @param location the warehouse location to filter by (optional, can be null)
     * @param minCapacity minimum warehouse capacity (optional, can be null)
     * @param maxCapacity maximum warehouse capacity (optional, can be null)
     * @param sortBy the field name to sort results by (e.g., "capacity", "location")
     * @param sortOrder sorting direction: "asc" for ascending or "desc" for descending
     * @param page the page number (0-indexed, defaults to 0 if null)
     * @param pageSize the number of results per page (defaults to 10 if null)
     * @return a list of warehouses matching the search criteria, converted to REST response format
     * @throws WebApplicationException if search parameters are invalid (400 Bad Request)
     */
    @Override
    public List<com.warehouse.api.beans.Warehouse> searchWarehouses(
            String location,
            BigInteger minCapacity,
            BigInteger maxCapacity,
            String sortBy,
            String sortOrder,
            BigInteger page,
            BigInteger pageSize) {

        LOGGER.info("Request received: search warehouses");

        Integer minCap = minCapacity != null ? minCapacity.intValue() : null;
        Integer maxCap = maxCapacity != null ? maxCapacity.intValue() : null;

        int pageValue = page != null ? page.intValue() : 0;
        int pageSizeValue = pageSize != null ? pageSize.intValue() : 10;

        return searchWarehouseUseCase.search(
                location,
                minCap,
                maxCap,
                pageValue,
                pageSizeValue,
                sortBy,
                sortOrder
        )
        .stream()
        .map(this::toResponse)
        .toList();
    }
    /* ============================================================
       LIST
    ============================================================ */

    /**
     * Retrieves all active (non-archived) warehouse units.
     *
     * <p>Returns a complete list of all warehouses in the system. This operation
     * does not apply any filters or pagination.
     *
     * @return a list of all active warehouses, converted to REST response format,
     *         or an empty list if no warehouses exist
     */
    @Override
    public List<com.warehouse.api.beans.Warehouse> listAllWarehousesUnits() {

        LOGGER.info("Request received: list all warehouses");

        return getWarehouseUseCase.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /* ============================================================
       CREATE
    ============================================================ */

    /**
     * Creates a new warehouse unit.
     *
     * <p>Initiates the warehouse creation process using the provided data. The newly created
     * warehouse is immediately persisted and returned with its assigned properties.
     *
     * @param data the warehouse data containing business unit code, location, and capacity
     * @return the newly created warehouse with all assigned properties, converted to REST response format
     * @throws WebApplicationException with 400 Bad Request status if:
     *         <ul>
     *           <li>Warehouse with the same business unit code already exists</li>
     *           <li>Required fields are missing or invalid</li>
     *           <li>Business unit code format is invalid</li>
     *         </ul>
     */
    @Override
    public com.warehouse.api.beans.Warehouse createANewWarehouseUnit(
            @NotNull com.warehouse.api.beans.Warehouse data) {

        LOGGER.infof("Request received: create warehouse %s", data.getBusinessUnitCode());

        try {

            createWarehouseOperation.create(
                    data.getBusinessUnitCode(),
                    data.getLocation(),
                    data.getCapacity().intValue()
            );

            Warehouse domain =
                    getWarehouseUseCase
                            .findByBusinessUnitCode(data.getBusinessUnitCode());

            LOGGER.infof("Warehouse created successfully: %s", data.getBusinessUnitCode());

            return toResponse(domain);

        } catch (IllegalArgumentException e) {

            LOGGER.errorf("Error creating warehouse: %s", e.getMessage());

            throw new WebApplicationException(e.getMessage(), 400);
        }
    }

    /* ============================================================
       GET BY ID
    ============================================================ */

    /**
     * Retrieves a specific warehouse unit by its business unit code.
     *
     * @param id the business unit code identifying the warehouse (must not be null)
     * @return the warehouse with matching business unit code, converted to REST response format
     * @throws WebApplicationException with 404 Not Found status if:
     *         <ul>
     *           <li>No warehouse exists with the provided business unit code</li>
     *           <li>The warehouse has been archived</li>
     *         </ul>
     */
    @Override
    public com.warehouse.api.beans.Warehouse getAWarehouseUnitByID(String id) {

        LOGGER.infof("Request received: get warehouse %s", id);

        Warehouse domain =
                getWarehouseUseCase.findByBusinessUnitCode(id);

        if (domain == null) {

            LOGGER.warnf("Warehouse not found: %s", id);

            throw new WebApplicationException(
                    "Warehouse with business unit code '" + id + "' not found",
                    404);
        }

        return toResponse(domain);
    }

    /* ============================================================
       ARCHIVE
    ============================================================ */

    /**
     * Archives (soft-deletes) a warehouse unit by its business unit code.
     *
     * <p>Marks the warehouse as archived without physically deleting it from the database.
     * Archived warehouses are excluded from normal queries but retained for historical and
     * audit trail purposes. This operation is idempotent.
     *
     * @param id the business unit code of the warehouse to archive (must not be null)
     * @throws WebApplicationException with 400 Bad Request status if:
     *         <ul>
     *           <li>Warehouse is not found</li>
     *           <li>Warehouse is already archived</li>
     *           <li>Archive operation fails due to validation constraints</li>
     *         </ul>
     */
    @Override
    public void archiveAWarehouseUnitByID(String id) {

        LOGGER.infof("Request received: archive warehouse %s", id);

        try {

            archiveWarehouseOperation.archive(id);

            LOGGER.infof("Warehouse archived successfully: %s", id);

        } catch (IllegalArgumentException e) {

            LOGGER.errorf("Error archiving warehouse %s : %s", id, e.getMessage());

            throw new WebApplicationException(e.getMessage(), 400);
        }
    }

    /* ============================================================
       REPLACE
    ============================================================ */

    /**
     * Replaces/updates the data of an existing active warehouse.
     *
     * <p>Updates the warehouse's location, capacity, and current stock. The business unit code
     * cannot be modified. This operation performs an optimistic locking check to prevent
     * concurrent modification conflicts.
     *
     * @param businessUnitCode the business unit code of the warehouse to update (must not be null)
     * @param data the new warehouse data (location, capacity, stock)
     * @return the updated warehouse with all current properties, converted to REST response format
     * @throws WebApplicationException with 400 Bad Request status if:
     *         <ul>
     *           <li>Warehouse is not found</li>
     *           <li>Warehouse is archived</li>
     *           <li>Provided data fails validation</li>
     *           <li>Optimistic locking conflict detected (concurrent modification)</li>
     *         </ul>
     */
    @Override
    public com.warehouse.api.beans.Warehouse replaceTheCurrentActiveWarehouse(
            String businessUnitCode,
            @NotNull com.warehouse.api.beans.Warehouse data) {

        LOGGER.infof("Request received: replace warehouse %s", businessUnitCode);

        try {

            replaceWarehouseOperation.replace(
                    businessUnitCode,
                    data.getLocation(),
                    data.getCapacity().intValue(),
                    data.getStock().intValue()
            );

            Warehouse domain =
                    getWarehouseUseCase
                            .findByBusinessUnitCode(businessUnitCode);

            LOGGER.infof("Warehouse replaced successfully: %s", businessUnitCode);

            return toResponse(domain);

        } catch (IllegalArgumentException e) {

            LOGGER.errorf("Error replacing warehouse %s : %s",
                    businessUnitCode, e.getMessage());

            throw new WebApplicationException(
                    e.getMessage(),
                    Response.Status.BAD_REQUEST
            );
        }
    }

    /* ============================================================
       MAPPER
    ============================================================ */

    /**
     * Converts a domain warehouse model to a REST API response bean.
     *
     * <p>Maps domain model properties to the API bean format for HTTP serialization.
     * Only non-null properties are mapped.
     *
     * @param domain the domain warehouse object (must not be null)
     * @return the API response bean with properties populated from the domain object
     */
    private com.warehouse.api.beans.Warehouse toResponse(Warehouse domain) {

        com.warehouse.api.beans.Warehouse response =
                new com.warehouse.api.beans.Warehouse();

        response.setBusinessUnitCode(domain.businessUnitCode);
        response.setLocation(domain.location);
        response.setCapacity(domain.capacity);
        response.setStock(domain.stock);

        return response;
    }
}