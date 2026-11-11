package shop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The same CustomerRepository, stored in the customers table of PostgreSQL through JDBC.
 * The tests create it with the address of their own lab database on your local server.
 */
public class JdbcCustomerRepository implements CustomerRepository {
    private final String url;
    private final String user;
    private final String password;

    /** password may be null: the PostgreSQL driver then reads your password file. */
    public JdbcCustomerRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Task 2: the customer with this id, or Optional.empty() when there is none. */
    @Override
    public Optional<Customer> findById(long id) {
        // TODO Task 2: SELECT id, name, city FROM customers WHERE id = ?, bind id with setLong,
        // move the cursor with rs.next() and map that row into a new Customer, closing all three
        // resources with try-with-resources. Wrap an SQLException as findAll's catch block does
        // (copy only that catch: findAll's row mapping is the Task 6 bug).
        throw new UnsupportedOperationException("findById is not written yet");
    }

    /** Task 3: every customer in this city, by id. */
    @Override
    public List<Customer> findByCity(String city) {
        // The previous developer's version. Task 3: it joins the value into the SQL text; fix it.
        String sql = "SELECT id, name, city FROM customers WHERE city = '" + city + "' ORDER BY id";
        try (Connection conn = connect(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<Customer> found = new ArrayList<>();
            while (rs.next()) {
                found.add(new Customer(rs.getLong("id"), rs.getString("name"), rs.getString("city")));
            }
            return found;
        } catch (SQLException e) {
            throw new IllegalStateException("could not read the customers of " + city, e);
        }
    }

    /** Task 6: every customer, by id. A failing test says what is wrong here. */
    @Override
    public List<Customer> findAll() {
        String sql = "SELECT id, name, city FROM customers ORDER BY id";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Customer> found = new ArrayList<>();
            while (rs.next()) {
                found.add(new Customer(rs.getLong("id"), rs.getString("city"), rs.getString("name")));
            }
            return found;
        } catch (SQLException e) {
            // The interface declares no SQLException, so it travels on inside an unchecked exception.
            throw new IllegalStateException("could not read the customers", e);
        }
    }

    /** Task 4: stores one customer (id, name and city, which may be null). */
    @Override
    public void save(Customer customer) {
        // TODO Task 4: INSERT INTO customers (id, name, city) VALUES (?, ?, ?), bind the record's three
        // fields in that order, run executeUpdate, and wrap an SQLException as findAll does.
        throw new UnsupportedOperationException("save is not written yet");
    }
}
