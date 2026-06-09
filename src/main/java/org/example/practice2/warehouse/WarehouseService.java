package org.example.practice2.warehouse;

import java.math.BigDecimal;
import java.util.Optional;
import org.example.practice4.Product;
import org.example.practice4.SqlLiteDatabaseImpl;

public class WarehouseService {
    private final SqlLiteDatabaseImpl database;
    private final int defaultCategoryId;

    public WarehouseService(SqlLiteDatabaseImpl database) {
        this.database = database;
        this.defaultCategoryId = database.getOrCreateCategory("Default");
    }

    public synchronized int getStock(String productName) {
        return database.getProductByName(productName)
                .map(Product::getQuantity)
                .orElse(0);
    }

    public synchronized int addProducts(String productName, int quantity) {
        Optional<Product> optProduct = database.getProductByName(productName);
        if (optProduct.isPresent()) {
            Product p = optProduct.get();
            p.setQuantity(p.getQuantity() + quantity);
            database.update(p);
            return p.getQuantity();
        } else {
            Product p = new Product(productName, defaultCategoryId, quantity, BigDecimal.ZERO);
            database.create(p);
            return quantity;
        }
    }

    public synchronized int deductProducts(String productName, int quantityToDeduct) {
        Optional<Product> optProduct = database.getProductByName(productName);
        if (optProduct.isPresent()) {
            Product p = optProduct.get();
            int updated = Math.max(0, p.getQuantity() - quantityToDeduct);
            p.setQuantity(updated);
            database.update(p);
            return updated;
        }
        return 0;
    }

    public synchronized void addGroup(String groupName) {
        database.getOrCreateCategory(groupName);
    }

    public synchronized void addProductToGroup(String groupName, String productName) {
        int categoryId = database.getOrCreateCategory(groupName);
        Optional<Product> optProduct = database.getProductByName(productName);

        if (optProduct.isPresent()) {
            Product p = optProduct.get();
            p.setCategoryId(categoryId);
            database.update(p);
        } else {
            Product p = new Product(productName, categoryId, 0, BigDecimal.ZERO);
            database.create(p);
        }
    }

    public synchronized void setPrice(String productName, double price) {
        Optional<Product> optProduct = database.getProductByName(productName);
        if (optProduct.isPresent()) {
            Product p = optProduct.get();
            p.setPrice(BigDecimal.valueOf(price));
            database.update(p);
        } else {
            Product p = new Product(productName, defaultCategoryId, 0, BigDecimal.valueOf(price));
            database.create(p);
        }
    }

    public synchronized BigDecimal getPrice(String productName) {
        return BigDecimal.valueOf(
                database.getProductByName(productName)
                .map(p -> p.getPrice().doubleValue())
                .orElse(0.0));
    }
}
