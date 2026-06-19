package org.example.practice4;

import java.util.List;
import java.util.Optional;

public interface Database {
    int create(Product product);

    List<Product> getAll(Filter filter);

    Optional<Product> getById(int id);

    int update(Product product);

    int deleteAll();

    int deleteById(int id);

    int count();

    Optional<Product> getProductByName(String name);

    void createCategory(Category category);

    Optional<Category> getCategoryById(int id);
}
