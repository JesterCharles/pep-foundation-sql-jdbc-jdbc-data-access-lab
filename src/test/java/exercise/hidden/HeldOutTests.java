package exercise.hidden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import exercise.Customer;
import exercise.JdbcCustomerRepository;
import exercise.Shop;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Held-out tests: graded on submit, never shown to the learner. Edge cases for Tasks 1, 2 and 4. */
class HeldOutTests {
    private static final String USER = "lab";
    private static final String PASSWORD = "lab";

    private String url;
    private Connection db;

    @BeforeEach
    void freshDatabase() throws Exception {
        url = "jdbc:h2:mem:held_out_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
        db = DriverManager.getConnection(url, USER, PASSWORD);
        try (Statement s = db.createStatement()) {
            s.execute(resource("schema.sql"));
            s.execute(resource("shop-data.sql"));
        }
    }

    @AfterEach
    void dropDatabase() throws Exception {
        try (Statement s = db.createStatement()) {
            s.execute("SHUTDOWN");
        }
        db.close();
    }

    private static String resource(String file) throws IOException {
        try (InputStream in = HeldOutTests.class.getResourceAsStream("/" + file)) {
            if (in == null) {
                throw new IOException(file + " is missing from src/test/resources");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private List<List<String>> rows(String sql) throws SQLException {
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery(sql)) {
            ResultSetMetaData meta = r.getMetaData();
            List<List<String>> out = new ArrayList<>();
            while (r.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    Object value = r.getObject(i);
                    row.add(value == null ? "NULL" : value.toString());
                }
                out.add(row);
            }
            return out;
        }
    }

    private JdbcCustomerRepository customers() {
        return new JdbcCustomerRepository(url, USER, PASSWORD);
    }

    @Test
    void task1UrlUsesTheEnvironment() {
        String url;
        try {
            url = Shop.jdbcUrl(Map.of("PGHOST", "localhost", "PGPORT", "5432", "PGDATABASE", "jdbc_lab_3f9c0a1b7d2e"));
        } catch (UnsupportedOperationException e) {
            fail("Task 1: " + e.getMessage() + ". Write Shop.jdbcUrl (lab README, Task 1).");
            return;
        }
        assertEquals("jdbc:postgresql://localhost:5432/jdbc_lab_3f9c0a1b7d2e", url,
                "Task 1: the URL must be built from the PGHOST, PGPORT and PGDATABASE values in env, never fixed values.");
    }

    @Test
    void task2FindByIdEdgeCases() {
        JdbcCustomerRepository repo = customers();
        try {
            assertEquals(Optional.of(new Customer(4, "Dana", null)), repo.findById(4),
                    "Task 2: findById(4) should return Dana with city null: getString returns null for a NULL column.");
            assertEquals(Optional.empty(), repo.findById(99),
                    "Task 2: findById(99) should return Optional.empty(): when rs.next() is false there is no row to map.");
        } catch (UnsupportedOperationException e) {
            fail("Task 2: " + e.getMessage() + ". Write JdbcCustomerRepository.findById (lab README, Task 2).");
        }
    }

    @Test
    void task4SaveEdgeCases() throws Exception {
        JdbcCustomerRepository repo = customers();
        try {
            repo.save(new Customer(6, "Fen", null));
        } catch (UnsupportedOperationException e) {
            fail("Task 4: " + e.getMessage() + ". Write JdbcCustomerRepository.save (lab README, Task 4).");
            return;
        } catch (RuntimeException e) {
            fail("Task 4: save failed: " + e + (e.getCause() == null ? "" : ", caused by " + e.getCause().getMessage()));
            return;
        }
        assertEquals(List.of(List.of("6", "Fen", "NULL")), rows("SELECT id, name, city FROM customers WHERE id = 6"),
                "Task 4: a null city must store NULL.");
        RuntimeException duplicate = null;
        try {
            repo.save(new Customer(1, "Amara again", "Hull"));
        } catch (RuntimeException e) {
            duplicate = e;
        }
        assertTrue(duplicate != null && duplicate.getCause() instanceof SQLException,
                "Task 4: id 1 already exists, so the primary key rejects the INSERT; save must throw an unchecked exception "
                        + "whose cause is the SQLException, as findAll does. It "
                        + (duplicate == null ? "returned normally." : "threw " + duplicate + " instead."));
    }
}
