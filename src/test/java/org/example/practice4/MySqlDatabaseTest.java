package org.example.practice4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MySqlDatabaseTest extends BaseMySqlTest {

    private Database database;

    @BeforeEach
    void setup() {
        database = new MySqlDatabaseImpl(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());

        database.create(new Product("product1", 1, 1, BigDecimal.ONE));
        database.create(new Product("product2", 2, 2, BigDecimal.TWO));
        database.create(new Product("product3", 3, 3, BigDecimal.valueOf(3)));
    }

    @AfterEach
    void cleanUp() {
        int number = database.deleteAll();
        System.out.printf("Removed %s products%n", number);
    }

    @Test
    void shouldIncreaseCountAfterCreation() {
        int countBefore = database.count();

        database.create(new Product("test4", 4, 4, BigDecimal.valueOf(4)));

        assertThat(database.count())
                .isEqualTo(countBefore + 1);
    }

    @Test
    void shouldGetProductById() {
        int id = database.create(new Product("test4", 4, 4, BigDecimal.valueOf(4)));

        assertThat(database.getById(id))
                .isPresent()
                .get()
                .isEqualTo(new Product(id, "test4", 4, 4, BigDecimal.valueOf(4)));
    }

    @Test
    void shouldReturnEmptyOptionalWhenProductNotFound() {
        assertThat(database.getById(10)).isEmpty();
    }

    @Test
    void shouldGetAllThreeProducts() {
        Filter filter = new Filter();

        List<Product> products = database.getAll(filter);

        assertThat(products).hasSize(3);
        assertThat(products)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("product1", "product2", "product3");
    }

    @Test
    void shouldFilterProductsByCategory() {
        Filter filter = new Filter();
        filter.categoryIds = List.of(1, 2);

        List<Product> products = database.getAll(filter);

        assertThat(products).hasSize(2);
        assertThat(products)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("product1", "product2");
    }

    @Test
    void shouldUpdateProductSuccessfully() {
        int id = database.create(new Product("someName", 10, 10, BigDecimal.TEN));
        Product updatedProduct = new Product(id,
                "newName",
                20,
                20,
                BigDecimal.valueOf(20));

        int rowsUpdated = database.update(updatedProduct);

        assertThat(rowsUpdated).isEqualTo(1);
        assertThat(database.getById(id))
                .isPresent()
                .get()
                .isEqualTo(updatedProduct);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentProduct() {
        Product nonExistentProduct = new Product(10, "product10", 10, 10, BigDecimal.TEN);

        assertThatThrownBy(() -> database.update(nonExistentProduct))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldDeleteProductById() {
        int id = database.create(new Product("someName", 10, 10, BigDecimal.TEN));
        int countBefore = database.count();
        int rowsDeleted = database.deleteById(id);
        assertThat(rowsDeleted).isEqualTo(1);
        assertThat(database.count()).isEqualTo(countBefore - 1);
        assertThat(database.getById(id)).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentProduct() {
        assertThatThrownBy(() -> database.deleteById(10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldFilterProductsByQuantityRange() {
        Filter filter = new Filter();
        filter.minQuantity = 2;
        filter.maxQuantity = 3;

        List<Product> products = database.getAll(filter);

        assertThat(products)
                .hasSize(2)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("product2", "product3");
    }

    @Test
    void shouldFilterProductsByPriceRange() {
        Filter filter = new Filter();
        filter.minPrice = BigDecimal.TWO;
        filter.maxPrice = BigDecimal.TEN;

        List<Product> products = database.getAll(filter);

        assertThat(products)
                .hasSize(2)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("product2", "product3");
    }

    @Test
    void shouldReturnLimitedNumberOfProducts() {
        Filter filter = new Filter();
        filter.limit = 2;

        List<Product> products = database.getAll(filter);

        assertThat(products).hasSize(2);
    }

    @Test
    void shouldReturnProductsWithOffset() {
        Filter filter = new Filter();
        filter.limit = 1;
        filter.offset = 1;

        List<Product> products = database.getAll(filter);

        assertThat(products).hasSize(1);
        assertThat(products.getFirst().getName()).isEqualTo("product2");
    }

    @Test
    void shouldFilterProductsByMultipleConditionsCombined() {
        Filter filter = new Filter();
        filter.categoryIds = List.of(2, 3);
        filter.minPrice = BigDecimal.valueOf(2.5);
        filter.limit = 10;

        List<Product> products = database.getAll(filter);

        assertThat(products).hasSize(1);
        assertThat(products.getFirst().getName()).isEqualTo("product3");
    }

    @Test
    void shouldReturnAllProductsWhenFilterHasAllNullFields() {
        Filter filter = new Filter();

        List<Product> products = database.getAll(filter);

        assertThat(products).hasSize(3);
    }

    @Test
    void shouldReturnAllProductsWhenFilterIsNull() {
        List<Product> products = database.getAll(null);

        assertThat(products).hasSize(3);
    }

    @Test
    void shouldIgnoreOffsetIfLimitIsNotSet() {
        Filter filter = new Filter();
        filter.offset = 1;

        List<Product> products = database.getAll(filter);

        assertThat(products).hasSize(3);
    }
}
