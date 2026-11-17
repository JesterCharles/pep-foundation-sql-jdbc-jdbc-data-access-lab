package shop;

/** One row of the customers table. city may be null. */
public record Customer(long id, String name, String city) {
}
