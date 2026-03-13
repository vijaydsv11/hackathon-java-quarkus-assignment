package com.fulfilment.application.monolith.warehouses.adapters.database;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing the database persistence of a warehouse.
 * 
 * This entity uses optimistic locking via a @Version column to handle concurrent updates.
 * The businessUnitCode is maintained as a unique constraint to ensure data integrity.
 * The archivedAt timestamp tracks when a warehouse was marked as inactive.
 */
@Entity
public class DbWarehouse {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "warehouse_seq")
	@SequenceGenerator(name = "warehouse_seq", sequenceName = "warehouse_seq", allocationSize = 1)
	public Long id;

	@Version
	@Column(nullable = false)
	public Long version;

	@Column(unique = true, nullable = false)
	public String businessUnitCode;

	@Column(nullable = false)
	public String location;

	@Column(nullable = false)
	public Integer capacity;

	@Column(nullable = false)
	public Integer stock;

	@Column(nullable = false)
	public LocalDateTime createdAt;

	public LocalDateTime archivedAt;

	public DbWarehouse() {
	}
}