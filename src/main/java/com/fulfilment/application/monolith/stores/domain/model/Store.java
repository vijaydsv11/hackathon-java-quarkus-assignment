
package com.fulfilment.application.monolith.stores.domain.model;

public class Store {

    private Long id;
    private String name;
    private Integer quantityProductsInStock;

    public Store(Long id, String name, Integer quantityProductsInStock) {
        this.id = id;
        this.name = name;
        this.quantityProductsInStock = quantityProductsInStock;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Integer getQuantityProductsInStock() { return quantityProductsInStock; }

    public void setId(Long id) { this.id = id; }

    public void update(String name, Integer quantity) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Store name must not be blank");
        }
        this.name = name;
        this.quantityProductsInStock = quantity;
    }
}
