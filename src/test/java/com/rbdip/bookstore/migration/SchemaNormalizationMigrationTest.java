package com.rbdip.bookstore.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class SchemaNormalizationMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookstore_migration_test")
            .withUsername("bookstore")
            .withPassword("bookstore");

    @Test
    void migratesLegacyOrdersAndItemsToNormalizedRelations() throws SQLException {
        PGSimpleDataSource dataSource = dataSource();
        migrateToVersionOne(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            long productId = insertAndReturnId(
                    connection,
                    "INSERT INTO products (name, price) VALUES ('Existing book', 25.00)");
            long orderId = insertAndReturnId(
                    connection,
                    "INSERT INTO orders (customer_full_name, customer_address, customer_phone, status) "
                            + "VALUES ('Ivan Petrov', 'Moscow, Lenina 1', NULL, 'new')");
            executeUpdate(
                    connection,
                    "INSERT INTO order_items (order_id, product_name, product_price, quantity) "
                            + "VALUES (" + orderId + ", 'Existing book', 25.00, 2)");
            executeUpdate(
                    connection,
                    "INSERT INTO order_items (order_id, product_name, product_price, quantity) "
                            + "VALUES (" + orderId + ", 'Legacy-only book', 15.00, 1)");

            migrateToLatest(dataSource);

            assertThat(queryLong(
                    connection,
                    "SELECT customer_id FROM orders WHERE id = " + orderId))
                    .isPositive();
            assertThat(queryString(
                    connection,
                    "SELECT customer.first_name FROM orders "
                            + "JOIN customers AS customer ON customer.id = orders.customer_id "
                            + "WHERE orders.id = " + orderId))
                    .isEqualTo("Ivan");
            assertThat(queryString(
                    connection,
                    "SELECT customer.last_name FROM orders "
                            + "JOIN customers AS customer ON customer.id = orders.customer_id "
                            + "WHERE orders.id = " + orderId))
                    .isEqualTo("Petrov");
            assertThat(queryLong(
                    connection,
                    "SELECT COUNT(*) FROM order_items AS item "
                            + "JOIN products AS product ON product.id = item.product_id "
                            + "WHERE item.order_id = " + orderId + " AND product.name IN "
                            + "('Existing book', 'Legacy-only book')"))
                    .isEqualTo(2);
            assertThat(queryLong(
                    connection,
                    "SELECT product_id FROM order_items WHERE order_id = " + orderId + " "
                            + "AND product_id = " + productId))
                    .isEqualTo(productId);

            assertThat(columnExists(connection, "orders", "customer_full_name")).isFalse();
            assertThat(columnExists(connection, "orders", "customer_address")).isFalse();
            assertThat(columnExists(connection, "orders", "customer_phone")).isFalse();
            assertThat(columnExists(connection, "order_items", "product_name")).isFalse();
            assertThat(columnExists(connection, "order_items", "product_price")).isFalse();
            assertThat(columnExists(connection, "customers", "full_name")).isFalse();
            assertThat(columnExists(connection, "customers", "first_name")).isTrue();
            assertThat(columnExists(connection, "customers", "last_name")).isTrue();
            assertThat(foreignKeyCount(connection, "orders", "customer_id")).isEqualTo(1);
            assertThat(foreignKeyCount(connection, "order_items", "product_id")).isEqualTo(1);
        }
    }

    private PGSimpleDataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUser(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        return dataSource;
    }

    private void migrateToVersionOne(PGSimpleDataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("1")
                .load()
                .migrate();
    }

    private void migrateToLatest(PGSimpleDataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private long insertAndReturnId(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void executeUpdate(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private long queryLong(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private String queryString(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_name = ? AND column_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1) == 1;
            }
        }
    }

    private long foreignKeyCount(Connection connection, String tableName, String columnName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.table_constraints AS constraint "
                + "JOIN information_schema.key_column_usage AS key_usage "
                + "ON constraint.constraint_name = key_usage.constraint_name "
                + "AND constraint.table_schema = key_usage.table_schema "
                + "WHERE constraint.constraint_type = 'FOREIGN KEY' "
                + "AND constraint.table_name = ? AND key_usage.column_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }
}
