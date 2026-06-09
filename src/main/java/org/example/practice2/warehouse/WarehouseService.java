package org.example.practice2.warehouse;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WarehouseService {
    private final ConcurrentHashMap<String, Integer> quantities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> prices = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> groups = new ConcurrentHashMap<>();

    public int getStock(String product) {
        return quantities.getOrDefault(product, 0);
    }

    public int addProducts(String product, int quantity) {
        int current = quantities.getOrDefault(product, 0);
        int updated = current + quantity;
        quantities.put(product, updated);
        return updated;
    }

    public int deductProducts(String product, int quantityToDeduct) {
        int current = quantities.getOrDefault(product, 0);
        int updated = Math.max(0, current - quantityToDeduct);
        quantities.put(product, updated);
        return updated;
    }

    public void addGroup(String groupName) {
        groups.putIfAbsent(groupName, ConcurrentHashMap.newKeySet());
    }

    public void addProductToGroup(String groupName, String product) {
        groups.computeIfAbsent(groupName, k -> ConcurrentHashMap.newKeySet())
                .add(product);
    }

    public void setPrice(String product, double price) {
        prices.put(product, price);
    }

    public Double getPrice(String product) {
        return prices.get(product);
    }
}
