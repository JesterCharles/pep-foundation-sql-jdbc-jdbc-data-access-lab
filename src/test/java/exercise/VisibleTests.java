package exercise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Trainer-authored visible tests, one per task. Restored over the learner's checkout at grade time.
 *
 * Every test gets its own new in-memory database (H2 in PostgreSQL compatibility mode) holding the
 * shop's tables and seed rows from src/test/resources. Your code reaches it with an ordinary
 * jdbc:postgresql://localhost:5432/jdbc_lab_... URL: the LabDatabase helper below sends those URLs
 * to the test's database, so no PostgreSQL server is needed.
 */
class VisibleTests {
    private Connection db;

    @BeforeEach
    void freshDatabase() throws Exception {
        db = LabDatabase.create();
    }

    @AfterEach
    void dropDatabase() throws Exception {
        LabDatabase.drop(db);
    }

    private JdbcCustomerRepository customers() {
        return new JdbcCustomerRepository(LabDatabase.url(), LabDatabase.USER, LabDatabase.PASSWORD);
    }

    private AccountRepository accounts() {
        return new AccountRepository(LabDatabase.url(), LabDatabase.USER, LabDatabase.PASSWORD);
    }

    private List<List<String>> rows(String sql) throws SQLException {
        return LabDatabase.rows(db, sql);
    }

    private static List<String> row(String... values) {
        return List.of(values);
    }

    private void noOpenConnections(String task) throws Exception {
        long open = 0;
        for (int attempt = 0; attempt < 30; attempt++) {
            open = LabDatabase.openRepositoryConnections(db);
            if (open == 0) {
                return;
            }
            Thread.sleep(100);
        }
        fail(task + ": the repository left " + open + " connection(s) open. Open the Connection, the PreparedStatement and "
                + "the ResultSet in try-with-resources so each is closed, even when a statement fails.");
    }

    /** This test's database settings, as the service would read them from its environment. */
    private static Map<String, String> labEnvironment() {
        Map<String, String> env = new HashMap<>();
        env.put("PGHOST", "localhost");
        env.put("PGPORT", "5432");
        env.put("PGDATABASE", LabDatabase.name());
        env.put("PGUSER", LabDatabase.USER);
        env.put("PGPASSWORD", LabDatabase.PASSWORD);
        return env;
    }

    @Test
    void task1UrlFromEnvironment() {
        String sample;
        try {
            sample = Shop.jdbcUrl(Map.of("PGHOST", "db.shop.test", "PGPORT", "6543", "PGDATABASE", "shop"));
        } catch (UnsupportedOperationException e) {
            fail("Task 1: " + e.getMessage() + ". Write Shop.jdbcUrl (lab instructions, Task 1).");
            return;
        }
        assertEquals("jdbc:postgresql://db.shop.test:6543/shop", sample,
                "Task 1: for PGHOST=db.shop.test, PGPORT=6543 and PGDATABASE=shop the URL must be "
                        + "jdbc:postgresql://db.shop.test:6543/shop: the jdbc:postgresql: prefix, // before the host, "
                        + "a colon before the port and a slash before the database.");
    }

    @Test
    void task2FindById() throws Exception {
        JdbcCustomerRepository repo = customers();
        Optional<Customer> chidi;
        try {
            chidi = repo.findById(3);
        } catch (UnsupportedOperationException e) {
            fail("Task 2: " + e.getMessage() + ". Write JdbcCustomerRepository.findById (lab instructions, Task 2).");
            return;
        }
        assertEquals(Optional.of(new Customer(3, "Chidi", "Leeds")), chidi,
                "Task 2: findById(3) should return Customer[id=3, name=Chidi, city=Leeds], read with rs.next() "
                        + "and the columns id, name and city.");
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
        } catch (UnsupportedOperationException e) {
            fail("Task 4: " + e.getMessage() + ". Write JdbcCustomerRepository.save (lab instructions, Task 4).");
            return;
        } catch (RuntimeException e) {
            fail("Task 4: save failed: " + e + (e.getCause() == null ? "" : ", caused by " + e.getCause().getMessage()));
            return;
        }
        assertEquals(List.of(row("5", "Eli", "York")),
                rows("SELECT id, name, city FROM customers WHERE id = 5"),
                "Task 4: the stored row does not match the record. Bind id, name and city to the placeholders in the "
                        + "order of the INSERT's column list.");
        noOpenConnections("Task 4");
    }

    private List<List<String>> balances() throws SQLException {
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
            fail("Task 7: " + e.getMessage() + ". Tasks 1 and 4 come first (lab instructions, Task 7).");
            return;
        } catch (RuntimeException e) {
            fail("Task 7: saving through Shop.customers failed: " + e
                    + (e.getCause() == null ? "" : ", caused by " + e.getCause().getMessage())
                    + ". Check that jdbcUrl(env) uses the values in env (Task 1) and that save works (Task 4).");
            return;
        }
        assertEquals(List.of(row("7", "Gus", "Leeds")), rows("SELECT id, name, city FROM customers WHERE id = 7"),
                "Task 7: the customer saved through Shop.customers is not a row in the database: it is still the "
                        + roster.getClass().getSimpleName() + ". Change only the line that constructs the repository, so it "
                        + "returns a JdbcCustomerRepository built from jdbcUrl(env), env.get(\"PGUSER\") and env.get(\"PGPASSWORD\").");
    }

    /**
     * One throwaway database per test: H2 in memory, in PostgreSQL compatibility mode. A small JDBC driver,
     * registered once, sends jdbc:postgresql://host:port/NAME URLs to the in-memory database NAME, so your
     * code uses the same URLs it would use against a real PostgreSQL server.
     */
    static final class LabDatabase {
        static final String USER = "lab";
        static final String PASSWORD = "lab";
        private static final String OPTIONS = ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
        private static final SecureRandom RANDOM = new SecureRandom();
        private static String name;

        static {
            try {
                DriverManager.registerDriver(new PostgresUrlToH2());
            } catch (SQLException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private LabDatabase() {
        }

        static String name() {
            return name;
        }

        /** The jdbc:postgresql URL of this test's database. */
        static String url() {
            return "jdbc:postgresql://localhost:5432/" + name;
        }

        private static Connection open(String database) throws SQLException {
            return DriverManager.getConnection("jdbc:h2:mem:" + database + OPTIONS, USER, PASSWORD);
        }

        /** A new database with the shop's tables and seed rows, and the tests' own connection to it. */
        static Connection create() throws SQLException, IOException {
            byte[] bytes = new byte[6];
            RANDOM.nextBytes(bytes);
            name = "jdbc_lab_" + HexFormat.of().formatHex(bytes);
            Connection c = open(name);
            try (Statement s = c.createStatement()) {
                s.execute(resource("schema.sql"));
                s.execute(resource("shop-data.sql"));
            }
            return c;
        }

        /** Closes the database, including any connection the repository left open. */
        static void drop(Connection c) throws SQLException {
            if (c != null) {
                try (Statement s = c.createStatement()) {
                    s.execute("SHUTDOWN");
                }
                c.close();
            }
        }

        private static String resource(String file) throws IOException {
            try (InputStream in = LabDatabase.class.getResourceAsStream("/" + file)) {
                if (in == null) {
                    throw new IOException(file + " is missing from src/test/resources");
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        /** Connections open on this database other than the tests' own. */
        static long openRepositoryConnections(Connection c) throws SQLException {
            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery("SELECT count(*) FROM information_schema.sessions")) {
                r.next();
                return r.getLong(1) - 1;
            }
        }

        /** Runs one SELECT and returns its rows as text, with NULL written as "NULL". */
        static List<List<String>> rows(Connection c, String sql) throws SQLException {
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
                ResultSetMetaData meta = r.getMetaData();
                List<List<String>> out = new ArrayList<>();
                while (r.next()) {
                    List<String> row = new ArrayList<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        Object value = r.getObject(i);
                        if (value == null) {
                            row.add("NULL");
                        } else if (value instanceof BigDecimal d) {
                            row.add(d.signum() == 0 ? "0" : d.stripTrailingZeros().toPlainString());
                        } else {
                            row.add(value.toString());
                        }
                    }
                    out.add(row);
                }
                return out;
            }
        }

        /** Accepts jdbc:postgresql://host:port/NAME and connects to the in-memory database NAME. */
        static final class PostgresUrlToH2 implements Driver {
            private static final String PREFIX = "jdbc:postgresql://";

            @Override
            public Connection connect(String url, Properties info) throws SQLException {
                if (!acceptsURL(url)) {
                    return null;
                }
                String rest = url.substring(PREFIX.length());
                int query = rest.indexOf('?');
                if (query >= 0) {
                    rest = rest.substring(0, query);
                }
                int slash = rest.indexOf('/');
                String database = slash < 0 ? "" : rest.substring(slash + 1);
                if (!database.matches("[A-Za-z0-9_]+")) {
                    throw new SQLException("database \"" + database + "\" does not exist (from the URL " + url + ")");
                }
                return open(database + ";IFEXISTS=TRUE");
            }

            @Override
            public boolean acceptsURL(String url) {
                return url != null && url.startsWith(PREFIX);
            }

            @Override
            public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
                return new DriverPropertyInfo[0];
            }

            @Override
            public int getMajorVersion() {
                return 1;
            }

            @Override
            public int getMinorVersion() {
                return 0;
            }

            @Override
            public boolean jdbcCompliant() {
                return false;
            }

            @Override
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                throw new SQLFeatureNotSupportedException();
            }
        }
    }
}
