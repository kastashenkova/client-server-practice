package org.example.practice5;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.example.practice4.Category;
import org.example.practice4.Product;
import org.example.practice4.SqlLiteDatabaseImpl;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CustomHttpServerTest {

    private CustomHttpServer server;
    private SqlLiteDatabaseImpl databaseMock;

    @BeforeEach
    void start() throws IOException {
        databaseMock = Mockito.mock(SqlLiteDatabaseImpl.class);
        server = new CustomHttpServer(8181, databaseMock);
        server.start();

        RestAssured.port = 8181;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterEach
    void cleanUp() {
        server.stop();
    }

    private String getAdminToken() {
        return JWT.create()
                .withSubject("admin")
                .withClaim("role", "ROLE_ADMIN")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC256("secretDefaultKey"));
    }

    private String getUserToken() {
        return JWT.create()
                .withSubject("user")
                .withClaim("role", "ROLE_USER")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC256("secretDefaultKey"));
    }

    @Test
    void shouldReturnNotFoundWhenUnknownPath() {
        RestAssured.given()
                .expect()
                .statusCode(404)
                .when()
                .get("/unknown-path");
    }

    @Test
    void shouldReturnUnauthorizedForInvalidLogin() {
        String loginPayload = "{\"username\":\"unknownUser\",\"password\":\"wrongPass\"}";

        Mockito.when(databaseMock.getUserByUsername("unknownUser")).thenReturn(Optional.empty());

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(loginPayload)
                .expect()
                .statusCode(401)
                .when()
                .post("/login");
    }

    @Test
    void shouldReturnUnauthorizedWhenNoTokenProvided() {
        RestAssured.given()
                .expect()
                .statusCode(401)
                .when()
                .get("/products/1");
    }

    @Test
    void shouldGetProductByIdWhenAuthorized() {
        Product mockProduct = new Product(1, "Laptop", 1, 10, BigDecimal.valueOf(1500.0));
        Mockito.when(databaseMock.getById(1)).thenReturn(Optional.of(mockProduct));

        RestAssured.given()
                .header("Authorization", "Bearer " + getUserToken())
                .expect()
                .statusCode(200)
                .body("name", CoreMatchers.is("Laptop"))
                .when()
                .get("/products/1");
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() {
        Mockito.when(databaseMock.getById(99)).thenReturn(Optional.empty());

        RestAssured.given()
                .header("Authorization", "Bearer " + getUserToken())
                .expect()
                .statusCode(404)
                .when()
                .get("/products/99");
    }

    @Test
    void shouldReturnForbiddenWhenCreatingProductWithoutAdminRole() {
        Product product = new Product(2, "Phone", 1, 5, BigDecimal.valueOf(800.0));

        RestAssured.given()
                .header("Authorization", "Bearer " + getUserToken())
                .contentType(ContentType.JSON)
                .body(product)
                .expect()
                .statusCode(403)
                .when()
                .put("/products");
    }

    @Test
    void shouldCreateProductWhenAdmin() {
        Product product = new Product(3, "Monitor", 2, 15, BigDecimal.valueOf(300.0));

        Mockito.when(databaseMock.getProductByName("Monitor")).thenReturn(Optional.empty());
        Mockito.when(databaseMock.getCategoryById(2)).thenReturn(Optional.of(Mockito.mock(Category.class)));

        RestAssured.given()
                .header("Authorization", "Bearer " + getAdminToken())
                .contentType(ContentType.JSON)
                .body(product)
                .expect()
                .statusCode(201)
                .when()
                .put("/products");

        Mockito.verify(databaseMock).create(Mockito.any(Product.class));
    }

    @Test
    void shouldReturnBadRequestWhenCreatingProductWithInvalidCategory() {
        Product product = new Product(4, "Tablet", 99, 5, BigDecimal.valueOf(400.0));

        Mockito.when(databaseMock.getProductByName("Tablet")).thenReturn(Optional.empty());
        Mockito.when(databaseMock.getCategoryById(99)).thenReturn(Optional.empty());

        RestAssured.given()
                .header("Authorization", "Bearer " + getAdminToken())
                .contentType(ContentType.JSON)
                .body(product)
                .expect()
                .statusCode(400)
                .when()
                .put("/products");
    }

    @Test
    void shouldDeleteProductWhenAdmin() {
        RestAssured.given()
                .header("Authorization", "Bearer " + getAdminToken())
                .expect()
                .statusCode(204)
                .when()
                .delete("/products/1");

        Mockito.verify(databaseMock).deleteById(1);
    }

    @Test
    void shouldReturnMethodNotAllowedForLoginGet() {
        RestAssured.given()
                .expect()
                .statusCode(405)
                .when()
                .get("/login");
    }

    @Test
    void shouldReturnBadRequestForInvalidLoginJson() {
        String invalidJson = "{bad json}";

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(invalidJson)
                .expect()
                .statusCode(400)
                .when()
                .post("/login");
    }

    @Test
    void shouldReturnConflictWhenProductExists() {
        Product product = new Product(1, "Laptop", 1, 10, BigDecimal.valueOf(1000));

        Mockito.when(databaseMock.getProductByName("Laptop"))
                .thenReturn(Optional.of(product));

        RestAssured.given()
                .header("Authorization", "Bearer " + getAdminToken())
                .contentType(ContentType.JSON)
                .body(product)
                .expect()
                .statusCode(409)
                .when()
                .put("/products");
    }

    @Test
    void shouldReturnUnauthorizedWhenNoAuthorizationHeader() {
        RestAssured.given()
                .expect()
                .statusCode(401)
                .when()
                .get("/products/1");
    }

    @Test
    void shouldUpdateProductWhenExists() {
        Product updated = new Product(1, "Laptop Pro", 1, 20, BigDecimal.valueOf(2000));

        RestAssured.given()
                .header("Authorization", "Bearer " + getAdminToken())
                .contentType(ContentType.JSON)
                .body(updated)
                .expect()
                .statusCode(200)
                .when()
                .post("/products/1");

        Mockito.verify(databaseMock).update(Mockito.any(Product.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingProduct() {
        Product updated = new Product(1, "Laptop Pro", 1, 20, BigDecimal.valueOf(2000));

        Mockito.doThrow(new RuntimeException())
                .when(databaseMock).update(Mockito.any(Product.class));

        RestAssured.given()
                .header("Authorization", "Bearer " + getAdminToken())
                .contentType(ContentType.JSON)
                .body(updated)
                .expect()
                .statusCode(404)
                .when()
                .post("/products/1");
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingProduct() {
        Mockito.doThrow(new RuntimeException())
                .when(databaseMock).deleteById(99);

        RestAssured.given()
                .header("Authorization", "Bearer " + getAdminToken())
                .expect()
                .statusCode(404)
                .when()
                .delete("/products/99");
    }

    @Test
    void shouldAllowUserToGetProduct() {
        Product product = new Product(1, "Laptop", 1, 10, BigDecimal.valueOf(1500));

        Mockito.when(databaseMock.getById(1)).thenReturn(Optional.of(product));

        RestAssured.given()
                .header("Authorization", "Bearer " + getUserToken())
                .expect()
                .statusCode(200)
                .when()
                .get("/products/1");
    }

    @Test
    void shouldReturnUnauthorizedForMalformedToken() {
        RestAssured.given()
                .header("Authorization", "Bearer invalid.token.here")
                .expect()
                .statusCode(401)
                .when()
                .get("/products/1");
    }

    @Test
    void shouldReturnNotFoundForRootPath() {
        RestAssured.given()
                .expect()
                .statusCode(404)
                .when()
                .get("/");
    }
}
