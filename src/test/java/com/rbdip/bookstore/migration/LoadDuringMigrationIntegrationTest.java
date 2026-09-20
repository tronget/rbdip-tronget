package com.rbdip.bookstore.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoadDuringMigrationIntegrationTest {

    private static final int WORKER_COUNT = 4;
    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(2);

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookstore_http_migration")
            .withUsername("bookstore")
            .withPassword("bookstore");

    static {
        postgres.start();
    }

    @LocalServerPort
    private int port;

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.target", () -> "3");
    }

    @Test
    void servesHttpRequestsWithoutServerErrorsOrTimeoutsDuringValidationMigration() throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
        long customerId = createOrderAndFindCustomerId(client);
        AtomicBoolean keepSending = new AtomicBoolean(true);
        AtomicBoolean migrationInProgress = new AtomicBoolean(false);
        AtomicInteger requestsDuringMigration = new AtomicInteger();
        CountDownLatch initialResponses = new CountDownLatch(WORKER_COUNT);
        List<String> errors = new CopyOnWriteArrayList<>();
        ExecutorService workers = Executors.newFixedThreadPool(WORKER_COUNT);
        ExecutorService migrationExecutor = Executors.newSingleThreadExecutor();
        try (Connection migrationGate = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            migrationGate.setAutoCommit(false);
            lockCustomerRow(migrationGate, customerId);
            migrationInProgress.set(true);
            Future<Integer> migration = migrationExecutor.submit(this::migrateToVersionFour);
            assertThat(awaitMigrationBlocked()).isTrue();

            for (int worker = 0; worker < WORKER_COUNT; worker++) {
                workers.submit(() -> sendRequests(
                        client, keepSending, migrationInProgress, requestsDuringMigration, initialResponses, errors));
            }
            assertThat(initialResponses.await(HTTP_TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isTrue();

            migrationGate.commit();
            assertThat(migration.get(HTTP_TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isEqualTo(1);
        } finally {
            migrationInProgress.set(false);
            keepSending.set(false);
            workers.shutdown();
            migrationExecutor.shutdown();
        }

        assertThat(workers.awaitTermination(HTTP_TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isTrue();
        assertThat(migrationExecutor.awaitTermination(HTTP_TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isTrue();
        assertThat(requestsDuringMigration.get()).isPositive();
        assertThat(errors).as("0 HTTP errors, 5xx responses and timeouts are allowed during migration").isEmpty();
    }

    private int migrateToVersionFour() {
        return Flyway.configure()
                .dataSource(databaseUrl, databaseUsername, databasePassword)
                .locations("classpath:db/migration")
                .target("4")
                .load()
                .migrate()
                .migrationsExecuted;
    }

    private long createOrderAndFindCustomerId(HttpClient client) throws Exception {
        long productId = postJson(client, "/products", "{\"name\":\"Migration book\",\"price\":10.00}");
        long orderId = postJson(
                client,
                "/orders",
                """
                        {"customerFullName":"Ivan Petrov","customerAddress":"Moscow, Lenina 1",
                        "customerPhone":"+79990000000","customerType":"regular",
                        "items":[{"productId":%d,"quantity":1}]}
                        """.formatted(productId));
        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT customer_id FROM orders WHERE id = " + orderId)) {
            result.next();
            return result.getLong(1);
        }
    }

    private long postJson(HttpClient client, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(HTTP_CREATED);
        JsonNode responseBody = objectMapper.readTree(response.body());
        return responseBody.get("id").asLong();
    }

    private void lockCustomerRow(Connection connection, long customerId) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SELECT id FROM customers WHERE id = " + customerId + " FOR UPDATE");
        }
    }

    private boolean awaitMigrationBlocked() throws SQLException, InterruptedException {
        for (int attempt = 0; attempt < 20; attempt++) {
            try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
                    Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT COUNT(*) FROM pg_stat_activity "
                                    + "WHERE datname = current_database() "
                                    + "AND query LIKE 'UPDATE customers%' "
                                    + "AND wait_event_type = 'Lock'")) {
                result.next();
                if (result.getInt(1) > 0) {
                    return true;
                }
            }
            Thread.sleep(50);
        }
        return false;
    }

    private void sendRequests(
            HttpClient client,
            AtomicBoolean keepSending,
            AtomicBoolean migrationInProgress,
            AtomicInteger requestsDuringMigration,
            CountDownLatch initialResponses,
            List<String> errors) {
        boolean firstResponse = true;
        while (keepSending.get()) {
            try {
                if (migrationInProgress.get()) {
                    requestsDuringMigration.incrementAndGet();
                }
                HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/orders"))
                        .timeout(HTTP_TIMEOUT)
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != HTTP_OK) {
                    errors.add("HTTP " + response.statusCode());
                } else if (!response.body().contains("\"customerFullName\":\"Ivan Petrov\"")) {
                    errors.add("order response does not use the split customer name");
                }
            } catch (Exception exception) {
                errors.add(exception.getClass().getSimpleName() + ": " + exception.getMessage());
            } finally {
                if (firstResponse) {
                    initialResponses.countDown();
                    firstResponse = false;
                }
            }
        }
    }
}
