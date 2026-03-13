
package com.fulfilment.application.monolith.stores;

import com.fulfilment.application.monolith.stores.application.*;
import com.fulfilment.application.monolith.stores.domain.model.Store;
import com.fulfilment.application.monolith.stores.domain.port.StoreRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("store")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class StoreResource {

    @Inject CreateStoreUseCase createUseCase;
    @Inject UpdateStoreUseCase updateUseCase;
    @Inject DeleteStoreUseCase deleteUseCase;
    @Inject StoreRepository repository;

    @GET
    public List<Store> getAll() {
        return repository.findAll();
    }

    @GET
    @Path("{id}")
    public Store getById(@PathParam("id") Long id) {
        Store store = repository.findById(id);
        if (store == null) throw new WebApplicationException("Store not found", 404);
        return store;
    }

    @POST
    public Response create(Store store) {
        Store created = createUseCase.execute(store);
        return Response.status(201).entity(created).build();
    }

    @PUT
    @Path("{id}")
    public Store update(@PathParam("id") Long id, Store store) {
        return updateUseCase.execute(id, store);
    }

    @PATCH
    @Path("{id}")
    public Store patch(@PathParam("id") Long id, Store store) {
        return updateUseCase.execute(id, store);
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") Long id) {
        deleteUseCase.execute(id);
        return Response.status(204).build();
    }
}
