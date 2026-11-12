# JDBC Data Access lab: move the shop's customers into PostgreSQL

The shop keeps its customers in memory, so they vanish whenever the service stops, and its store-credit transfer can leave one account credited when the other cannot pay. You give the service a real JDBC layer: a database URL built from the environment, a `JdbcCustomerRepository` behind the existing `CustomerRepository` interface, a transfer that is one transaction, and a one-line swap from in-memory storage to PostgreSQL. Your Java code talks to a database on the PostgreSQL server the tests connect to through the PostgreSQL JDBC driver.

```
OPEN the project -> EDIT a TODO -> RUN the tests -> READ the feedback -> STOP when all pass
                        ^                                  |
                        +------------ next task -----------+
```

In words: open the lab project, edit the TODO for one task, run the tests, read what the failing test tells you, and repeat with the next task. Stop when every test passes.

**Time:** at most 40 minutes in two sittings. Start after the five JDBC Data Access pages.

| Sitting | Tasks | Estimate | Limit |
| --- | --- | --- | --- |
| 1 | Open and run, Tasks 1-2 | about 12 minutes | 15 minutes |
| 2 | [Resume](#resuming-sitting-2), Tasks 3-7 | about 20 minutes | 25 minutes |

If a sitting runs past its limit, note the next failing task and finish it at the start of the next sitting or later.

## Quick start

1. **Open** the lab project in VS Code, then open its terminal with **Terminal > New Terminal**. Go into the Maven project folder:

   ```
   cd starter
   ```

   Run every command in this README from this `starter` folder.
2. **Run** the tests:

   ```
   mvn -q -o test
   ```

   **All 7 tests fail at first. That is expected:** each failure is a task you have not done yet. After sitting 1, Tasks 3 to 7 still fail until sitting 2.

   If Maven instead says it cannot resolve a dependency, run `mvn -q dependency:go-offline` once (it needs the internet), then run the tests again.
3. **Edit** the file for one task in [Your tasks](#your-tasks).
4. **Read** the feedback: each failing test starts with its task number and names the next thing to fix. Run that task's test command again after each change.
5. **Stop** when the full run `mvn -q -o test` prints no `[ERROR]` lines.

The tests need a PostgreSQL server they can reach, JDK 25 and Maven 3.9 or later ([Prerequisites](#prerequisites)). If a command fails, see [Troubleshooting](#troubleshooting).

## Your tasks

You edit only these three files, all in `starter/src/main/java/shop/`. Each task's method has a `Task` comment, and the ones you write from scratch have a `TODO`. After each task, run its test; it is done when the run prints no `[ERROR]` lines.

**Sitting 1**

- [ ] **1. Build the URL from the environment:** `Shop.java`, method `jdbcUrl`
  `mvn -q -o test -Dtest='ShopLabTest#task1UrlFromEnvironment'`
- [ ] **2. Find one customer:** `JdbcCustomerRepository.java`, method `findById`
  `mvn -q -o test -Dtest='ShopLabTest#task2FindById'`

**Sitting 2** (start with [Resuming](#resuming-sitting-2))

- [ ] **3. Close the injection hole:** `JdbcCustomerRepository.java`, method `findByCity`
  `mvn -q -o test -Dtest='ShopLabTest#task3FindByCityBound'`
- [ ] **4. Save a customer:** `JdbcCustomerRepository.java`, method `save`
  `mvn -q -o test -Dtest='ShopLabTest#task4Save'`
- [ ] **5. Make the transfer one transaction:** `AccountRepository.java`, method `transfer`
  `mvn -q -o test -Dtest='ShopLabTest#task5TransferIsOneTransaction'`
- [ ] **6. Read the failing test:** `JdbcCustomerRepository.java`, method `findAll`
  `mvn -q -o test -Dtest='ShopLabTest#task6ReadTheFailingTest'`
- [ ] **7. Swap the storage:** `Shop.java`, method `customers`
  `mvn -q -o test -Dtest='ShopLabTest#task7SwapTheStorage'`

**Finish:** run all the tests with `mvn -q -o test`. You are done when it prints no `[ERROR]` lines and `target/surefire-reports/shop.ShopLabTest.txt` contains:

```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

`WARNING:` lines from Java or Maven, such as `WARNING: A restricted method ...`, are not test failures.

### Resuming (sitting 2)

Open the lab project in VS Code and its terminal, then:

```
cd starter
mvn -q -o test
```

If you set any [Setup](#setup) variables in sitting 1, set them again first. The first failing task should be Task 3.

## The data

`../fixtures/schema.sql` creates two tables: `customers (id, name, city)`, where `city` may be NULL, and `accounts (id, owner, balance)`, whose `CHECK (balance >= 0)` rejects any balance below 0. `../fixtures/shop-data.sql` holds four customers (Amara and Chidi in Leeds, Bo in York, Dana with no city) and two accounts (Amara 200.00, Bo 50.00).

Before every test, the tests rebuild both tables, so each task can pass on its own, with one exception: Task 7 uses your Task 1 and Task 4 code. The tests compile all the Java files first, so a compile error stops all seven tests.

## Task details

1. **Build the URL from the environment (`Shop.jdbcUrl`).** The service passes `System.getenv()` as `env`. Return the `jdbc:postgresql` URL for the values of `PGHOST`, `PGPORT` and `PGDATABASE` in `env`, never a hard-coded host, port or database. For `PGHOST=localhost`, `PGPORT=5432` and `PGDATABASE=shop` it is `jdbc:postgresql://localhost:5432/shop`. The test checks a sample URL, then connects with the URL your method builds from this run's settings.
2. **Find one customer (`JdbcCustomerRepository.findById`).** Replace the `throw` line.
   1. Open the connection with `connect()` and `prepareStatement` the fixed text `SELECT id, name, city FROM customers WHERE id = ?`.
   2. Bind the id with `setLong(1, id)` and run `executeQuery`.
   3. Move the cursor with `rs.next()`. When it returns `true`, map the row into `new Customer(rs.getLong("id"), rs.getString("name"), rs.getString("city"))` and return it in `Optional.of(...)`. When it returns `false`, return `Optional.empty()`.
   4. Open the Connection and PreparedStatement in one try-with-resources header and the ResultSet in its own; they close in the reverse order they were opened.
   5. The interface declares no `SQLException`, so catch it and throw an unchecked exception with it as the cause, as the `catch` block of the supplied `findAll` does. Copy only that `catch` block: `findAll`'s row mapping has the bug you fix in Task 6.

   The test also checks that a customer with no city comes back with `city` null and that no connection is left open.
3. **Close the injection hole (`JdbcCustomerRepository.findByCity`).** The supplied version joins the city into the SQL text. Run the test: the city `x' OR '1'='1` turns the `WHERE` into a condition that is true for every row, and every customer comes back. Rewrite it with a `?` placeholder in fixed text (`... WHERE city = ? ORDER BY id`), `prepareStatement`, and `setString(1, city)` before `executeQuery`. Open the ResultSet in its own try-with-resources. When it passes, Leeds still returns Amara and Chidi, and the attack finds nobody.
4. **Save a customer (`JdbcCustomerRepository.save`).** `findById` reads columns into a new `Customer`; `save` goes the other way. Use `INSERT INTO customers (id, name, city) VALUES (?, ?, ?)`, bind `customer.id()`, `customer.name()` and `customer.city()` in that order (`setLong`, `setString`, `setString`; parameter numbers start at 1; a null city stores NULL) and run `executeUpdate`. Catch `SQLException` and wrap it as in Task 2. The test saves a customer with a city and one without, reads the rows back, saves an existing id (the primary key rejects it, and your exception must carry the `SQLException` as its cause) and checks that no connection is left open.
5. **Make the transfer one transaction (`AccountRepository.transfer`).** The supplied `transfer` credits the receiving account, then debits the paying one, each as its own auto-committed statement. Run the test: when the debit fails because the payer cannot cover it, the credit is already committed and money has appeared from nowhere. Make both statements one transaction. `conn` must still be in scope where you roll back, so open the Connection in its own try-with-resources and the two statements in an inner one:

   ```java
   try (Connection conn = connect()) {
       conn.setAutoCommit(false);
       try (PreparedStatement credit = ...; PreparedStatement debit = ...) {
           // bind and run both statements
           conn.commit();
       } catch (SQLException e) {
           conn.rollback();
           throw e;
       }
   }
   ```

   When it passes, a transfer of 50.00 changes both balances, and a transfer of 500.00 throws `SQLException` and changes neither. The test cannot see whether you call `rollback()`. Call it anyway: a reused or pooled connection would otherwise carry the failed transaction into its next call.
6. **Read the failing test (`JdbcCustomerRepository.findAll`).** This test fails from the start. Read its message: it prints the expected list and the actual one. Compare one customer field by field to see which value landed in which field, find the line in `findAll` that causes it and fix only that line.
7. **Swap the storage (`Shop.customers`).** The service gets its storage only through `CustomerRepository`, and `customers` still constructs the `InMemoryCustomerRepository`, which forgets everything when the program stops. Change only that line, so it returns a `JdbcCustomerRepository` built from `jdbcUrl(env)`, `env.get("PGUSER")` and `env.get("PGPASSWORD")`. Nothing else in the service changes. When it passes, a customer saved through `Shop.customers` is a row in PostgreSQL.

Leave `Customer.java`, `CustomerRepository.java` and `InMemoryCustomerRepository.java` as they are.

**Starting over (optional):** the shipped version of every file is in the lab's original repository, in the same folder. Copy your attempt somewhere else first if you want to keep it, then paste the shipped text back into the file.

## What the tests do

This is the test-database lifecycle from *Testing a repository against a test database*. Each run creates its own database with a unique name such as `jdbc_lab_3f9c0a1b7d2e`, applies the schema and seed rows before every test, runs the tests, closes the connections and drops the database. It never touches a database it did not create. While a run is in progress, the database's name is in `starter/target/lab-database.txt`. After setup, the tests run offline and talk only to that PostgreSQL server.

## Prerequisites

| Tool | Version | Check |
| --- | --- | --- |
| PostgreSQL server and `psql` | 18 (16 or later works) | the connection check below |
| JDK | 25 | `java -version` |
| Apache Maven | 3.9 or later | `mvn -v` |

### Connection check

```
psql -h localhost -p 5432 -d postgres -c "SELECT version();"
```

It should print one row starting `PostgreSQL 18` (or 16, 17) without asking for a password.

## Setup

The quick start is all the setup most learners need. Only if the connection check needs other values: the tests read the same environment variables as `psql`. Set the ones you need in the terminal where you run the tests; they last until you close it, so set them again in a new terminal.

| Variable | Meaning | Default |
| --- | --- | --- |
| `PGHOST` | server host name | `localhost` |
| `PGPORT` | server port | `5432` |
| `PGUSER` | a PostgreSQL user allowed to create databases | your login name |
| `PGDATABASE` | an existing database to connect to first | `postgres` |

For example `export PGUSER=postgres` (macOS or Linux) or `$env:PGUSER = "postgres"` (Windows PowerShell).

**If your server asks for a password**, put one line, `localhost:5432:*:postgres:your-password`, in `~/.pgpass` and run `chmod 600 ~/.pgpass` (macOS or Linux), or in `%APPDATA%\postgresql\pgpass.conf` (Windows). Never write a password into a lab file.

## Troubleshooting

| What you see | What to do |
| --- | --- |
| `cd: no such file or directory: starter` (or `Cannot find path`) | The terminal is not in the lab's top folder, the one that holds this `README.md` and the `starter` folder. Change to that folder, then run `cd starter`. |
| `Could not reach PostgreSQL to create the lab database` | Run the [connection check](#connection-check) until it prints the version. |
| `password authentication failed` or `no password was provided` | Set up a [password file](#setup), or set `PGUSER`. |
| `permission denied to create database` | Use a PostgreSQL user that may create databases, such as `postgres`. |
| `database "..." does not exist` in Task 1 | `jdbcUrl` put the wrong value after the last slash; it must be `PGDATABASE`. |
| `Connection refused` in Task 1 | The host or port in the URL is wrong; use `PGHOST` and `PGPORT` exactly. |
| `COMPILATION ERROR` | Maven names the file and line. Fix the Java there first: no test runs until the code compiles. |
| `ResultSet not positioned properly, perhaps you need to call next` | Call `rs.next()` before reading a column; it moves the cursor onto the first row. |
| `The column index is out of range` | Parameter numbers start at 1 and follow the `?` placeholders from left to right; a quoted `'?'` is not a placeholder. |
| `cannot find symbol` ... `variable conn` | `conn` is used outside the try that declares it. Open the Connection in its own try-with-resources, as in Task 5's outline. |
| `unreported exception java.sql.SQLException` | `CustomerRepository` declares no `SQLException`: catch it and throw an unchecked exception with it as the cause, as the `catch` block in `findAll` does. |
| `left ... connection(s) open` | Put every Connection, PreparedStatement and ResultSet in a try-with-resources, in one header or nested; the nested form is needed only when you bind parameters before `executeQuery`. |
| `No suitable driver found` | Run the test command from `starter`; the PostgreSQL driver comes from `pom.xml`. |
| Maven cannot resolve a dependency offline | Run `mvn -q dependency:go-offline` once with the internet on. |
| A run was interrupted | Drop the database named in `starter/target/lab-database.txt`, for example `psql -h localhost -d postgres -c "DROP DATABASE jdbc_lab_3f9c0a1b7d2e WITH (FORCE);"` with the name from the file. |

## Tested with

PostgreSQL 18.3, JDK 25 and Maven 3.9.9 on macOS (arm64), with no-password local connections. Linux and Windows runs have not been recorded yet; the steps follow PostgreSQL's and pgJDBC's documented behaviour there.
