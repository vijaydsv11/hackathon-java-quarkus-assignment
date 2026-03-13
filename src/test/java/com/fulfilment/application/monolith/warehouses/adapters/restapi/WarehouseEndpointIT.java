package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
public class WarehouseEndpointIT {

    @Test
    public void testSimpleListWarehouses() {

        final String path = "warehouse";

        given()
                .when()
                .get(path)
                .then()
                .statusCode(200)
                .body(
                        containsString("MWH.001"),
                        containsString("MWH.012"),
                        containsString("MWH.023")
                );
    }
}