package org.example.practice2;

import org.example.practice2.warehouse.WarehouseService;

import java.math.BigDecimal;

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

        scaling.stop();
    }
}
