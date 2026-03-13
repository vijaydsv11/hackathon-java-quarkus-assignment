package com.fulfilment.application.monolith.warehouses.domain.models;

import java.time.LocalDateTime;

/**
 * Domain model representing a warehouse aggregate.
 * 
 * A warehouse is identified by its business unit code and manages inventory
 * at a specific location. It tracks capacity constraints and can be archived.
 * 
 * This is the core aggregate for warehouse management, enforcing all business rules
 * related to warehouse operations like archiving and stock management.
 */
public class Warehouse {

	// Must be non-final for default constructor compatibility
	public String businessUnitCode;
	public String location;
	public int capacity;
	public int stock;
	public LocalDateTime createdAt;
	public LocalDateTime archivedAt;

	// Required for tests
	public Warehouse() {
	}

	/**
	 * Factory method to create a new warehouse with proper validation.
	 * 
	 * @param code the unique business unit code for the warehouse
	 * @param location the location identifier where the warehouse operates
	 * @param capacity the total capacity of the warehouse
	 * @param locationMaxCapacity the maximum capacity allowed at the location
	 * @return a new Warehouse instance with initialized properties
	 * @throws IllegalArgumentException if any parameter validation fails
	 */
	public static Warehouse create(String code, String location, int capacity, int locationMaxCapacity) {

		validateBusinessUnitCode(code);
		validateLocation(location);
		validateCapacity(capacity, locationMaxCapacity);

		Warehouse warehouse = new Warehouse();
		warehouse.businessUnitCode = code;
		warehouse.location = location;
		warehouse.capacity = capacity;
		warehouse.stock = 0;
		warehouse.createdAt = LocalDateTime.now();
		warehouse.archivedAt = null;

		return warehouse;
	}


	/**
	 * Adds stock to the warehouse if not archived.
	 * 
	 * @param quantity the quantity to add (must be positive)
	 * @throws IllegalArgumentException if quantity is not positive or would exceed capacity
	 * @throws IllegalStateException if warehouse is archived
	 */
	public void addStock(int quantity) {
		ensureNotArchived();

		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be greater than zero");
		}

		if (this.stock + quantity > this.capacity) {
			throw new IllegalArgumentException("Stock cannot exceed capacity");
		}

		this.stock += quantity;
	}

	/**
	 * Archives the warehouse by setting the archive timestamp.
	 * 
	 * @throws IllegalArgumentException if warehouse is already archived
	 */
	public void archive() {
		if (this.archivedAt != null) {
			throw new IllegalArgumentException("Warehouse already archived");
		}
		this.archivedAt = LocalDateTime.now();
	}

	/**
	 * Validates the warehouse is not archived before allowing modifications.
	 * 
	 * @throws IllegalArgumentException if warehouse is archived
	 */
	private void ensureNotArchived() {
		if (this.archivedAt != null) {
			throw new IllegalArgumentException("Archived warehouse cannot be modified");
		}
	}

	/**
	 * Validates business unit code is not null or blank.
	 * 
	 * @param code the code to validate
	 * @throws IllegalArgumentException if code is null or blank
	 */
	private static void validateBusinessUnitCode(String code) {
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("Business unit code must not be blank");
		}
	}

	/**
	 * Validates location identifier is not null or blank.
	 * 
	 * @param location the location to validate
	 * @throws IllegalArgumentException if location is null or blank
	 */
	private static void validateLocation(String location) {
		if (location == null || location.isBlank()) {
			throw new IllegalArgumentException("Location must not be blank");
		}
	}

	/**
	 * Validates capacity is positive and within location constraints.
	 * 
	 * @param capacity the capacity to validate
	 * @param maxCapacity the maximum allowed capacity for the location
	 * @throws IllegalArgumentException if capacity is non-positive or exceeds max
	 */
	private static void validateCapacity(int capacity, int maxCapacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("Capacity must be positive");
		}

		if (capacity > maxCapacity) {
			throw new IllegalArgumentException("Capacity exceeds location max capacity");
		}
	}

	public static Warehouse reconstruct(String businessUnitCode, String location, int capacity,
			int stock, LocalDateTime createdAt, LocalDateTime archivedAt) {

		Warehouse warehouse = new Warehouse();
		warehouse.businessUnitCode = businessUnitCode;
		warehouse.location = location;
		warehouse.capacity = capacity;
		warehouse.stock = stock;
		warehouse.createdAt = createdAt;
		warehouse.archivedAt = archivedAt;

		return warehouse;
	}
}