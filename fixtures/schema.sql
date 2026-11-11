-- The shop's two tables, applied to a fresh lab database before every test.
CREATE TABLE customers (
    id   bigint       PRIMARY KEY,
    name varchar(100) NOT NULL,
    city varchar(60)
);

CREATE TABLE accounts (
    id      bigint        PRIMARY KEY,
    owner   varchar(100)  NOT NULL,
    balance numeric(10,2) NOT NULL CHECK (balance >= 0)
);
