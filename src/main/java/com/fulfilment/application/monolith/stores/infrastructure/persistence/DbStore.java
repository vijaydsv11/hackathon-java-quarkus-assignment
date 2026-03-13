
package com.fulfilment.application.monolith.stores.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "dbstore",
       uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class DbStore extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    public Integer quantityProductsInStock;

    @Version
    public Long version;
}
