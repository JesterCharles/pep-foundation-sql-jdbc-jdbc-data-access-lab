-- Seed rows for every test: four customers (one with no city) and two accounts.
INSERT INTO customers (id, name, city) VALUES
  (1, 'Amara', 'Leeds'),
  (2, 'Bo', 'York'),
  (3, 'Chidi', 'Leeds'),
  (4, 'Dana', NULL);

INSERT INTO accounts (id, owner, balance) VALUES
  (1, 'Amara', 200.00),
  (2, 'Bo', 50.00);
