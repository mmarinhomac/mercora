-- Create the 'orders' table if it doesn't exist
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    total_amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_method VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    payment_id UUID,
    created_at TIMESTAMP NOT NULL
);

-- Insert fake order data (50 records)
INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174000', 'CUST001', 150.5000, 'USD', 'credit_card', 'PAYMENT_PENDING', NULL, '2026-01-01 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174000');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174001', 'CUST002', 299.9900, 'USD', 'credit_card', 'PAID', NULL, '2026-01-02 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174001');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174002', 'CUST003', 75.2500, 'USD', 'debit_card', 'PAYMENT_PENDING', NULL, '2026-01-02 12:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174002');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174003', 'CUST004', 450.0000, 'USD', 'bank_transfer', 'PAID', NULL, '2026-01-03 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174003');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174004', 'CUST005', 199.9900, 'USD', 'credit_card', 'FAILED', NULL, '2026-01-03 10:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174004');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174005', 'CUST006', 325.5000, 'USD', 'debit_card', 'PAYMENT_PENDING', NULL, '2026-01-04 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174005');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174006', 'CUST007', 89.9900, 'USD', 'credit_card', 'PAID', NULL, '2026-01-04 09:15:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174006');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174007', 'CUST008', 550.0000, 'BRL', 'bank_transfer', 'PAYMENT_PENDING', NULL, '2026-01-05 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174007');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174008', 'CUST009', 245.7500, 'USD', 'credit_card', 'PAID', NULL, '2026-01-05 13:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174008');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174009', 'CUST010', 175.0000, 'USD', 'debit_card', 'CREATED', NULL, '2026-01-06 08:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174009');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174010', 'CUST011', 520.9900, 'USD', 'credit_card', 'PAID', NULL, '2026-01-06 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174010');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174011', 'CUST012', 99.9900, 'USD', 'bank_transfer', 'FAILED', NULL, '2026-01-07 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174011');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174012', 'CUST013', 410.5000, 'BRL', 'credit_card', 'PAYMENT_PENDING', NULL, '2026-01-07 11:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174012');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174013', 'CUST014', 275.0000, 'USD', 'debit_card', 'PAID', NULL, '2026-01-08 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174013');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174014', 'CUST015', 650.0000, 'USD', 'credit_card', 'PAYMENT_PENDING', NULL, '2026-01-08 14:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174014');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174015', 'CUST016', 125.5000, 'USD', 'bank_transfer', 'PAID', NULL, '2026-01-09 09:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174015');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174016', 'CUST017', 375.9900, 'BRL', 'credit_card', 'CREATED', NULL, '2026-01-09 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174016');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174017', 'CUST018', 225.0000, 'USD', 'debit_card', 'PAID', NULL, '2026-01-10 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174017');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174018', 'CUST019', 299.5000, 'USD', 'credit_card', 'FAILED', NULL, '2026-01-10 13:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174018');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174019', 'CUST020', 550.7500, 'USD', 'bank_transfer', 'PAYMENT_PENDING', NULL, '2026-01-11 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174019');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174020', 'CUST021', 185.9900, 'BRL', 'credit_card', 'PAID', NULL, '2026-01-11 10:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174020');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174021', 'CUST022', 425.0000, 'USD', 'debit_card', 'PAYMENT_PENDING', NULL, '2026-01-12 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174021');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174022', 'CUST023', 350.5000, 'USD', 'credit_card', 'PAID', NULL, '2026-01-12 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174022');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174023', 'CUST024', 100.0000, 'USD', 'bank_transfer', 'CREATED', NULL, '2026-01-13 08:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174023');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174024', 'CUST025', 599.9900, 'BRL', 'credit_card', 'PAID', NULL, '2026-01-13 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174024');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174025', 'CUST026', 275.5000, 'USD', 'debit_card', 'FAILED', NULL, '2026-01-14 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174025');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174026', 'CUST027', 475.0000, 'USD', 'credit_card', 'PAYMENT_PENDING', NULL, '2026-01-14 11:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174026');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174027', 'CUST028', 150.7500, 'USD', 'bank_transfer', 'PAID', NULL, '2026-01-15 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174027');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174028', 'CUST029', 625.9900, 'BRL', 'credit_card', 'PAYMENT_PENDING', NULL, '2026-01-15 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174028');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174029', 'CUST030', 200.0000, 'USD', 'debit_card', 'PAID', NULL, '2026-01-16 09:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174029');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174030', 'CUST031', 325.5000, 'USD', 'credit_card', 'CREATED', NULL, '2026-01-16 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174030');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174031', 'CUST032', 500.0000, 'USD', 'bank_transfer', 'PAID', NULL, '2026-01-17 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174031');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174032', 'CUST033', 150.0000, 'BRL', 'credit_card', 'FAILED', NULL, '2026-01-17 10:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174032');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174033', 'CUST034', 575.5000, 'USD', 'debit_card', 'PAYMENT_PENDING', NULL, '2026-01-18 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174033');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174034', 'CUST035', 225.9900, 'USD', 'credit_card', 'PAID', NULL, '2026-01-18 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174034');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174035', 'CUST036', 450.0000, 'USD', 'bank_transfer', 'PAYMENT_PENDING', NULL, '2026-01-19 08:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174035');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174036', 'CUST037', 350.7500, 'BRL', 'credit_card', 'PAID', NULL, '2026-01-19 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174036');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174037', 'CUST038', 125.0000, 'USD', 'debit_card', 'CREATED', NULL, '2026-01-20 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174037');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174038', 'CUST039', 675.5000, 'USD', 'credit_card', 'PAID', NULL, '2026-01-20 11:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174038');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174039', 'CUST040', 325.9900, 'USD', 'bank_transfer', 'FAILED', NULL, '2026-01-21 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174039');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174040', 'CUST041', 500.0000, 'BRL', 'credit_card', 'PAYMENT_PENDING', NULL, '2026-01-21 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174040');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174041', 'CUST042', 175.5000, 'USD', 'debit_card', 'PAID', NULL, '2026-01-22 09:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174041');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174042', 'CUST043', 425.9900, 'USD', 'credit_card', 'PAYMENT_PENDING', NULL, '2026-01-22 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174042');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174043', 'CUST044', 300.0000, 'USD', 'bank_transfer', 'PAID', NULL, '2026-01-23 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174043');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174044', 'CUST045', 550.5000, 'BRL', 'credit_card', 'PAYMENT_PENDING', NULL, '2026-01-23 10:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174044');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174045', 'CUST046', 200.7500, 'USD', 'debit_card', 'PAID', NULL, '2026-01-24 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174045');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174046', 'CUST047', 475.9900, 'USD', 'credit_card', 'FAILED', NULL, '2026-01-24 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174046');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174047', 'CUST048', 375.0000, 'USD', 'bank_transfer', 'PAYMENT_PENDING', NULL, '2026-01-25 08:30:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174047');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174048', 'CUST049', 625.5000, 'BRL', 'credit_card', 'PAID', NULL, '2026-01-25 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174048');

INSERT INTO orders (id, customer_id, total_amount, currency, payment_method, status, payment_id, created_at)
SELECT '123e4567-e89b-12d3-a456-426614174049', 'CUST050', 225.9900, 'USD', 'debit_card', 'CREATED', NULL, '2026-01-25 12:00:00'
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE id = '123e4567-e89b-12d3-a456-426614174049');
