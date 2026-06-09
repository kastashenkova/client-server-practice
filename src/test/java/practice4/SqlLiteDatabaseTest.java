package practice4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.example.practice4.Database;
import org.example.practice4.Filter;
import org.example.practice4.SqlLiteDatabaseImpl;
import org.example.practice4.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqlLiteDatabaseTest {

    private Database database;

    @BeforeEach
    void setup() {
        database = new SqlLiteDatabaseImpl(":memory:"); // in-memory tests

        database.create(new Product("product1", "category1", 1, BigDecimal.ONE));
        database.create(new Product("product2", "category2", 2, BigDecimal.TWO));
        database.create(new Product("product3", "category3", 3, BigDecimal.valueOf(3)));
    }

    @AfterEach
    void cleanUp() {
        int number = database.deleteAll();
        System.out.printf("Removed %s products", number);
        if (database instanceof AutoCloseable) {
            try {
                ((AutoCloseable) database).close();
            } catch (Exception e) {
                System.err.println("Failed to close database: " + e.getMessage());
            }
        }
    }

    @Test
    void shouldIncreaseCountAfterCreation() {
        int countBefore = database.count();

        database.create(new Product("test4", "test4", 4, BigDecimal.valueOf(4)));

        assertThat(database.count())
                .isEqualTo(countBefore + 1);
    }

    @Test
    void shouldGetProductById() {
        int id = database.create(new Product("test4", "test4", 4, BigDecimal.valueOf(4)));

        assertThat(database.getById(id))
                .isPresent()
                .get()
                .isEqualTo(new Product(id, "test4", "test4", 4, BigDecimal.valueOf(4)));
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
        filter.category = "category2";

        List<Product> products = database.getAll(filter);

        assertThat(products).hasSize(1);
        assertThat(products.getFirst().getName()).isEqualTo("product2");
    }

    @Test
    void shouldUpdateProductSuccessfully() {
        int id = 1;
        Product updatedProduct = new Product(id,
                "newName",
                "newCategory",
                4,
                BigDecimal.valueOf(4));

        int rowsUpdated = database.update(updatedProduct);

        assertThat(rowsUpdated).isEqualTo(1);
        assertThat(database.getById(id))
                .isPresent()
                .get()
                .isEqualTo(updatedProduct);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentProduct() {
        Product nonExistentProduct = new Product(10, "product10", "category10", 10, BigDecimal.TEN);

        assertThatThrownBy(() -> database.update(nonExistentProduct))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldDeleteProductById() {
        int id = 1;
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
}
