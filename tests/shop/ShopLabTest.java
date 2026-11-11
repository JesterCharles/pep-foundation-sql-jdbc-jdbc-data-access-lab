package shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Public tests for the JDBC Data Access lab. Every test runs your Java code against a fresh copy of the
 * shop tables (../fixtures/schema.sql and ../fixtures/shop-data.sql) in this run's own lab database on
 * your local PostgreSQL server, created before the first test and dropped after the last one.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class ShopLabTest {
    private static Connection db;

    @BeforeAll
    static void createDatabase() throws Exception {
        db = LabDatabase.create();
    }

    @AfterAll
    static void dropDatabase() throws Exception {
        LabDatabase.drop(db);
    }

    @BeforeEach
    void freshData() throws Exception {
        LabDatabase.closeRepositoryConnections(db);
        LabDatabase.shopData(db);
    }

    private static JdbcCustomerRepository customers() throws SQLException {
        return new JdbcCustomerRepository(LabDatabase.repositoryUrl(), LabDatabase.user(), LabDatabase.password());
    }

    private static AccountRepository accounts() throws SQLException {
        return new AccountRepository(LabDatabase.repositoryUrl(), LabDatabase.user(), LabDatabase.password());
    }

    private static List<List<String>> rows(String sql) throws SQLException {
        return LabDatabase.rows(db, sql);
    }

    private static List<String> row(String... values) {
        return List.of(values);
    }

    private static void noOpenConnections(String task) throws Exception {
        String count = "0";
        for (int attempt = 0; attempt < 30; attempt++) {
            count = rows("SELECT count(*) FROM pg_stat_activity WHERE application_name = '" + LabDatabase.REPOSITORY_APP
                    + "' AND datname = current_database()").get(0).get(0);
            if (count.equals("0")) {
                return;
            }
            Thread.sleep(100);
        }
        fail(task + ": the repository left " + count + " connection(s) open. Open the Connection, the PreparedStatement and "
                + "the ResultSet in try-with-resources so each is closed, even when a statement fails.");
    }

    /** The settings of this run's lab database, as the service would read them from its environment. */
    private static Map<String, String> labEnvironment() throws SQLException {
        Map<String, String> env = new HashMap<>();
        env.put("PGHOST", LabDatabase.host());
        env.put("PGPORT", LabDatabase.env("PGPORT", "5432"));
        env.put("PGDATABASE", LabDatabase.ownedName());
        env.put("PGUSER", LabDatabase.user());
        env.put("PGPASSWORD", LabDatabase.password());
        return env;
    }

    @Test
    void task1UrlFromEnvironment() throws Exception {
        String sample;
        try {
            sample = Shop.jdbcUrl(Map.of("PGHOST", "db.shop.test", "PGPORT", "6543", "PGDATABASE", "shop"));
        } catch (UnsupportedOperationException e) {
            fail("Task 1: " + e.getMessage() + ". Write Shop.jdbcUrl (README.md, Task 1).");
            return;
        }
        assertEquals("jdbc:postgresql://db.shop.test:6543/shop", sample,
                "Task 1: for PGHOST=db.shop.test, PGPORT=6543 and PGDATABASE=shop the URL must be "
                        + "jdbc:postgresql://db.shop.test:6543/shop: the jdbc:postgresql: prefix, // before the host, "
                        + "a colon before the port and a slash before the database.");
        Map<String, String> env = labEnvironment();
        String url = Shop.jdbcUrl(env);
        try (Connection c = DriverManager.getConnection(url, env.get("PGUSER"), env.get("PGPASSWORD"));
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT current_database()")) {
            r.next();
            assertEquals(LabDatabase.ownedName(), r.getString(1),
                    "Task 1: the URL built from this run's settings connected to the wrong database.");
        } catch (SQLException e) {
            fail("Task 1: could not connect with the URL " + url + " built from this run's settings: " + e.getMessage());
        }
    }

    @Test
    void task2FindById() throws Exception {
        JdbcCustomerRepository repo = customers();
        Optional<Customer> chidi;
        try {
            chidi = repo.findById(3);
        } catch (UnsupportedOperationException e) {
            fail("Task 2: " + e.getMessage() + ". Write JdbcCustomerRepository.findById (README.md, Task 2).");
            return;
        }
        assertEquals(Optional.of(new Customer(3, "Chidi", "Leeds")), chidi,
                "Task 2: findById(3) should return Customer[id=3, name=Chidi, city=Leeds], read with rs.next() "
                        + "and the columns id, name and city.");
        assertEquals(Optional.of(new Customer(4, "Dana", null)), repo.findById(4),
                "Task 2: findById(4) should return Dana with city null: getString returns null for a NULL column.");
        assertEquals(Optional.empty(), repo.findById(99),
                "Task 2: findById(99) should return Optional.empty(): when rs.next() is false there is no row to map.");
        noOpenConnections("Task 2");
    }

    @Test
    void task3FindByCityBound() throws Exception {
        JdbcCustomerRepository repo = customers();
        assertEquals(List.of(new Customer(1, "Amara", "Leeds"), new Customer(3, "Chidi", "Leeds")), repo.findByCity("Leeds"),
                "Task 3: findByCity(\"Leeds\") should return Amara and Chidi, by id.");
        List<Customer> attack;
        try {
            attack = repo.findByCity("x' OR '1'='1");
        } catch (RuntimeException e) {
            attack = List.of();
            fail("Task 3: findByCity(\"x' OR '1'='1\") threw " + e + "; it should simply find no customer.");
        }
        assertEquals(List.of(), attack, "Task 3: a city containing a quote changed the query: findByCity(\"x' OR '1'='1\") "
                + "returned " + attack.size() + " customer(s) instead of none. Put a ? placeholder in fixed SQL text, use "
                + "prepareStatement, and bind the city with setString(1, city) instead of joining it into the text.");
        noOpenConnections("Task 3");
    }

    @Test
    void task4Save() throws Exception {
        JdbcCustomerRepository repo = customers();
        try {
            repo.save(new Customer(5, "Eli", "York"));
            repo.save(new Customer(6, "Fen", null));
        } catch (UnsupportedOperationException e) {
            fail("Task 4: " + e.getMessage() + ". Write JdbcCustomerRepository.save (README.md, Task 4).");
            return;
        } catch (RuntimeException e) {
            fail("Task 4: save failed: " + e + (e.getCause() == null ? "" : ", caused by " + e.getCause().getMessage()));
            return;
        }
        assertEquals(List.of(row("5", "Eli", "York"), row("6", "Fen", "NULL")),
                rows("SELECT id, name, city FROM customers WHERE id IN (5, 6) ORDER BY id"),
                "Task 4: the stored rows do not match the records. Bind id, name and city to the placeholders in the "
                        + "order of the INSERT's column list; a null city stores NULL.");
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
        noOpenConnections("Task 4");
    }

    private static List<List<String>> balances() throws SQLException {
        return rows("SELECT id, balance FROM accounts ORDER BY id");
    }

    @Test
    void task5TransferIsOneTransaction() throws Exception {
        AccountRepository repo = accounts();
        try {
            repo.transfer(1, 2, new BigDecimal("50.00"));
        } catch (SQLException e) {
            fail("Task 5: transfer(1, 2, 50.00) should succeed but threw: " + e.getMessage());
        }
        assertEquals(List.of(row("1", "150"), row("2", "100")), balances(),
                "Task 5: after transfer(1, 2, 50.00) Amara should have 150 and Bo 100. With auto-commit off, nothing is "
                        + "kept until conn.commit() runs after both UPDATEs.");
        boolean threw = false;
        try {
            repo.transfer(2, 1, new BigDecimal("500.00"));
        } catch (SQLException e) {
            threw = true;
        }
        assertTrue(threw, "Task 5: transfer(2, 1, 500.00) would take Bo's balance below 0, which the CHECK rejects; "
                + "transfer must throw SQLException to the caller.");
        assertEquals(List.of(row("1", "150"), row("2", "100")), balances(),
                "Task 5: the failed transfer of 500.00 still credited Amara: the credit was committed on its own before the "
                        + "debit failed. Turn off auto-commit, commit only after both UPDATEs succeed, and roll back in a "
                        + "catch before rethrowing.");
        noOpenConnections("Task 5");
    }

    @Test
    void task6ReadTheFailingTest() throws Exception {
        List<Customer> all = customers().findAll();
        List<Customer> expected = List.of(new Customer(1, "Amara", "Leeds"), new Customer(2, "Bo", "York"),
                new Customer(3, "Chidi", "Leeds"), new Customer(4, "Dana", null));
        assertEquals(expected, all, "Task 6: findAll does not return the shop's customers. Compare the expected and actual "
                + "values below field by field to find the bug in JdbcCustomerRepository.findAll.");
        noOpenConnections("Task 6");
    }

    @Test
    void task7SwapTheStorage() throws Exception {
        CustomerRepository roster;
        try {
            roster = Shop.customers(labEnvironment());
            roster.save(new Customer(7, "Gus", "Leeds"));
        } catch (UnsupportedOperationException e) {
            fail("Task 7: " + e.getMessage() + ". Tasks 1 and 4 come first (README.md, Task 7).");
            return;
        }
        assertEquals(List.of(row("7", "Gus", "Leeds")), rows("SELECT id, name, city FROM customers WHERE id = 7"),
                "Task 7: the customer saved through Shop.customers is not a row in PostgreSQL: it is still the "
                        + roster.getClass().getSimpleName() + ". Change only the line that constructs the repository, so it "
                        + "returns a JdbcCustomerRepository built from jdbcUrl(env), env.get(\"PGUSER\") and env.get(\"PGPASSWORD\").");
    }
}
