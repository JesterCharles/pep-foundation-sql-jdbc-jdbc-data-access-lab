package shop;

import java.util.List;
import java.util.Optional;

/** The only way the shop reaches its customers. Callers never know which implementation they hold. */
public interface CustomerRepository {
    Optional<Customer> findById(long id);

    List<Customer> findByCity(String city);

    List<Customer> findAll();

    void save(Customer customer);
}
