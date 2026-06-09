package org.example.practice2;

import org.example.practice2.warehouse.WarehouseService;
import org.example.practice4.Filter;
import org.example.practice4.Product;

import java.math.BigDecimal;
import java.util.List;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Scaling scaling = new Scaling(2, 2, 4, 3, 5);
        scaling.start();

        Thread.sleep(5000);

        WarehouseService warehouse = scaling.getWarehouseService();

        String[] products = {"rice", "buckwheat", "pasta", "beans", "oats"};

        for (String product : products) {
            int stock = warehouse.getStock(product);
            BigDecimal price = warehouse.getPrice(product);
            System.out.println("Product: " + product + ", left: " + stock + ", current price: " + price);
        }

        Filter filter = new Filter();
        filter.minQuantity = 100;
        filter.maxPrice = new BigDecimal("50.00");
        filter.limit = 3;

        List<Product> searchResults = warehouse.searchProducts(filter);

        if (searchResults.isEmpty()) {
            System.out.println("No products found for filter");
        } else {
            for (Product p : searchResults) {
                System.out.println("For filter found product: " + p.getName()
                        + ", left: " + p.getQuantity()
                        + ", current price: " + p.getPrice());

            }
        }

        scaling.stop();
    }
}
