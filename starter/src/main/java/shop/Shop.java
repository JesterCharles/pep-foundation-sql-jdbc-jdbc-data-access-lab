package shop;

import java.util.Map;

/** How the shop service is wired. The service passes System.getenv() as env. */
public final class Shop {
    private Shop() {
    }

    /** Task 1: the jdbc:postgresql URL for PGHOST, PGPORT and PGDATABASE in env. */
    public static String jdbcUrl(Map<String, String> env) {
        // TODO Task 1: build jdbc:postgresql://<host>:<port>/<database> from env, never hard-coded values.
        throw new UnsupportedOperationException("jdbcUrl is not written yet");
    }

    /** Task 7: the customer storage the service uses. */
    public static CustomerRepository customers(Map<String, String> env) {
        return new InMemoryCustomerRepository();
    }
}
