-- StockPulse demo seed based on Addendum A in stockpulse-brief 1 (1).html.
-- Uses the current UUID/JPA schema and leaves any existing SKU untouched.

INSERT INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, version)
SELECT CAST('10000000-0000-4000-8000-000000000001' AS UUID), 'SKU-ELEC-001', 'Wireless Earbuds Pro', 'ELECTRONICS', 79.99, 45, 20, 3, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-ELEC-001');

INSERT INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, version)
SELECT CAST('10000000-0000-4000-8000-000000000002' AS UUID), 'SKU-ELEC-002', 'USB-C Hub 7-Port', 'ELECTRONICS', 34.99, 120, 30, 1, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-ELEC-002');

INSERT INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, version)
SELECT CAST('10000000-0000-4000-8000-000000000003' AS UUID), 'SKU-APP-001', 'Organic Cotton T-Shirt', 'APPAREL', 24.99, 8, 15, 12, 'PRICE_REVIEW_PENDING', 0
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-APP-001');

INSERT INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, version)
SELECT CAST('10000000-0000-4000-8000-000000000004' AS UUID), 'SKU-APP-002', 'Running Shorts - Navy', 'APPAREL', 39.99, 55, 20, 2, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-APP-002');

INSERT INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, version)
SELECT CAST('10000000-0000-4000-8000-000000000005' AS UUID), 'SKU-HOME-001', 'Ceramic Pour-Over Set', 'HOME', 49.99, 22, 10, 4, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-HOME-001');

INSERT INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, version)
SELECT CAST('10000000-0000-4000-8000-000000000006' AS UUID), 'SKU-HOME-002', 'LED Desk Lamp - Dimmable', 'HOME', 59.99, 0, 15, 0, 'OUT_OF_STOCK', 0
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-HOME-002');

INSERT INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, version)
SELECT CAST('10000000-0000-4000-8000-000000000007' AS UUID), 'SKU-ELEC-003', 'Portable Charger 20K', 'ELECTRONICS', 44.99, 18, 25, 8, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-ELEC-003');

INSERT INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, version)
SELECT CAST('10000000-0000-4000-8000-000000000008' AS UUID), 'SKU-APP-003', 'Hoodie - Heather Grey', 'APPAREL', 54.99, 11, 12, 15, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-APP-003');
