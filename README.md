# JDBC Data Access lab: move the shop's customers into PostgreSQL

The shop from the JDBC Data Access pages keeps its customers in memory, so they vanish whenever the service stops, and its store-credit transfer can leave one account credited when the other cannot pay. You give the service a real JDBC layer: a URL built from the environment, a `JdbcCustomerRepository` behind the existing `CustomerRepository` interface, a transfer that is one transaction, and a one-line swap from the in-memory storage to PostgreSQL. Everything runs for real: your Java code talks to a database on your own PostgreSQL server through the PostgreSQL JDBC driver (pgJDBC).

This lab practises *Connecting Java to Postgres with JDBC*, *Prepared statements and SQL injection*, *Transactions and ACID*, *Swapping the in-memory repository for a JDBC one* and *Testing a repository against a test database*. It needs no cloud account, container or shared server: only your local PostgreSQL server, a JDK and Maven. It is the hands-on lab of the course's JDBC data access project, and you start it after the five JDBC Data Access pages.

**Time: at most 40 minutes in two sittings**, the course's JDBC project-lab time.

| Sitting | When | Estimate | Limit |
| --- | --- | --- | --- |
| 1: setup and Tasks 1-2 | day F23, the 15 project-lab minutes of the Build block, after the JDBC Data Access pages | about 12 minutes: setup 5, Task 1 2, Task 2 5 | 15 |
| 2: Tasks 3-7 | day F24, the Practice without AI block | about 20 minutes: resuming 2, Task 3 3, Task 4 4, Task 5 6, Task 6 3, Task 7 2 | 25 |

Together that is about 32 minutes of work, with a few minutes of buffer in each sitting. The estimates are for an average learner. On day F23, stop after Task 2; on day F24, start with Resuming, then Task 3. If a sitting runs past its limit, pause, note the next failing task, and finish it at the start of the next sitting or in self-study.

## What you edit

| File (in `starter/src/main/java/shop/`) | What you do | Why the shop needs it |
| --- | --- | --- |
| `Shop.java` | Task 1: write `jdbcUrl`; Task 7: change one line in `customers` | how the service reaches PostgreSQL |
| `JdbcCustomerRepository.java` | Task 2: write `findById`; Task 3: fix `findByCity`; Task 4: write `save`; Task 6: fix `findAll` | the customers table behind the repository interface |
| `AccountRepository.java` | Task 5: fix `transfer` | a store-credit transfer that cannot go half done |

Leave `Customer.java`, `CustomerRepository.java` and `InMemoryCustomerRepository.java` (in the same folder), `tests/`, `fixtures/`, `expected/`, `starter/pom.xml` and `starter/.gitignore` alone: the tests and your copy's version control read them. The single test command, run in `starter/`, is:

```
mvn -q -o test
```

Each failing check starts with its task number and names the next thing to fix. After sitting 1, the checks for Tasks 3 to 7 still fail; that is expected until sitting 2.

## Prerequisites

| Tool | Version | Check |
| --- | --- | --- |
| PostgreSQL server and `psql` | 18 (16 or later works) | the connection check below |
| JDK | 25 | `java -version` |
| Apache Maven | 3.9 or later | `mvn -v` |

**Connection check.** Run this in a new terminal. It must print one row starting `PostgreSQL 18` (or 16, 17) without asking for a password:

```
psql -h localhost -p 5432 -d postgres -c "SELECT version();"
```

## Setup

1. Open the lab. This lab is a self-contained project: the folder that holds this `README.md` is its root, and in your own copy or clone it is the repository root. Open that root folder in VS Code or any other editor; no extension is needed. Run every command in a terminal whose working directory is `starter/`, the Maven project folder inside the root.
2. Only if the connection check needed other settings: set the same values for the tests in this terminal. They last only as long as the terminal, so set them again in every new terminal. They read the variables `psql` reads: `PGHOST` (a host name such as `localhost`, default `localhost`), `PGPORT` (default `5432`), `PGUSER` (a user allowed to create databases, default your login name) and `PGDATABASE` (an existing database, default `postgres`). For example `export PGUSER=postgres` (macOS or Linux) or `$env:PGUSER = "postgres"` (Windows PowerShell). If your server asks for a password, put it in a password file: one line `localhost:5432:*:postgres:your-password` in `~/.pgpass` (then `chmod 600 ~/.pgpass`) on macOS or Linux, or in `%APPDATA%\postgresql\pgpass.conf` on Windows. Never write a password into a lab file.
3. Download the test libraries once (needs the internet; after the SQL labs, Maven already has most of them):

   ```
   mvn -q dependency:go-offline
   ```

After this step the test command works offline.

**Resuming (start of sitting 2).** Open a terminal in `starter/`, set the Setup step 2 variables again if you needed them, run `mvn -q -o test` once and read the first failing task: it should be Task 3.

## The data

`fixtures/schema.sql` creates the shop's two tables: `customers (id, name, city)`, where `city` may be NULL, and `accounts (id, owner, balance)`, whose `CHECK (balance >= 0)` rejects any balance below 0. `fixtures/shop-data.sql` holds four customers (Amara and Chidi in Leeds, Bo in York, Dana with no city) and two accounts (Amara 200.00, Bo 50.00). Before every test, the tests rebuild both tables from these two files, so each task can pass on its own and in any order, with one exception: Task 7 uses your Task 1 and Task 4 code. The tests compile the Java files first, so a Java compile error stops all seven tests.

This is the test-database lifecycle from *Testing a repository against a test database*: each run creates its own database, applies the schema and seed rows before every test, runs the tests, closes the connections and drops the database. Your everyday databases are never touched.

## Tasks

1. **Build the URL from the environment (`Shop.jdbcUrl`).** The service passes `System.getenv()` as `env`. Return the `jdbc:postgresql` URL for the values of `PGHOST`, `PGPORT` and `PGDATABASE` in `env`, never a hard-coded host, port or database: for `PGHOST=localhost`, `PGPORT=5432` and `PGDATABASE=shop` it is `jdbc:postgresql://localhost:5432/shop`. Done: `task1UrlFromEnvironment` passes. It checks a sample URL and then connects with the URL your method builds from this run's settings.
2. **Find one customer (`JdbcCustomerRepository.findById`).** Replace the `throw` line. Open the connection with `connect()` and `prepareStatement` the fixed text `SELECT id, name, city FROM customers WHERE id = ?`, bind the id with `setLong(1, id)` and run `executeQuery`. Move the cursor with `rs.next()`: when it returns `true`, map the row into `new Customer(rs.getLong("id"), rs.getString("name"), rs.getString("city"))` and return it in `Optional.of(...)`; when it returns `false`, return `Optional.empty()`. Open the Connection and PreparedStatement in one try-with-resources header and the ResultSet in its own; they close in the reverse order they were opened. The interface declares no `SQLException`, so catch it and throw an unchecked exception with it as the cause, as the `catch` block of the supplied `findAll` does. Copy only that `catch` block: `findAll`'s row mapping has the bug you fix in Task 6. Done: `task2FindById` passes. It also checks that a customer with no city comes back with `city` null and that no connection is left open.
3. **Close the injection hole (`JdbcCustomerRepository.findByCity`).** The supplied version joins the city into the SQL text. Run the tests: the city `x' OR '1'='1` turns the `WHERE` into a condition that is true for every row, and every customer comes back. Rewrite it with a `?` placeholder in fixed text (`... WHERE city = ? ORDER BY id`), `prepareStatement`, and `setString(1, city)` before `executeQuery`. Open the ResultSet in its own try-with-resources. Done: `task3FindByCityBound` passes: Leeds still returns Amara and Chidi, and the attack finds nobody.
4. **Save a customer (`JdbcCustomerRepository.save`).** `findById` reads columns into a new `Customer`; `save` goes the other way. `INSERT INTO customers (id, name, city) VALUES (?, ?, ?)`, bind `customer.id()`, `customer.name()` and `customer.city()` in that order (`setLong`, `setString`, `setString`; parameter numbers start at 1; a null city stores NULL) and run `executeUpdate`, which returns the number of rows inserted. Catch `SQLException` and wrap it as in Task 2. Done: `task4Save` passes. It saves a customer with a city and one without, reads the rows back, saves an existing id (the primary key rejects it, and your exception must carry the `SQLException` as its cause) and checks that no connection is left open.
5. **Make the transfer one transaction (`AccountRepository.transfer`).** The supplied `transfer` credits the receiving account, then debits the paying one, each as its own auto-committed statement. Run the tests: when the debit fails because the payer cannot cover it (the `CHECK` rejects a negative balance), the credit has already been committed and money has appeared from nowhere. Make both statements one transaction. `conn` must still be in scope where you roll back, so open the Connection in its own try-with-resources and the two statements in an inner one:

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

   Done: `task5TransferIsOneTransaction` passes: a transfer of 50.00 changes both balances, and a transfer of 500.00 throws `SQLException` and changes neither. The test cannot see whether you call `rollback()`. Call it anyway: here the connection closes right after and the server discards the unfinished work, but a reused or pooled connection would carry the failed transaction into its next call.
6. **Read the failing test (`JdbcCustomerRepository.findAll`).** `task6ReadTheFailingTest` fails from the start. Read its message: it prints the expected list and the actual one. Compare one customer field by field to see which value landed in which field, find the line in `findAll` that causes it and fix only that line. Done: `task6ReadTheFailingTest` passes.
7. **Swap the storage (`Shop.customers`).** The service gets its storage only through `CustomerRepository`, and `customers` still constructs the `InMemoryCustomerRepository`, which forgets everything when the program stops. Change only that line, so it returns a `JdbcCustomerRepository` built from `jdbcUrl(env)`, `env.get("PGUSER")` and `env.get("PGPASSWORD")`. Nothing else in the service changes. Done: `task7SwapTheStorage` passes: a customer saved through `Shop.customers` is now a row in PostgreSQL. The whole run then matches Expected output.

**Starting over.** Save your attempt elsewhere if you want to keep it, then restore the file you want to restart from a fresh copy of the lab.

## Test

In `starter/`:

```
mvn -q -o test
```

Each run creates its own database with a unique name such as `jdbc_lab_3f9c0a1b7d2e`, runs the tests in it, and drops it again. It never removes a database it did not create. While a run is in progress, the database's name is in `starter/target/lab-database.txt`.

As shipped, all seven tests fail; that is expected.

## Expected output

When every task is done, `mvn -q -o test` exits with status 0 (`echo $?` on macOS or Linux, `$LASTEXITCODE` in PowerShell, prints `0`) and `starter/target/surefire-reports/shop.ShopLabTest.txt` contains the line `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`. Warnings such as `WARNING: A restricted method ...` are not test failures. The full summary is in `expected/test-summary.txt`.

## Offline note

Only the setup step downloads anything. The test command runs offline and talks only to your local PostgreSQL server.

## Cleanup

- The tests drop their own database when they finish.
- After an interrupted run, drop the database named in `starter/target/lab-database.txt`, for example `psql -h localhost -d postgres -c "DROP DATABASE jdbc_lab_3f9c0a1b7d2e WITH (FORCE);"`, before you delete `starter/target`.
- To reset the lab, delete `starter/target` (compiled classes and test reports) and restore any file you want to restart from a fresh copy of the lab. The libraries in your local Maven repository (`~/.m2/repository`) are kept for later labs.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `Could not reach PostgreSQL to create the lab database` | Run the connection check with the same settings until it prints the version. |
| `password authentication failed` or `no password was provided` | Check the password file line (host, port, user, password) and its `chmod 600`, or `PGUSER`. |
| `database "..." does not exist` in Task 1 | `jdbcUrl` put the wrong value after the last slash; it must be `PGDATABASE`. |
| `Connection refused` in Task 1 | The host or port in the URL is wrong; use `PGHOST` and `PGPORT` exactly. |
| `permission denied to create database` | Use a user that may create databases, such as `postgres`. |
| `COMPILATION ERROR` | Maven names the file and line; fix the Java syntax there first, because no test runs until the code compiles. |
| `ResultSet not positioned properly, perhaps you need to call next` | Call `rs.next()` before reading a column; it moves the cursor onto the first row. |
| `The column index is out of range` | Parameter numbers start at 1 and follow the `?` placeholders from left to right; a quoted `'?'` is not a placeholder. |
| `cannot find symbol` ... `variable conn` | `conn` is used outside the try that declares it. Open the Connection in its own try-with-resources, as in Task 5's outline. |
| `unreported exception java.sql.SQLException` | `CustomerRepository` declares no `SQLException`: catch it and throw an unchecked exception with it as the cause, as the `catch` block in `findAll` does. |
| `left ... connection(s) open` | Put every Connection, PreparedStatement and ResultSet in a try-with-resources, in one header or nested; the nested form is needed only when you bind parameters before `executeQuery`. |
| `No suitable driver found` | Run the tests with the test command from `starter/`; the PostgreSQL driver comes from `pom.xml`. |
| Maven cannot resolve a dependency offline | Run the setup step once with the internet on. |
| A sitting runs past its limit | Pause, note the next failing task, and finish it at the start of the next sitting or in self-study. |

## Tested on

The shipped starter and a completed set of files were run from a fresh clone of this lab on macOS (arm64) with PostgreSQL 18.3, JDK 25 and Maven 3.9.9 against a local server with trust authentication; the course authors keep that run's record with the course's own evidence. Linux and Windows runs have not yet been recorded; the steps follow PostgreSQL's and pgJDBC's documented behaviour on those systems.
