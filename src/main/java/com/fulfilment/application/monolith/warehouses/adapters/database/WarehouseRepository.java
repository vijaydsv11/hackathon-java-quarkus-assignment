package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class WarehouseRepository
        implements WarehouseStore, PanacheRepository<DbWarehouse> {

    private static final Logger LOGGER =
            Logger.getLogger(WarehouseRepository.class);

    /* ==========================
       READ
    ========================== */

    @Transactional(Transactional.TxType.SUPPORTS)
    @Override
    public List<Warehouse> getAll() {

        LOGGER.debug("Fetching all warehouses");

        return listAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

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

    @Transactional(Transactional.TxType.SUPPORTS)
    @Override
    public boolean existsByBusinessUnitCode(String buCode) {

        LOGGER.debugf("Checking existence of warehouse: %s", buCode);

        return count("businessUnitCode = ?1", buCode) > 0;
    }

    /* ==========================
       WRITE
    ========================== */

    @Transactional
    @Override
    public void create(Warehouse warehouse) {

        LOGGER.infof("Creating warehouse: %s", warehouse.businessUnitCode);

        DbWarehouse entity = toEntity(warehouse);

        // Important for concurrency tests
        persistAndFlush(entity);

        LOGGER.infof("Warehouse created successfully: %s", warehouse.businessUnitCode);
    }

    @Transactional
    @Override
    public void update(Warehouse warehouse) {

        LOGGER.infof("Updating warehouse: %s", warehouse.businessUnitCode);

        DbWarehouse db = find("businessUnitCode = ?1", warehouse.businessUnitCode)
                .firstResultOptional()
                .orElseThrow(() -> new IllegalStateException("Warehouse not found"));

        db.location = warehouse.location;
        db.capacity = warehouse.capacity;
        db.stock = warehouse.stock;
        db.archivedAt = warehouse.archivedAt;

        LOGGER.infof("Warehouse updated successfully: %s", warehouse.businessUnitCode);
    }

    @Transactional
    @Override
    public void remove(Warehouse warehouse) {

        LOGGER.warnf("Deleting warehouse: %s", warehouse.businessUnitCode);

        delete("businessUnitCode = ?1", warehouse.businessUnitCode);
    }

    /* ==========================
       Mapping
    ========================== */

    private Warehouse toDomain(DbWarehouse db) {

        return Warehouse.reconstruct(
                db.businessUnitCode,
                db.location,
                db.capacity,
                db.stock,
                db.createdAt,
                db.archivedAt
        );
    }

    private DbWarehouse toEntity(Warehouse domain) {

        DbWarehouse db = new DbWarehouse();

        db.businessUnitCode = domain.businessUnitCode;
        db.location = domain.location;
        db.capacity = domain.capacity;
        db.stock = domain.stock;
        db.createdAt = domain.createdAt;
        db.archivedAt = domain.archivedAt;

        return db;
    }
    
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

            io.quarkus.panache.common.Sort sort =
                    "desc".equalsIgnoreCase(sortOrder)
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