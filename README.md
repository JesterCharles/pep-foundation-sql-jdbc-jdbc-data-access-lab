# JDBC Data Access lab: move the shop's customers into PostgreSQL

The shop keeps its customers in memory, so they vanish whenever the service stops, and its store-credit transfer can leave one account credited when the other cannot pay.
You give the service a real JDBC layer, in seven tasks in the package `shop`: a database URL built from the environment, a `JdbcCustomerRepository` behind the existing `CustomerRepository` interface, a transfer that is one transaction, and a one-line swap from in-memory storage to PostgreSQL.

You practise connections, PreparedStatement and bound parameters, ResultSet mapping, try-with-resources and transactions.

The full learner instructions are in [`.evolv/instructions.md`](.evolv/instructions.md), which the lab platform shows as the lab's instructions.

## Layout

This repository follows the Exa coding-lab scaffold for Java.

| Path | What it is |
|---|---|
| `src/main/java/shop/Shop.java` | Lab source |
| `src/main/java/shop/JdbcCustomerRepository.java` | Lab source |
| `src/main/java/shop/AccountRepository.java` | Lab source |
| `src/main/java/shop/Customer.java`, `CustomerRepository.java`, `InMemoryCustomerRepository.java` | Supplied source, not edited |
| `src/test/java/shop/ShopLabTest.java` | Visible tests |
| `src/test/java/shop/LabDatabase.java` | Test support: creates and drops the run's own lab database |
| `fixtures/schema.sql`, `fixtures/shop-data.sql` | The shop tables and seed rows each test starts from |
| `.evolv/instructions.md` | Learner instructions |
| `.evolv/grade.yml` | Grading manifest, used by Run Tests and by the grader |
| `pom.xml` | Maven build: Java 17 and JUnit 5.11.3, matching the scaffold, plus the PostgreSQL JDBC driver |
| `Dockerfile` | Image for local Run Tests; warms the Maven cache |
| `expected/test-summary.txt` | The result of a finished lab |

There are no held-out tests in this lab: every test is visible.

## PostgreSQL prerequisite

The tests need a running PostgreSQL server (16 or later) that they reach over TCP.
They read `PGHOST`, `PGPORT`, `PGUSER` and `PGDATABASE` (defaults `localhost`, `5432`, the login user, `postgres`) and a password file or `PGPASSWORD`.
The user must be allowed to create databases: each run creates its own uniquely named database and drops it afterwards.
Without a reachable server every test fails with `Could not reach PostgreSQL to create the lab database`.

## Run the tests

On the platform, select **Run Tests**. From a terminal with JDK 17 or later, Apache Maven 3.9 and a reachable PostgreSQL server:

```
mvn -q test
```

All 7 tests fail until the `TODO`s are done. That is expected.
