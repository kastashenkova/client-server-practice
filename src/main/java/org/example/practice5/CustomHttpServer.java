package org.example.practice5;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpServer;
import org.example.practice4.Database;
import org.example.practice4.Product;
import org.example.practice4.SqlLiteDatabaseImpl;

public class CustomHttpServer {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String SECRET_KEY = System.getenv("AUTH_SECRET_KEY") != null
            ? System.getenv("AUTH_SECRET_KEY")
            : "secretDefaultKey";

    private final Database database;
    private final HttpServer server;

    public CustomHttpServer(int port, Database database) throws IOException {
        this.database = database;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        createEndpoints();
    }

    public void start() {
        server.start();
        System.out.println("HTTP Server started on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        System.out.println("HTTP Server stopped.");
    }

    private void createEndpoints() {
        // authorization
        server.createContext("/login", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    LoginDto loginDto = mapper.readValue(exchange.getRequestBody(), LoginDto.class);

                    if (database instanceof SqlLiteDatabaseImpl db) {
                        var userOpt = db.getUserByUsername(loginDto.username());
                        if (userOpt.isPresent()
                                && userOpt.get().password().equals(loginDto.password())) {
                            String role = userOpt.get().role();

                            String token = createJwt(loginDto.username(), role);
                            String jsonResponse = mapper.writeValueAsString(new TokenResponse(token));

                            sendResponse(exchange, 200, jsonResponse);
                        }
                    }
                    sendResponse(exchange, 401, "{\"error\": \"Unauthorized: Invalid credentials\"}");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\": \"Bad request format\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
            }
        });

        // protected endpoints
        HttpContext productsContext = server.createContext("/products", exchange -> {
            try {
                String method = exchange.getRequestMethod();
                String userRole = (String) exchange.getAttribute("USER_ROLE");

                if (!"GET".equalsIgnoreCase(method) && !"ROLE_ADMIN".equals(userRole)) {
                    sendResponse(exchange, 403, "{\"error\": \"Forbidden: Admin role required\"}");
                    return;
                }

                String path = exchange.getRequestURI().getPath();
                String[] parts = path.split("/");

                if ("PUT".equalsIgnoreCase(method)) {
                    Product product = mapper.readValue(exchange.getRequestBody(), Product.class);
                    if (database.getProductByName(product.getName()).isPresent()) {
                        sendResponse(exchange, 409, "{\"error\": \"Product with this name already exists\"}");
                    } else if (database.getCategoryById(product.getCategoryId()).isEmpty()) {
                        sendResponse(exchange, 400, "{\"error\": \"Bad Request: Category not found\"}");
                    } else {
                        database.create(product);
                        sendResponse(exchange, 201, "{\"message\": \"Created\"}");
                    }
                } else if (parts.length == 3) {
                    int id = Integer.parseInt(parts[2]);
                    if ("GET".equalsIgnoreCase(method)) {
                        Optional<Product> product = database.getById(id);
                        if (product.isPresent()) {
                            sendResponse(exchange, 200, mapper.writeValueAsString(product.get()));
                        } else {
                            sendResponse(exchange, 404, "{\"error\": \"Not found\"}");
                        }
                    } else if ("POST".equalsIgnoreCase(method)) {
                        Product product = mapper.readValue(exchange.getRequestBody(), Product.class);
                        try {
                            database.update(new Product(id,
                                    product.getName(),
                                    product.getCategoryId(),
                                    product.getQuantity(),
                                    product.getPrice()));
                            sendResponse(exchange, 200, "{\"message\": \"Updated\"}");
                        } catch (RuntimeException ex) {
                            sendResponse(exchange, 404, "{\"error\": \"Product not found\"}");
                        }
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        try {
                            database.deleteById(id);
                            sendResponse(exchange, 204, "");
                        } catch (RuntimeException ex) {
                            sendResponse(exchange, 404, "{\"error\": \"Product not found\"}");
                        }
                    } else {
                        sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                    }
                } else {
                    sendResponse(exchange, 400, "{\"error\": \"Bad request format\"}");
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\": \"Internal Server Error\"}");
            }
        });

        // fallback
        server.createContext("/", exchange -> sendResponse(exchange, 404, ""));

        productsContext.setAuthenticator(new JwtAuthenticator());
    }

    private static void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
        exchange.close();
    }

    private static String createJwt(String username, String role) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
            return JWT.create()
                    .withSubject(username)
                    .withClaim("role", role)
                    .withExpiresAt(Instant.now().plusSeconds(3600)) // 1 hour
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new RuntimeException("Unable to create JWT", e);
        }
    }

    private static class JwtAuthenticator extends Authenticator {
        @Override
        public Result authenticate(HttpExchange exchange) {
            List<String> values = exchange.getRequestHeaders().get("Authorization");

            if (values == null || values.isEmpty()) {
                return new Failure(401);
            }

            String[] credentialParts = values.getFirst().split(" ");
            if (credentialParts.length != 2 || !"Bearer".equalsIgnoreCase(credentialParts[0])) {
                return new Failure(401);
            }

            try {
                DecodedJWT decodedJWT = decodeJWT(credentialParts[1]);
                String username = decodedJWT.getSubject();
                String role = decodedJWT.getClaim("role").asString();
                exchange.setAttribute("USER_ROLE", role);
                return new Success(new HttpPrincipal(username, role));

            } catch (JWTVerificationException e) {
                return new Failure(401);
            }
        }

        private static DecodedJWT decodeJWT(String token) {
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        }
    }
}
