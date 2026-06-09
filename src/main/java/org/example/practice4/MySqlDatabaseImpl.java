package org.example.practice4;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MySqlDatabaseImpl implements Database, AutoCloseable {

    private final Connection connection;

    public MySqlDatabaseImpl(String dbUrl, String username, String password) {
        try {
            this.connection = DriverManager.getConnection(dbUrl, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Can't create MySQL DB", e);
        }

        init();
    }

    @Override
    public int create(Product product) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO product(name, category, quantity, price) values (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setInt(3, product.getQuantity());
            ps.setBigDecimal(4, product.getPrice());

            int inserted = ps.executeUpdate();
            if (inserted < 1) {
                throw new RuntimeException("Failed to create product: " + product.getName());
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            throw new RuntimeException(
                    "Failed to obtain generated id for product: " + product.getName());
        } catch (SQLException e) {
            throw new RuntimeException("Can't create product: " + product, e);
        }
    }

    @Override
    public int count() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM product")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Can't count products", e);
        }
    }

    @Override
    public List<Product> getAll(Filter filter) {
        SqlWrapper wrapper = filterBuilder(filter);
        try (PreparedStatement ps = connection.prepareStatement(wrapper.sql)) {
            for (int i = 0; i < wrapper.params.size(); i++) {
                ps.setObject(i + 1, wrapper.params.get(i));
            }
            List<Product> products = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(new Product(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("price")));
                }
            }

            return products;
        } catch (SQLException e) {
            throw new RuntimeException("Can't get products", e);
        }
    }

    @Override
    public Optional<Product> getById(int id) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product WHERE id = ?")) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Product(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("price")));
                }
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Can't get product by id: " + id, e);
        }
    }

    @Override
    public int update(Product product) {
        try (PreparedStatement ps = connection.prepareStatement("""
                UPDATE product
                SET name = ?, category = ?, quantity = ?, price = ?
                WHERE id = ?
                """)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setInt(3, product.getQuantity());
            ps.setBigDecimal(4, product.getPrice());
            ps.setInt(5, product.getId());

            int updated = ps.executeUpdate();

            if (updated == 0) {
                throw new RuntimeException(
                        "Product with id " + product.getId() + " not found");
            }

            return updated;
        } catch (SQLException e) {
            throw new RuntimeException("Can't update product: " + product, e);
        }
    }

    @Override
    public int deleteAll() {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM product")) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't delete products", e);
        }
    }

    @Override
    public int deleteById(int id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM product WHERE id = ?")) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();

            if (deleted == 0) {
                throw new RuntimeException(
                        "Product with id " + id + " not found");
            }

            return deleted;
        } catch (SQLException e) {
            throw new RuntimeException("Can't delete product by id: " + id, e);
        }
    }

    private void init() {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS product (
                    id INTEGER PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(30) NOT NULL,
                    category VARCHAR(30) NOT NULL,
                    quantity INT NOT NULL,
                    price DECIMAL(10,2) NOT NULL
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException("Exception while database init", e);
        }
    }

    @Override
    public void close() {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Can't close connection", e);
        }
    }

    private SqlWrapper filterBuilder(Filter filter) {
        SqlWrapper wrapper = new SqlWrapper();
        wrapper.params = new ArrayList<>();

        String str = Stream.of(
                        stringEquals("name", filter.name, wrapper.params),
                        stringEquals("category", filter.category, wrapper.params),
                        numberGreaterOrEqual("quantity", filter.minQuantity, wrapper.params),
                        numberLessOrEqual("quantity", filter.maxQuantity, wrapper.params),
                        numberGreaterOrEqual("price", filter.minPrice, wrapper.params),
                        numberLessOrEqual("price", filter.maxPrice, wrapper.params))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" and "));

        StringBuilder builder = new StringBuilder("SELECT * FROM product");
        if (!str.isEmpty()) {
            builder.append(" WHERE ");
            builder.append(str);
        }

        if (filter.limit != null) {
            builder.append(" LIMIT ?");
            wrapper.params.add(filter.limit);

            if (filter.offset != null) {
                builder.append(" OFFSET ?");
                wrapper.params.add(filter.offset);
            }
        }

        wrapper.sql = builder.toString();
        return wrapper;
    }

    private String stringEquals(String columnName, String value, List<Object> params) {
        if (value == null) {
            return null;
        }
        params.add(value);
        return columnName + " = ?";
    }

    private String numberGreaterOrEqual(String columnName, Object value, List<Object> params) {
        if (value == null) {
            return null;
        }
        params.add(value);
        return columnName + " >= ?";
    }

    private String numberLessOrEqual(String columnName, Object value, List<Object> params) {
        if (value == null) {
            return null;
        }
        params.add(value);
        return columnName + " <= ?";
    }

//    private String stringIn(String columnName, List<Object> values, List<Object> params) {
//        if (values == null || values.isEmpty()) {
//            return null;
//        }
//        params.addAll(values);
//        return columnName + " IN(" + values.stream().map(i -> "?").collect(Collectors.joining(",")) + ")";
//    }
}
