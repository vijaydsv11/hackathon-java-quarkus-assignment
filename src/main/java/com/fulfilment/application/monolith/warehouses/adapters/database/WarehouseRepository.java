package com.fulfilment.application.monolith.warehouses.adapters.database;

import java.util.List;

import org.jboss.logging.Logger;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;

/**
 * Repository adapter for Warehouse persistence operations.
 *
 * <p>
 * This class implements the {@link WarehouseStore} port and provides database
 * access using
 * Quarkus Hibernate ORM Panache. It handles all CRUD operations for Warehouse
 * entities,
 * managing both read and write transactions with appropriate logging.
 *
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>Fetch warehouses (all, by business unit code)</li>
 * <li>Create, update, and delete warehouse records</li>
 * <li>Execute complex search queries with filtering and pagination</li>
 * <li>Map between domain and entity models</li>
 * <li>Ensure transaction consistency and concurrency control</li>
 * </ul>
 *
 * @see WarehouseStore
 * @see DbWarehouse
 * @see Warehouse
 */
@ApplicationScoped
public class WarehouseRepository
        implements WarehouseStore, PanacheRepository<DbWarehouse> {

    private static final Logger LOGGER = Logger.getLogger(WarehouseRepository.class);

    /*
     * ==========================
     * READ
     * ==========================
     */

    /**
     * Retrieves all active (non-archived) warehouses.
     *
     * @return a list of all warehouses in the system, or an empty list if none
     *         exist
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    @Override
    public List<Warehouse> getAll() {

        LOGGER.debug("Fetching all warehouses");

        return listAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * Finds a warehouse by its business unit code.
     *
     * @param buCode the business unit code to search for (must not be null)
     * @return the warehouse matching the business unit code, or {@code null} if not
     *         found
     * @see #existsByBusinessUnitCode(String)
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {

        if (buCode == null) {
            LOGGER.warn("Attempted to find warehouse with null businessUnitCode");
            return null;
        }

        LOGGER.debugf("Finding warehouse with BU code: %s", buCode);

        return find("businessUnitCode = ?1", buCode)
                .firstResultOptional()
                .map(this::toDomain)
                .orElse(null);
    }

    /**
     * Checks if a warehouse exists for the given business unit code.
     *
     * @param buCode the business unit code to check
     * @return {@code true} if a warehouse exists, {@code false} otherwise
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    @Override
    public boolean existsByBusinessUnitCode(String buCode) {

        LOGGER.debugf("Checking existence of warehouse: %s", buCode);

        return count("businessUnitCode = ?1", buCode) > 0;
    }

    /*
     * ==========================
     * WRITE
     * ==========================
     */

    /**
     * Creates a new warehouse in the database.
     *
     * <p>
     * This operation persists the warehouse immediately and flushes the transaction
     * to ensure visibility to concurrent operations (important for concurrency
     * tests).
     *
     * @param warehouse the warehouse to create (must not be null)
     * @throws IllegalArgumentException if warehouse validation fails
     */
    @Transactional
    @Override
    public void create(Warehouse warehouse) {

        LOGGER.infof("Creating warehouse: %s", warehouse.businessUnitCode);

        DbWarehouse entity = toEntity(warehouse);

        // Important for concurrency tests
        persistAndFlush(entity);
        warehouse.version = entity.version;

        LOGGER.infof("Warehouse created successfully: %s", warehouse.businessUnitCode);
    }

    /**
     * Updates an existing warehouse with new values.
     *
     * <p>
     * Updates the following fields: location, capacity, stock, and archived
     * timestamp.
     *
     * @param warehouse the warehouse with updated values (must not be null)
     * @throws IllegalStateException if the warehouse is not found in the database
     */
    @Transactional
    @Override
    public void update(Warehouse warehouse) {

        LOGGER.debugf(
                "Warehouse update request - businessUnitCode: %s, version: %s, location: %s, capacity: %s",
                warehouse.businessUnitCode,
                warehouse.version,
                warehouse.location,
                warehouse.capacity);

        DbWarehouse db = find("businessUnitCode = ?1 and version = ?2",
                warehouse.businessUnitCode,
                warehouse.version)
                .firstResultOptional()
                .orElseThrow(() -> new OptimisticLockException(
                        "Warehouse version mismatch for " +
                                warehouse.businessUnitCode + " with version " + warehouse.version));

        db.location = warehouse.location;
        db.capacity = warehouse.capacity;
        db.stock = warehouse.stock;
        db.archivedAt = warehouse.archivedAt;

        flush();

        LOGGER.infof("Warehouse updated successfully: %s, new version: %s",
                db.businessUnitCode,
                db.version);
    }

    /**
     * Deletes a warehouse from the database.
     *
     * @param warehouse the warehouse to delete (must not be null)
     */
    @Transactional
    @Override
    public void remove(Warehouse warehouse) {

        LOGGER.warnf("Deleting warehouse: %s", warehouse.businessUnitCode);

        delete("businessUnitCode = ?1", warehouse.businessUnitCode);
    }

    /*
     * ==========================
     * Mapping
     * ==========================
     */

    /**
     * Converts a database entity to a domain model.
     *
     * @param db the database entity (must not be null)
     * @return the corresponding domain warehouse object
     */
    private Warehouse toDomain(DbWarehouse db) {

        return Warehouse.reconstruct(
                db.businessUnitCode,
                db.location,
                db.capacity,
                db.stock,
                db.version,
                db.createdAt,
                db.archivedAt);
    }

    /**
     * Converts a domain model to a database entity.
     *
     * @param domain the domain warehouse object (must not be null)
     * @return the corresponding database entity
     */
    private DbWarehouse toEntity(Warehouse domain) {

        DbWarehouse db = new DbWarehouse();

        db.businessUnitCode = domain.businessUnitCode;
        db.location = domain.location;
        db.capacity = domain.capacity;
        db.stock = domain.stock;
        db.version = domain.version;
        db.createdAt = domain.createdAt;
        db.archivedAt = domain.archivedAt;

        return db;
    }

    /**
     * Searches for warehouses based on multiple criteria with pagination and
     * sorting.
     *
     * <p>
     * Filters are optional and can be combined:
     * <ul>
     * <li>Location: exact match filter</li>
     * <li>Capacity: range filter (min and/or max)</li>
     * <li>Pagination: splits results into pages</li>
     * <li>Sorting: orders results by specified field</li>
     * </ul>
     *
     * <p>
     * Only active (non-archived) warehouses are returned.
     *
     * @param location    the warehouse location to filter by (optional, can be
     *                    null)
     * @param minCapacity minimum warehouse capacity (optional, can be null)
     * @param maxCapacity maximum warehouse capacity (optional, can be null)
     * @param page        the page number (0-indexed)
     * @param pageSize    the number of results per page (capped at 100)
     * @param sortBy      the field name to sort by (e.g., "capacity", "location")
     * @param sortOrder   sorting direction: "asc" or "desc"
     * @return a list of warehouses matching the search criteria
     * @throws IllegalArgumentException if query parameters are invalid
     * @throws RuntimeException         if the search operation fails
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Warehouse> search(
            String location,
            Integer minCapacity,
            Integer maxCapacity,
            int page,
            int pageSize,
            String sortBy,
            String sortOrder) {

        try {
            LOGGER.debugf("Building search query - Location: %s, MinCap: %s, MaxCap: %s, Page: %d, PageSize: %d",
                    location, minCapacity, maxCapacity, page, pageSize);

            StringBuilder query = new StringBuilder("archivedAt IS NULL");
            var params = new java.util.HashMap<String, Object>();

            if (location != null) {
                query.append(" and location = :location");
                params.put("location", location);
            }

            if (minCapacity != null) {
                query.append(" and capacity >= :minCapacity");
                params.put("minCapacity", minCapacity);
            }

            if (maxCapacity != null) {
                query.append(" and capacity <= :maxCapacity");
                params.put("maxCapacity", maxCapacity);
            }

            io.quarkus.panache.common.Sort sort = "desc".equalsIgnoreCase(sortOrder)
                    ? io.quarkus.panache.common.Sort.by(sortBy).descending()
                    : io.quarkus.panache.common.Sort.by(sortBy);

            List<Warehouse> results = find(query.toString(), sort, params)
                    .page(page, Math.min(pageSize, 100))
                    .list()
                    .stream()
                    .map(this::toDomain)
                    .toList();

            LOGGER.infof("Search query executed successfully - Found %d warehouses", results.size());
            return results;

        } catch (IllegalArgumentException e) {
            LOGGER.warnf(e, "Invalid query parameters for warehouse search: %s", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.errorf(e, "Error executing warehouse search query: %s", e.getMessage());
            throw new RuntimeException("Failed to search warehouses", e);
        }
    }
}