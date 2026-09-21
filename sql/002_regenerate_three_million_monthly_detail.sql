-- WARNING: this script deletes all existing largedata.detail rows.
-- It recreates exactly 3,000,000 detail rows from 2026-01-01 through 2026-12-31.
-- Each calendar month has 20 to 36 actual Model Families.

TRUNCATE TABLE largedata.detail;

WITH generated AS (
    SELECT id,
           DATE '2026-01-01' + ((id - 1) % 365)::INTEGER AS report_date
    FROM generate_series(1, 3000000) AS series(id)
)
INSERT INTO largedata.detail (
    id, product_group, component_code, qty, amount, created_at, report_date, region_code, country_code,
    facility_code, customer_code, order_no, lot_no, serial_no, status, priority, sales_channel,
    currency_code, unit_price, discount_amount, tax_amount, net_amount, shipped_qty,
    pending_qty, is_active, updated_at
)
SELECT id,
       'Family_' || ((id - 1) % (18 + ((EXTRACT(MONTH FROM report_date)::INTEGER * 3) % 19)) + 1),
       'Component_' || ((id - 1) % 200 + 1),
       ((id - 1) % 10 + 1),
       ((id % 100000)::NUMERIC / 100),
       report_date + ((id % 86400) * INTERVAL '1 second'),
       report_date,
       CASE ((id - 1) % 5)::INTEGER
           WHEN 0 THEN (ARRAY['TW-TPE', 'TW-NWT', 'TW-TXG', 'TW-KHH', 'TW-TNN', 'TW-HSZ'])[((id - 1) % 6 + 1)::INTEGER]
           WHEN 1 THEN (ARRAY['JP-13', 'JP-27', 'JP-23', 'JP-01', 'JP-40', 'JP-14'])[((id - 1) % 6 + 1)::INTEGER]
           WHEN 2 THEN (ARRAY['US-CA', 'US-NY', 'US-TX', 'US-WA', 'US-IL', 'US-MA'])[((id - 1) % 6 + 1)::INTEGER]
           WHEN 3 THEN (ARRAY['DE-BE', 'DE-BY', 'DE-NW', 'DE-HE', 'DE-HH', 'DE-SN'])[((id - 1) % 6 + 1)::INTEGER]
           ELSE (ARRAY['SG-01', 'SG-02', 'SG-03', 'SG-04', 'SG-05', 'SG-06'])[((id - 1) % 6 + 1)::INTEGER]
       END,
       (ARRAY['TW', 'JP', 'US', 'DE', 'SG'])[((id - 1) % 5 + 1)::INTEGER],
       'FACILITY-' || LPAD((((id - 1) % 12 + 1)::TEXT), 2, '0'),
       'CUST-' || LPAD((((id - 1) % 5000 + 1)::TEXT), 5, '0'),
       'ORD-2026-' || LPAD(id::TEXT, 8, '0'),
       'LOT-' || LPAD((((id - 1) % 10000 + 1)::TEXT), 5, '0'),
       'SN-' || LPAD(id::TEXT, 10, '0'),
       (ARRAY['NEW', 'ALLOCATED', 'SHIPPED', 'CLOSED'])[((id - 1) % 4 + 1)::INTEGER],
       ((id - 1) % 5 + 1)::SMALLINT,
       (ARRAY['DIRECT', 'PARTNER', 'ONLINE'])[((id - 1) % 3 + 1)::INTEGER],
       (ARRAY['TWD', 'JPY', 'USD', 'EUR', 'SGD'])[((id - 1) % 5 + 1)::INTEGER],
       ((id % 100000)::NUMERIC / 100) + 10.00,
       ROUND(((id % 100000)::NUMERIC / 100) * 0.05, 2),
       ROUND((((id % 100000)::NUMERIC / 100) - ROUND(((id % 100000)::NUMERIC / 100) * 0.05, 2)) * 0.05, 2),
       ((id % 100000)::NUMERIC / 100) - ROUND(((id % 100000)::NUMERIC / 100) * 0.05, 2)
           + ROUND((((id % 100000)::NUMERIC / 100) - ROUND(((id % 100000)::NUMERIC / 100) * 0.05, 2)) * 0.05, 2),
       GREATEST(((id - 1) % 10 + 1) - (id % 3)::INTEGER, 0),
       (id % 4)::INTEGER,
       (id % 10) <> 0,
       report_date + ((id % 90000) * INTERVAL '1 second')
FROM generated;

ANALYZE largedata.detail;
