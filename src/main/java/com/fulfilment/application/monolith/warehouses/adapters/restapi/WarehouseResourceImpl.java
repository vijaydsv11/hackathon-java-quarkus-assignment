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