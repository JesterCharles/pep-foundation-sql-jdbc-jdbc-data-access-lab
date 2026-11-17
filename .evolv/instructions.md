# JDBC Data Access lab: move the shop's customers into PostgreSQL

The shop keeps its customers in memory, so they vanish whenever the service stops, and its store-credit transfer can leave one account credited when the other cannot pay.
You give the service a real JDBC layer, in seven tasks in the package `shop`:
- a database URL built from the environment;
- a `JdbcCustomerRepository` behind the existing `CustomerRepository` interface;
- a transfer that is one transaction;
- a one-line swap from in-memory storage to PostgreSQL.

Your Java code talks to a database on the PostgreSQL server the tests connect to, through the PostgreSQL JDBC driver.
You practise connections, PreparedStatement and bound parameters, ResultSet mapping, try-with-resources and transactions.

**Time:** at most 40 minutes in two sittings.

| Sitting | Tasks | Estimate | Limit |
| --- | --- | --- | --- |
| 1 | Open and run, Tasks 1-2 | about 12 minutes | 15 minutes |
| 2 | [Resume](#resuming-sitting-2), Tasks 3-7 | about 20 minutes | 25 minutes |

If a sitting runs past its limit, note the next failing task and finish it at the start of the next sitting or later.

## Before you start: PostgreSQL

The tests need a PostgreSQL server they can reach (16 or later). See [Prerequisites](#prerequisites) for the connection check, and [Setup](#setup) if your server needs other connection settings or a password.
If no server is reachable, every test fails with `Could not reach PostgreSQL to create the lab database`. That is a missing prerequisite, not a mistake in your code.

## Quick start

1. Open the lab project. The files you edit are in `src/main/java/shop/`; the checklist below names them.
2. Select **Run Tests** before you change anything.

   **All 7 tests fail at first. That is expected: each failure is a task you have not done yet.**
   Every failure message starts with its task number, such as `Task 1:`. After sitting 1, Tasks 3 to 7 still fail until sitting 2.
3. Work through the checklist below in order. After each task, select **Run Tests** again and read the remaining `Task N:` messages.
4. **You are finished when all 7 tests pass.** Then select **Submit**.

```
OPEN project -> RUN TESTS: 7 fail (expected)
  -> READ the "Task N:" messages -> EDIT the next TODO -> RUN TESTS again
  -> repeat for Tasks 1-7 -> SUBMIT when all 7 tests pass
```

In words: open the project, run the tests, read which tasks fail, complete one task's TODO, run the tests again, and submit when they all pass.

## Checklist

You edit only three files, all in `src/main/java/shop/`. Each task's method has a `Task` comment, and the ones you write from scratch have a `TODO`.

**Sitting 1**

- [ ] Task 1: `Shop.java`, `jdbcUrl` (build the URL from the environment; test `task1UrlFromEnvironment`)
- [ ] Task 2: `JdbcCustomerRepository.java`, `findById` (find one customer; test `task2FindById`)

**Sitting 2** (start with [Resuming](#resuming-sitting-2))

- [ ] Task 3: `JdbcCustomerRepository.java`, `findByCity` (close the injection hole; test `task3FindByCityBound`)
- [ ] Task 4: `JdbcCustomerRepository.java`, `save` (save a customer; test `task4Save`)
- [ ] Task 5: `AccountRepository.java`, `transfer` (make the transfer one transaction; test `task5TransferIsOneTransaction`)
- [ ] Task 6: `JdbcCustomerRepository.java`, `findAll` (read the failing test; test `task6ReadTheFailingTest`)
- [ ] Task 7: `Shop.java`, `customers` (swap the storage; test `task7SwapTheStorage`)

A task is done when no failure message starts with its number.
Edit only `Shop.java`, `JdbcCustomerRepository.java` and `AccountRepository.java`. Leave `Customer.java`, `CustomerRepository.java` and `InMemoryCustomerRepository.java` as they are.
The tests in `src/test/java/shop/` are restored to their original state before your work is graded, so changes to them do not count. Leave the build file `pom.xml`, the `fixtures/` folder and `expected/test-summary.txt` as they are.

From a terminal in the project folder, you can run one task's test on its own, for example `mvn -q test -Dtest='ShopLabTest#task1UrlFromEnvironment'`, or all of them with `mvn -q test`.
`WARNING:` lines from Java or Maven, such as `WARNING: A restricted method ...`, are not test failures.

### Resuming (sitting 2)

Open the lab project again and select **Run Tests**. If you run the tests from a terminal and set any [Setup](#setup) variables in sitting 1, set them again first. The first failing task should be Task 3.

## The data

`fixtures/schema.sql` creates two tables: `customers (id, name, city)`, where `city` may be NULL, and `accounts (id, owner, balance)`, whose `CHECK (balance >= 0)` rejects any balance below 0.
`fixtures/shop-data.sql` holds four customers (Amara and Chidi in Leeds, Bo in York, Dana with no city) and two accounts (Amara 200.00, Bo 50.00).

Before every test, the tests rebuild both tables, so each task can pass on its own, with one exception: Task 7 uses your Task 1 and Task 4 code.
The tests compile all the Java files first, so a compile error stops all seven tests.

## Task steps

### Task 1: build the URL from the environment (`Shop.jdbcUrl`)

The service passes `System.getenv()` as `env`. Return the `jdbc:postgresql` URL for the values of `PGHOST`, `PGPORT` and `PGDATABASE` in `env`, never a hard-coded host, port or database.
For `PGHOST=localhost`, `PGPORT=5432` and `PGDATABASE=shop` it is `jdbc:postgresql://localhost:5432/shop`.
The test checks a sample URL, then connects with the URL your method builds from this run's settings.

### Task 2: find one customer (`JdbcCustomerRepository.findById`)

Replace the `throw` line.
1. Open the connection with `connect()` and `prepareStatement` the fixed text `SELECT id, name, city FROM customers WHERE id = ?`.
2. Bind the id with `setLong(1, id)` and run `executeQuery`.
3. Move the cursor with `rs.next()`. When it returns `true`, map the row into `new Customer(rs.getLong("id"), rs.getString("name"), rs.getString("city"))` and return it in `Optional.of(...)`. When it returns `false`, return `Optional.empty()`.
4. Open the Connection and PreparedStatement in one try-with-resources header and the ResultSet in its own; they close in the reverse order they were opened.
5. The interface declares no `SQLException`, so catch it and throw an unchecked exception with it as the cause, as the `catch` block of the supplied `findAll` does. Copy only that `catch` block: `findAll`'s row mapping has the bug you fix in Task 6.

The test also checks that a customer with no city comes back with `city` null and that no connection is left open.

### Task 3: close the injection hole (`JdbcCustomerRepository.findByCity`)

The supplied version joins the city into the SQL text. Run the tests: the city `x' OR '1'='1` turns the `WHERE` into a condition that is true for every row, and every customer comes back.
Rewrite it with a `?` placeholder in fixed text (`... WHERE city = ? ORDER BY id`), `prepareStatement`, and `setString(1, city)` before `executeQuery`. Open the ResultSet in its own try-with-resources.
When it passes, Leeds still returns Amara and Chidi, and the attack finds nobody.

### Task 4: save a customer (`JdbcCustomerRepository.save`)

Use `INSERT INTO customers (id, name, city) VALUES (?, ?, ?)`, bind `customer.id()`, `customer.name()` and `customer.city()` in that order and run `executeUpdate`. Catch `SQLException` and wrap it as in Task 2.
The test saves a customer with a city and one without, reads the rows back, saves an existing id (the primary key rejects it, and your exception must carry the `SQLException` as its cause) and checks that no connection is left open.

<details><summary>Hint</summary>

`findById` reads columns into a new `Customer`; `save` goes the other way. Bind with `setLong`, `setString`, `setString`; parameter numbers start at 1, and a null city stores NULL.

</details>

### Task 5: make the transfer one transaction (`AccountRepository.transfer`)

The supplied `transfer` credits the receiving account, then debits the paying one, each as its own auto-committed statement. Run the tests: when the debit fails because the payer cannot cover it, the credit is already committed and money has appeared from nowhere.
Make both statements one transaction. `conn` must still be in scope where you roll back, so open the Connection in its own try-with-resources and the two statements in an inner one:

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

When it passes, a transfer of 50.00 changes both balances, and a transfer of 500.00 throws `SQLException` and changes neither.
The test cannot see whether you call `rollback()`. Call it anyway: a reused or pooled connection would otherwise carry the failed transaction into its next call.

### Task 6: read the failing test (`JdbcCustomerRepository.findAll`)

This test fails from the start. Read its message: it prints the expected list and the actual one. Find the line in `findAll` that causes the difference and fix only that line.

<details><summary>Hint</summary>

Compare one customer field by field to see which value landed in which field.

</details>

### Task 7: swap the storage (`Shop.customers`)

The service gets its storage only through `CustomerRepository`, and `customers` still constructs the `InMemoryCustomerRepository`, which forgets everything when the program stops.
Change only that line, so it returns a `JdbcCustomerRepository` built from `jdbcUrl(env)`, `env.get("PGUSER")` and `env.get("PGPASSWORD")`. Nothing else in the service changes.
When it passes, a customer saved through `Shop.customers` is a row in PostgreSQL.

**Starting over (optional):** before you change a file, keep a copy of it outside the project folder. To restart that file, paste the copy back.

### About the test results

A passing run shows the code behaves correctly against a real database.
It does not check every construct your code uses, so compare your code with each task's steps yourself.

## What the tests do

Each run creates its own database with a unique name such as `jdbc_lab_3f9c0a1b7d2e`, applies the schema and seed rows before every test, runs the tests, closes the connections and drops the database.
It never touches a database it did not create. While a run is in progress, the database's name is in `target/lab-database.txt`.
The tests talk only to that PostgreSQL server.

## Using an AI assistant

If you use an AI assistant, ask it to explain, not to answer.
"Explain why a bound parameter stops SQL injection" teaches you JDBC; "write my repository" does not.
Check any explanation with a test run before you trust it.

## Prerequisites

| Tool | Version | Check |
| --- | --- | --- |
| PostgreSQL server and `psql` | 16 or later | the connection check below |
| JDK (only for terminal runs) | 17 or later | `java -version` |
| Apache Maven (only for terminal runs) | 3.9 or later | `mvn -v` |

### Connection check

```
psql -h localhost -p 5432 -d postgres -c "SELECT version();"
```

It should print one row starting `PostgreSQL 16` (or later) without asking for a password.

## Setup

Only if the connection check needs other values: the tests read the same environment variables as `psql`.

| Variable | Meaning | Default |
| --- | --- | --- |
| `PGHOST` | server host name | `localhost` |
| `PGPORT` | server port | `5432` |
| `PGUSER` | a PostgreSQL user allowed to create databases | your login name |
| `PGDATABASE` | an existing database to connect to first | `postgres` |

For a terminal run, set the ones you need in that terminal, for example `export PGUSER=postgres` (macOS or Linux) or `$env:PGUSER = "postgres"` (Windows PowerShell). They last until you close it, so set them again in a new terminal.

**If your server asks for a password**, put one line, `localhost:5432:*:postgres:your-password`, in `~/.pgpass` and run `chmod 600 ~/.pgpass` (macOS or Linux), or in `%APPDATA%\postgresql\pgpass.conf` (Windows). Never write a password into a lab file.

## Troubleshooting

| What you see | What to do |
| --- | --- |
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
| `No suitable driver found` | Run the tests with **Run Tests** or from the project folder; the PostgreSQL driver comes from `pom.xml`. |
| A run was interrupted | Drop the database named in `target/lab-database.txt`, for example `psql -h localhost -d postgres -c "DROP DATABASE jdbc_lab_3f9c0a1b7d2e WITH (FORCE);"` with the name from the file. |
