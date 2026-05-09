-- ============================================================================
-- V2__seed_data.sql
-- Dữ liệu mẫu cho fee_rules, transaction_limits, bill_providers
-- Phục vụ cho test bảng quyết định và biên giá trị.
-- ============================================================================

-- ----- Hạn mức giao dịch theo hạng -----
INSERT INTO transaction_limits (tier, tx_type, per_transaction, per_day, per_month, daily_count_limit) VALUES
  ('BRONZE',   'TOPUP',         5000000,    20000000,   100000000, 10),
  ('BRONZE',   'WITHDRAW',      5000000,    10000000,    50000000, 5),
  ('BRONZE',   'TRANSFER',      5000000,    10000000,    50000000, 10),
  ('BRONZE',   'BILL_PAYMENT', 10000000,    20000000,   100000000, 20),

  ('SILVER',   'TOPUP',        20000000,    50000000,   500000000, 20),
  ('SILVER',   'WITHDRAW',     20000000,    50000000,   200000000, 10),
  ('SILVER',   'TRANSFER',     20000000,    50000000,   200000000, 20),
  ('SILVER',   'BILL_PAYMENT', 50000000,   100000000,   500000000, 50),

  ('GOLD',     'TOPUP',        50000000,   200000000,  1000000000, 50),
  ('GOLD',     'WITHDRAW',     50000000,   100000000,   500000000, 20),
  ('GOLD',     'TRANSFER',     50000000,   100000000,   500000000, 50),
  ('GOLD',     'BILL_PAYMENT', 100000000,  200000000,  1000000000, 100),

  ('PLATINUM', 'TOPUP',       500000000,  1000000000,  5000000000, 200),
  ('PLATINUM', 'WITHDRAW',    500000000,  1000000000,  5000000000, 100),
  ('PLATINUM', 'TRANSFER',    500000000,  1000000000,  5000000000, 200),
  ('PLATINUM', 'BILL_PAYMENT',500000000,  1000000000,  5000000000, 500);

-- ----- Rule phí (dùng cho bảng quyết định trong test) -----
-- Phí chuyển khoản P2P: miễn phí dưới 500k, tính 0.5% trên ngưỡng
INSERT INTO fee_rules (tx_type, tier, min_amount, max_amount, fee_type, fee_value, fee_min, fee_max) VALUES
  ('TRANSFER', 'BRONZE',          1,    500000, 'FIXED',   0,       0, NULL),
  ('TRANSFER', 'BRONZE',     500001,  10000000, 'PERCENT', 0.0050, 1000, 50000),
  ('TRANSFER', 'SILVER',          1,   1000000, 'FIXED',   0,       0, NULL),
  ('TRANSFER', 'SILVER',    1000001,  20000000, 'PERCENT', 0.0030, 1000, 30000),
  ('TRANSFER', 'GOLD',            1,   5000000, 'FIXED',   0,       0, NULL),
  ('TRANSFER', 'GOLD',      5000001,  50000000, 'PERCENT', 0.0020, 1000, 20000),
  ('TRANSFER', 'PLATINUM',        1, 500000000, 'FIXED',   0,       0, NULL);

-- Phí rút tiền: 5k cố định cho mọi hạng (Bronze, Silver), miễn phí Gold/Platinum
INSERT INTO fee_rules (tx_type, tier, min_amount, max_amount, fee_type, fee_value, fee_min, fee_max) VALUES
  ('WITHDRAW', 'BRONZE',          1, 5000000, 'FIXED', 5000, 0, NULL),
  ('WITHDRAW', 'SILVER',          1, 20000000, 'FIXED', 5000, 0, NULL),
  ('WITHDRAW', 'GOLD',            1, 50000000, 'FIXED', 0,    0, NULL),
  ('WITHDRAW', 'PLATINUM',        1, 500000000, 'FIXED', 0,    0, NULL);

-- ----- Nhà cung cấp hoá đơn -----
INSERT INTO bill_providers (code, name, service_type) VALUES
  ('EVN_HN',   'Tổng công ty Điện lực Hà Nội',     'ELECTRICITY'),
  ('EVN_HCM',  'Tổng công ty Điện lực TP.HCM',     'ELECTRICITY'),
  ('HAWACO',   'Công ty cổ phần nước sạch Hà Nội', 'WATER'),
  ('SAWACO',   'Tổng công ty cấp nước Sài Gòn',    'WATER'),
  ('VNPT',     'VNPT',                              'INTERNET'),
  ('VIETTEL',  'Viettel Telecom',                   'INTERNET'),
  ('FPT',      'FPT Telecom',                       'INTERNET'),
  ('MOBIFONE', 'MobiFone',                          'TELCO'),
  ('VINAPHONE','Vinaphone',                         'TELCO');
