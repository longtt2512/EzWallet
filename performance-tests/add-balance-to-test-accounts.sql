-- Script thêm số dư vào test accounts
-- Chạy script này sau khi đã tạo accounts

-- Thêm 50,000,000 VND vào wallet của từng test user
UPDATE wallets
SET balance = 50000000, updated_at = NOW()
WHERE user_id IN (
    SELECT id FROM users WHERE email IN (
        'user1@test.com',
        'user2@test.com',
        'user3@test.com',
        'user4@test.com',
        'user5@test.com'
    )
);

-- Verify balances
SELECT u.email, u.status, u.tier, w.balance
FROM users u
LEFT JOIN wallets w ON u.id = w.user_id
WHERE u.email LIKE 'user%@test.com'
ORDER BY u.email;
