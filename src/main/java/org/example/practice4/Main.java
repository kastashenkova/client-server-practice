package org.example.practice4;

import java.math.BigDecimal;

public class Main {

    public static void main(String[] args) {
        String jdbcUrl = "jdbc:mysql://localhost:3306/my_db";
        String user = "root";
        String password = "root";

        try (MySqlDatabaseImpl db = new MySqlDatabaseImpl(jdbcUrl, user, password)) {
            // (Database db = new SqlLiteDatabaseImpl("products.db"));

            System.out.println("Number of products: " + db.count());

            db.create(new Product("p1", "c1", 1, BigDecimal.ONE));
            db.create(new Product("p2", "c2", 2 , BigDecimal.TWO));
            int p3 = db.create(new Product("p3", "c3", 3, BigDecimal.valueOf(3)));

            System.out.println("Number of products: " + db.count());

            Filter filter = new Filter();
            filter.name = "p1";
            filter.category = "c1";
            filter.maxQuantity = 2;
            filter.minPrice = BigDecimal.ONE;

            System.out.println("All products: " + db.getAll(filter));
            System.out.println("Product by id: " + db.getById(p3));
            System.out.println("Product by unknown id: " + db.getById(1000000000));

        } catch (Exception e) {
            throw new RuntimeException("Error connecting to MySQL database: " + jdbcUrl, e);
        }
    }
}
