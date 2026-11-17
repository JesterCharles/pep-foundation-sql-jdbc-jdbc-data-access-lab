# JDBC Data Access lab: move the shop's customers into a database

The shop keeps its customers in memory, so they vanish whenever the service stops, and its store-credit transfer can leave one account credited when the other cannot pay.
You give the service a real JDBC layer, in seven tasks in the package `exercise`:
- a PostgreSQL database URL built from the environment;
- a `JdbcCustomerRepository` behind the existing `CustomerRepository` interface;
- a transfer that is one transaction;
- a one-line swap from in-memory storage to the database.

You practise connections, PreparedStatement and bound parameters, ResultSet mapping, try-with-resources and transactions.
Nothing needs installing: the tests run your code against a throwaway in-memory database that speaks PostgreSQL's SQL (see [What the tests do](#what-the-tests-do)).

**Time:** at most 40 minutes in two sittings.

| Sitting | Tasks | Estimate | Limit |
| --- | --- | --- | --- |
| 1 | Open and run, Tasks 1-2 | about 12 minutes | 15 minutes |
| 2 | [Resume](#resuming-sitting-2), Tasks 3-7 | about 20 minutes | 25 minutes |

If a sitting runs past its limit, note the next failing task and finish it at the start of the next sitting or later.

## Quick start

1. Open the lab project. The files you edit are in `src/main/java/exercise/`; the checklist below names them.
2. Select **Run Tests** before you change anything.

   **All 7 visible tests fail at first. That is expected: each failure is a task you have not done yet.**
   Every failure message starts with its task number, such as `Task 1:`. After sitting 1, Tasks 3 to 7 still fail until sitting 2.
3. Work through the checklist below in order. After each task, select **Run Tests** again and read the remaining `Task N:` messages.
4. **You are finished when all 7 visible tests pass.** Then select **Submit**.
   Submitting also runs held-out tests you can't see.

```
OPEN project -> RUN TESTS: 7 fail (expected)
  -> READ the "Task N:" messages -> EDIT the next TODO -> RUN TESTS again
  -> repeat for Tasks 1-7 -> SUBMIT when all 7 visible tests pass
```

In words: open the project, run the tests, read which tasks fail, complete one task's TODO, run the tests again, and submit when they all pass.

## Checklist

You edit only three files, all in `src/main/java/exercise/`. Each task's method has a `Task` comment, and the ones you write from scratch have a `TODO`.

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
The tests in `src/test/java/exercise/` and the data in `src/test/resources/` are restored to their original state before your work is graded, so changes to them do not count. Leave the build file `pom.xml` as it is.

Run Tests runs the visible tests. Submit also runs held-out tests you can't see: they try more cases, such as a customer with no city, an id that does not exist and an id that is already taken, so make each method correct for every case its task describes, not only the ones the visible tests try.

From a terminal in the project folder, you can run one task's test on its own, for example `mvn -q test -Dtest='VisibleTests#task1UrlFromEnvironment'`, or all of them with `mvn -q test`.
`WARNING:` lines from Java or Maven, such as `WARNING: A restricted method ...`, are not test failures.

### Resuming (sitting 2)

Open the lab project again and select **Run Tests**. The first failing task should be Task 3.

## The data

`src/test/resources/schema.sql` creates two tables: `customers (id, name, city)`, where `city` may be NULL, and `accounts (id, owner, balance)`, whose `CHECK (balance >= 0)` rejects any balance below 0.
`src/test/resources/shop-data.sql` holds four customers (Amara and Chidi in Leeds, Bo in York, Dana with no city) and two accounts (Amara 200.00, Bo 50.00).

Every test starts with a new database holding exactly these rows, so each task can pass on its own, with one exception: Task 7 uses your Task 1 and Task 4 code.
The tests compile all the Java files first, so a compile error stops all seven tests.

## Task steps

### Task 1: build the URL from the environment (`Shop.jdbcUrl`)

The service passes `System.getenv()` as `env`. Return the `jdbc:postgresql` URL for the values of `PGHOST`, `PGPORT` and `PGDATABASE` in `env`, never a hard-coded host, port or database.
For `PGHOST=localhost`, `PGPORT=5432` and `PGDATABASE=shop` it is `jdbc:postgresql://localhost:5432/shop`.
The visible test checks one sample URL; the held-out tests try other settings, so a URL that only matches the sample does not pass.

### Task 2: find one customer (`JdbcCustomerRepository.findById`)

Replace the `throw` line.
1. Open the connection with `connect()` and `prepareStatement` the fixed text `SELECT id, name, city FROM customers WHERE id = ?`.
2. Bind the id with `setLong(1, id)` and run `executeQuery`.
3. Move the cursor with `rs.next()`. When it returns `true`, map the row into `new Customer(rs.getLong("id"), rs.getString("name"), rs.getString("city"))` and return it in `Optional.of(...)`. When it returns `false`, return `Optional.empty()`.
4. Open the Connection and PreparedStatement in one try-with-resources header and the ResultSet in its own; they close in the reverse order they were opened.
5. The interface declares no `SQLException`, so catch it and throw an unchecked exception with it as the cause, as the `catch` block of the supplied `findAll` does. Copy only that `catch` block: `findAll`'s row mapping has the bug you fix in Task 6.

The visible test finds Chidi and checks that no connection is left open. The held-out tests also check that a customer with no city comes back with `city` null and that a missing id gives `Optional.empty()`.

### Task 3: close the injection hole (`JdbcCustomerRepository.findByCity`)

The supplied version joins the city into the SQL text. Run the tests: the city `x' OR '1'='1` turns the `WHERE` into a condition that is true for every row, and every customer comes back.
Rewrite it with a `?` placeholder in fixed text (`... WHERE city = ? ORDER BY id`), `prepareStatement`, and `setString(1, city)` before `executeQuery`. Open the ResultSet in its own try-with-resources.
When it passes, Leeds still returns Amara and Chidi, and the attack finds nobody.

### Task 4: save a customer (`JdbcCustomerRepository.save`)

Use `INSERT INTO customers (id, name, city) VALUES (?, ?, ?)`, bind `customer.id()`, `customer.name()` and `customer.city()` in that order and run `executeUpdate`. Catch `SQLException` and wrap it as in Task 2.
The visible test saves a customer, reads the row back and checks that no connection is left open. The held-out tests also save a customer with no city, and save an existing id: the primary key rejects it, and your exception must carry the `SQLException` as its cause.

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
When it passes, a customer saved through `Shop.customers` is a row in the test's database.

**Starting over (optional):** before you change a file, keep a copy of it outside the project folder. To restart that file, paste the copy back.

### About the test results

A passing run shows the code behaves correctly against a real SQL database.
It does not check every construct your code uses, so compare your code with each task's steps yourself.

## What the tests do

In production the shop's database is PostgreSQL. The tests don't need a PostgreSQL server: each test creates its own new in-memory database with a unique name such as `jdbc_lab_3f9c0a1b7d2e`, using H2 in PostgreSQL compatibility mode, and loads the schema and seed rows into it.
Your code still uses ordinary `jdbc:postgresql://localhost:5432/jdbc_lab_...` URLs: a small driver inside the tests sends those URLs to that test's database. When the test ends, the database is closed and disappears. Nothing is written outside the project's `target/` folder.

## Using an AI assistant

If you use an AI assistant, ask it to explain, not to answer.
"Explain why a bound parameter stops SQL injection" teaches you JDBC; "write my repository" does not.
Check any explanation with a test run before you trust it.

## Prerequisites

None for **Run Tests**. For terminal runs only:

| Tool | Version | Check |
| --- | --- | --- |
| JDK | 17 or later | `java -version` |
| Apache Maven | 3.9 or later | `mvn -v` |

## Troubleshooting

| What you see | What to do |
| --- | --- |
| `COMPILATION ERROR` | Maven names the file and line. Fix the Java there first: no test runs until the code compiles. |
| `No suitable driver found` in Task 7 | The URL from `jdbcUrl` does not start with `jdbc:postgresql://`. |
| `database "..." does not exist` or `Database ... not found` in Task 7 | `jdbcUrl` put the wrong value after the last slash; it must be `PGDATABASE`. |
| `No data is available` | Call `rs.next()` before reading a column; it moves the cursor onto the first row. |
| `Invalid value ... for parameter "parameterIndex"` | Parameter numbers start at 1 and follow the `?` placeholders from left to right; a quoted `'?'` is not a placeholder. |
| `cannot find symbol` ... `variable conn` | `conn` is used outside the try that declares it. Open the Connection in its own try-with-resources, as in Task 5's outline. |
| `unreported exception java.sql.SQLException` | `CustomerRepository` declares no `SQLException`: catch it and throw an unchecked exception with it as the cause, as the `catch` block in `findAll` does. |
| `left ... connection(s) open` | Put every Connection, PreparedStatement and ResultSet in a try-with-resources, in one header or nested; the nested form is needed only when you bind parameters before `executeQuery`. |
