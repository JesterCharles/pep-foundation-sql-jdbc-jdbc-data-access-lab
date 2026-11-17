package shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** The shop's first storage: a map that forgets everything when the program stops. */
public class InMemoryCustomerRepository implements CustomerRepository {
    private final Map<Long, Customer> customers = new TreeMap<>();

    @Override
    public Optional<Customer> findById(long id) {
        return Optional.ofNullable(customers.get(id));
    }

    @Override
    public List<Customer> findByCity(String city) {
        List<Customer> found = new ArrayList<>();
        for (Customer c : customers.values()) {
            if (Objects.equals(c.city(), city)) {
                found.add(c);
            }
        }
        return found;
    }

    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(customers.values());
    }

    @Override
    public void save(Customer customer) {
        if (customers.putIfAbsent(customer.id(), customer) != null) {
            throw new IllegalStateException("customer " + customer.id() + " already exists");
        }
    }
}
