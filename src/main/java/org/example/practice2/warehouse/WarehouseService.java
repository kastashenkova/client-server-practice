package org.example.practice2.warehouse;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WarehouseService {
    private final HashMap<String, Integer> quantities = new HashMap<>();
    private final HashMap<String, Double> prices = new HashMap<>();
    private final HashMap<String, Set<String>> groups = new HashMap<>();

    public int getStock(String product) {
        return quantities.getOrDefault(product, 0);
    }

    public synchronized int addProducts(String product, int quantity) {
        int current = quantities.getOrDefault(product, 0);
        int updated = current + quantity;
        quantities.put(product, updated);
        return updated;
    }

    public synchronized int deductProducts(String product, int quantityToDeduct) {
        int current = quantities.getOrDefault(product, 0);
        int updated = Math.max(0, current - quantityToDeduct);
        quantities.put(product, updated);
        return updated;
    }

    public synchronized void addGroup(String groupName) {
        groups.putIfAbsent(groupName, ConcurrentHashMap.newKeySet());
    }

    public synchronized void addProductToGroup(String groupName, String product) {
        groups.computeIfAbsent(groupName, k -> ConcurrentHashMap.newKeySet())
                .add(product);
    }

    public synchronized void setPrice(String product, double price) {
        prices.put(product, price);
    }

    public synchronized Double getPrice(String product) {
        return prices.get(product);
    }
}
