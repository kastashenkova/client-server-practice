package org.example.practice4;

import java.math.BigDecimal;
import java.util.Objects;

public class Product {
    private Integer id;
    private String name;
    private String category;
    private int quantity;
    private BigDecimal price;

    public Product(String name, String category, int quantity, BigDecimal price) {
        this(null, name, category, quantity, price);
    }

    public Product(Integer id, String name, String category, int quantity, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return quantity == product.quantity
                && Objects.equals(id, product.id)
                && Objects.equals(name, product.name)
                && (price != null && product.price != null ?
                price.compareTo(product.price) == 0 : price == product.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, category, quantity, price);
    }

    @Override
    public String toString() {
        return "Product{"
                + "id=" + id
                + ", name='" + name
                + '\'' + ", category='" + category + '\''
                + ", quantity=" + quantity
                + ", price=" + price + '}';
    }
}
