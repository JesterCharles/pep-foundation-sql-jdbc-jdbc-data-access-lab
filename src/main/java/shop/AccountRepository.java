package shop;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** The shop's store-credit accounts. A balance can never go below 0 (CHECK in fixtures/schema.sql). */
public class AccountRepository {
    private final String url;
    private final String user;
    private final String password;

    public AccountRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Supplied: the balance of one account. */
    public BigDecimal balance(long id) throws SQLException {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("SELECT balance FROM accounts WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("no account " + id);
                }
                return rs.getBigDecimal("balance");
            }
        }
    }

    /**
     * Task 5: moves amount from one account to another. Either both balances change or neither does.
     * The previous developer's version: each statement is committed on its own.
     */
    public void transfer(long from, long to, BigDecimal amount) throws SQLException {
        try (Connection conn = connect();
             PreparedStatement credit = conn.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE id = ?");
             PreparedStatement debit = conn.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE id = ?")) {
            credit.setBigDecimal(1, amount);
            credit.setLong(2, to);
            credit.executeUpdate();
            debit.setBigDecimal(1, amount);
            debit.setLong(2, from);
            debit.executeUpdate();
        }
    }
}
