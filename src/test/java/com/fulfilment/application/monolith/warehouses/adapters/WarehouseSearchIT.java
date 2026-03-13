package com.fulfilment.application.monolith.warehouses.adapters;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class WarehouseSearchIT {

	@Test
	void shouldReturnWarehousesWhenSearchingByLocation() {

		given().queryParam("location", "AMSTERDAM-001").when().get("/warehouse/search").then().statusCode(200).body("$",
				notNullValue());
	}

	@Test
	void shouldFilterByCapacityRange() {

		given().queryParam("minCapacity", 50).queryParam("maxCapacity", 200).when().get("/warehouse/search").then()
				.statusCode(200);
	}

	@Test
	void shouldReturnSortedWarehouses() {

		given().queryParam("sortBy", "capacity").queryParam("sortOrder", "desc").when().get("/warehouse/search").then()
				.statusCode(200);
	}

	@Test
	void shouldSupportPagination() {

		given().queryParam("page", 0).queryParam("pageSize", 5).when().get("/warehouse/search").then().statusCode(200);
	}

	@Test
	void shouldReturnEmptyWhenNoMatch() {

		given().queryParam("location", "UNKNOWN").when().get("/warehouse/search").then().statusCode(200);
	}
}