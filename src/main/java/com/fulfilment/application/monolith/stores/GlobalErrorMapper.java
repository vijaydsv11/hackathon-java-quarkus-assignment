
package com.fulfilment.application.monolith.stores;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
@ApplicationScoped
public class GlobalErrorMapper implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = Logger.getLogger(GlobalErrorMapper.class);

    @Inject ObjectMapper objectMapper;

    @Override
    public Response toResponse(Exception exception) {

        LOGGER.error("Failed to handle request", exception);

        int code = 500;
        if (exception instanceof WebApplicationException) {
            code = ((WebApplicationException) exception).getResponse().getStatus();
        }

        ObjectNode json = objectMapper.createObjectNode();
        json.put("exceptionType", exception.getClass().getName());
        json.put("code", code);

        if (exception.getMessage() != null) {
            json.put("error", exception.getMessage());
        }

        return Response.status(code).entity(json).build();
    }
}
