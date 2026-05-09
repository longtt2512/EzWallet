-- ============================================================================
-- V3 — Sample bills for manual & automated testing
-- Each provider gets several bills covering all status variants.
-- customer_code follows realistic Vietnamese utility-bill formats:
--   Electricity : 13-digit meter ID  (PE + 11 digits)
--   Water       : 10-digit subscriber ID  (SW + 8 digits)
--   Internet    : contract number  (provider prefix + 8 digits)
--   Telco       : 10-digit mobile number
-- ============================================================================

-- ─── ELECTRICITY — EVN Hà Nội (provider_id resolved by subquery) ───────────
INSERT INTO bills (bill_code, provider_id, customer_code, amount, status, due_date, version) VALUES
  ('EVN-HN-2025-001', (SELECT id FROM bill_providers WHERE code = 'EVN_HN'), 'PE00123456789', 285000,  'UNPAID',    '2025-06-15', 0),
  ('EVN-HN-2025-002', (SELECT id FROM bill_providers WHERE code = 'EVN_HN'), 'PE00987654321', 512000,  'UNPAID',    '2025-06-15', 0),
  ('EVN-HN-2025-003', (SELECT id FROM bill_providers WHERE code = 'EVN_HN'), 'PE00111222333', 134000,  'EXPIRED',   '2025-04-30', 0),
  ('EVN-HN-2025-004', (SELECT id FROM bill_providers WHERE code = 'EVN_HN'), 'PE00444555666', 960000,  'PAID',      '2025-05-15', 0);

-- ─── ELECTRICITY — EVN TP.HCM ───────────────────────────────────────────────
INSERT INTO bills (bill_code, provider_id, customer_code, amount, status, due_date, version) VALUES
  ('EVN-HCM-2025-001', (SELECT id FROM bill_providers WHERE code = 'EVN_HCM'), 'PE00777888999', 378000,  'UNPAID',    '2025-06-20', 0),
  ('EVN-HCM-2025-002', (SELECT id FROM bill_providers WHERE code = 'EVN_HCM'), 'PE00321654987', 1250000, 'UNPAID',    '2025-06-20', 0),
  ('EVN-HCM-2025-003', (SELECT id FROM bill_providers WHERE code = 'EVN_HCM'), 'PE00112233445', 89000,   'CANCELLED', '2025-05-01', 0);

-- ─── WATER — HAWACO Hà Nội ──────────────────────────────────────────────────
INSERT INTO bills (bill_code, provider_id, customer_code, amount, status, due_date, version) VALUES
  ('HAWACO-2025-001', (SELECT id FROM bill_providers WHERE code = 'HAWACO'), 'SW01234567', 156000, 'UNPAID',  '2025-06-25', 0),
  ('HAWACO-2025-002', (SELECT id FROM bill_providers WHERE code = 'HAWACO'), 'SW07654321', 312000, 'UNPAID',  '2025-06-25', 0),
  ('HAWACO-2025-003', (SELECT id FROM bill_providers WHERE code = 'HAWACO'), 'SW09988776', 78000,  'EXPIRED', '2025-04-25', 0);

-- ─── WATER — SAWACO TP.HCM ──────────────────────────────────────────────────
INSERT INTO bills (bill_code, provider_id, customer_code, amount, status, due_date, version) VALUES
  ('SAWACO-2025-001', (SELECT id FROM bill_providers WHERE code = 'SAWACO'), 'SW08765432', 245000, 'UNPAID', '2025-06-28', 0),
  ('SAWACO-2025-002', (SELECT id FROM bill_providers WHERE code = 'SAWACO'), 'SW02345678', 189000, 'PAID',   '2025-05-28', 0);

-- ─── INTERNET — VNPT ────────────────────────────────────────────────────────
INSERT INTO bills (bill_code, provider_id, customer_code, amount, status, due_date, version) VALUES
  ('VNPT-2025-001', (SELECT id FROM bill_providers WHERE code = 'VNPT'), 'VNPT12345678', 220000, 'UNPAID',  '2025-06-30', 0),
  ('VNPT-2025-002', (SELECT id FROM bill_providers WHERE code = 'VNPT'), 'VNPT87654321', 330000, 'UNPAID',  '2025-06-30', 0),
  ('VNPT-2025-003', (SELECT id FROM bill_providers WHERE code = 'VNPT'), 'VNPT11223344', 165000, 'EXPIRED', '2025-05-01', 0);

-- ─── INTERNET — Viettel ─────────────────────────────────────────────────────
INSERT INTO bills (bill_code, provider_id, customer_code, amount, status, due_date, version) VALUES
  ('VTT-2025-001', (SELECT id FROM bill_providers WHERE code = 'VIETTEL'), 'VTT00112233', 199000, 'UNPAID', '2025-06-30', 0),
  ('VTT-2025-002', (SELECT id FROM bill_providers WHERE code = 'VIETTEL'), 'VTT00998877', 299000, 'PAID',   '2025-05-30', 0);

-- ─── INTERNET — FPT ─────────────────────────────────────────────────────────
INSERT INTO bills (bill_code, provider_id, customer_code, amount, status, due_date, version) VALUES
  ('FPT-2025-001', (SELECT id FROM bill_providers WHERE code = 'FPT'), 'FPT56789012', 240000, 'UNPAID',    '2025-06-30', 0),
  ('FPT-2025-002', (SELECT id FROM bill_providers WHERE code = 'FPT'), 'FPT98765432', 180000, 'CANCELLED', '2025-05-15', 0);

-- ─── TELCO — MobiFone ───────────────────────────────────────────────────────
INSERT INTO bills (bill_code, provider_id, customer_code, amount, status, due_date, version) VALUES
  ('MBF-2025-001', (SELECT id FROM bill_providers WHERE code = 'MOBIFONE'), '0901234567', 100000, 'UNPAID', '2025-07-01', 0),
  ('MBF-2025-002', (SELECT id FROM bill_providers WHERE code = 'MOBIFONE'), '0912345678', 200000, 'UNPAID', '2025-07-01', 0),
  ('MBF-2025-003', (SELECT id FROM bill_providers WHERE code = 'MOBIFONE'), '0923456789', 50000,  'PAID',   '2025-06-01', 0);

-- ─── TELCO — Vinaphone ──────────────────────────────────────────────────────
INSERT INTO bills (bill_code, provider_id, customer_code, amount, status, due_date, version) VALUES
  ('VNP-2025-001', (SELECT id FROM bill_providers WHERE code = 'VINAPHONE'), '0981234567', 150000, 'UNPAID',  '2025-07-01', 0),
  ('VNP-2025-002', (SELECT id FROM bill_providers WHERE code = 'VINAPHONE'), '0987654321', 300000, 'EXPIRED', '2025-04-01', 0);
