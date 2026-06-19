package org.example.practice4;

import org.example.practice5.User;

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
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SqlLiteDatabaseImpl implements Database, AutoCloseable {
    private final Connection connection;
    private final ReentrantLock lock = new ReentrantLock();

    public SqlLiteDatabaseImpl(String dbName) {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        } catch (SQLException e) {
            throw new RuntimeException("Can't create SQLite DB", e);
        }
        init();
    }

    @Override
    public int create(Product product) {
        lock.lock();
        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO product(name, categoryId, quantity, price) values (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, product.getName());
                ps.setInt(2, product.getCategoryId());
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
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int count() {
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Product> getAll(Filter filter) {
        lock.lock();
        try {
            if (filter == null) {
                filter = new Filter();
            }
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
                                rs.getInt("categoryId"),
                                rs.getInt("quantity"),
                                rs.getBigDecimal("price")));
                    }
                }

                return products;
            } catch (SQLException e) {
                throw new RuntimeException("Can't get products", e);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<Product> getById(int id) {
        lock.lock();
        try {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product WHERE id = ?")) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new Product(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getInt("categoryId"),
                                rs.getInt("quantity"),
                                rs.getBigDecimal("price")));
                    }
                }

                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException("Can't get product by id: " + id, e);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int update(Product product) {
        lock.lock();
        try {
            try (PreparedStatement ps = connection.prepareStatement("""
                UPDATE product
                SET name = ?, categoryId = ?, quantity = ?, price = ?
                WHERE id = ?
                """)) {
                ps.setString(1, product.getName());
                ps.setInt(2, product.getCategoryId());
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
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int deleteAll() {
        lock.lock();
        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM product")) {
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Can't delete products", e);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int deleteById(int id) {
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
    }

    private void init() {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");

            statement.execute("""
            CREATE TABLE IF NOT EXISTS category (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name VARCHAR(30) UNIQUE NOT NULL
            )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS product (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name VARCHAR(30) NOT NULL,
                    categoryId INTEGER NOT NULL REFERENCES category(id),
                    quantity INT NOT NULL,
                    price DECIMAL(10,2) NOT NULL
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(20) NOT NULL
                )
                """);

            statement.execute("""
                INSERT OR IGNORE INTO users (id, username, password, role)
                VALUES
                    (1, 'kastashenkova', 'admin', 'ROLE_ADMIN'),
                    (2, 'user', 'user_password', 'ROLE_USER');
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

    @Override
    public Optional<Category> getCategoryById(int id) {
        lock.lock();
        try {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM category WHERE id = ?")) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new Category(
                                rs.getInt("id"),
                                rs.getString("name")));
                    }
                }

                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException("Can't get category by id: " + id, e);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<Product> getProductByName(String name) {
        lock.lock();
        try {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new Product(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getInt("categoryId"),
                                rs.getInt("quantity"),
                                rs.getBigDecimal("price")));
                    }
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException("Can't get product by name: " + name, e);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void createCategory(Category category) {
        lock.lock();
        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO category(name) values (?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, category.name());

                int inserted = ps.executeUpdate();
                if (inserted < 1) {
                    throw new RuntimeException("Failed to create category: " + category.name());
                }
            } catch (SQLException e) {
                throw new RuntimeException("Can't create category: " + category, e);
            }
        } finally {
            lock.unlock();
        }
    }

    public int getOrCreateCategory(String name) {
        lock.lock();
        try {
            try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM category WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error checking category", e);
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO category(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
                throw new RuntimeException("Failed to obtain id for category");
            } catch (SQLException e) {
                throw new RuntimeException("Can't create category: " + name, e);
            }
        } finally {
            lock.unlock();
        }
    }

    public Optional<User> getUserByUsername(String username) {
        lock.lock();
        try {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new User(
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getString("password"),
                                rs.getString("role")
                        ));
                    }
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException("Can't get user by username: " + username, e);
            }
        } finally {
            lock.unlock();
        }
    }

    private SqlWrapper filterBuilder(Filter filter) {
        SqlWrapper wrapper = new SqlWrapper();
        wrapper.params = new ArrayList<>();

        String str = Stream.of(
                        stringEquals("name", filter.name, wrapper.params),
                        in("categoryId", filter.categoryIds, wrapper.params),
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

        if (filter.limit != null && filter.offset != null) {
            builder.append(" LIMIT ? OFFSET ?");
            wrapper.params.add(filter.limit);
            wrapper.params.add(filter.offset);
        } else if (filter.limit != null) {
            builder.append(" LIMIT ?");
            wrapper.params.add(filter.limit);
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

    private String in(String columnName, List<?> values, List<Object> params) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        params.addAll(values);
        return columnName + " IN("
                + values.stream()
                .map(i -> "?")
                .collect(Collectors.joining(",")) + ")";
    }
}
