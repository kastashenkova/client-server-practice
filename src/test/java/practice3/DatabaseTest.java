package practice3;

import org.example.practice4.Database;
import org.example.practice4.DatabaseImpl;
import org.example.practice4.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseTest {

    private Database database;

    @BeforeEach
    void setup() {
        database = new DatabaseImpl();

        database.create(new Product("product1", "category1", 1, BigDecimal.ONE));
        database.create(new Product("product2", "category2", 2, BigDecimal.TWO));
        database.create(new Product("product3", "category3", 3, BigDecimal.TEN));
    }

    @AfterEach
    void cleanUp() {
        int number = database.deleteAll();
        System.out.printf("Removed %s products", number);
    }

//    @Test
//    void shouldIncreaseCountAfterInsert() {
//        int countBefore = mySqlDb.count();
//
//        mySqlDb.insert(new Student("test4", "test4", 4));
//
//        assertThat(mySqlDb.count())
//                .isEqualTo(countBefore + 1);
//    }
//
//    @Test
//    void shouldGetStudentById() {
//        int id = mySqlDb.insert(new Student("test4", "test4", 4));
//
//        assertThat(mySqlDb.getById(id))
//                .isPresent()
//                .get()
//                .isEqualTo(new Student(id, "test4", "test4", 4));
//    }
//

}
