-- Cleanup Performance Test Data
-- This script removes all transactions created during performance testing
-- and resets test account balances to initial state (50,000,000 VND)

\echo '========================================='
\echo 'Performance Test Data Cleanup'
\echo '========================================='
\echo ''

-- Get test user IDs and wallet IDs
CREATE TEMP TABLE test_user_ids AS
SELECT id FROM users WHERE username IN ('user1', 'user2', 'user3', 'user4', 'user5');

CREATE TEMP TABLE test_wallet_ids AS
SELECT id FROM wallets WHERE user_id IN (SELECT id FROM test_user_ids);

-- Show current state
\echo 'Current state of test accounts:'
SELECT
    u.username,
    u.email,
    w.balance,
    (SELECT COUNT(*) FROM transactions WHERE source_wallet_id = w.id OR target_wallet_id = w.id) as transaction_count
FROM users u
LEFT JOIN wallets w ON u.id = w.user_id
WHERE u.username IN ('user1', 'user2', 'user3', 'user4', 'user5')
ORDER BY u.username;

\echo ''
\echo 'Cleaning up transactions...'

-- Delete transactions from test accounts (source or target)
DELETE FROM transactions
WHERE source_wallet_id IN (SELECT id FROM test_wallet_ids)
   OR target_wallet_id IN (SELECT id FROM test_wallet_ids);

\echo 'Transactions deleted.'
\echo ''

-- Delete OTP tokens
\echo 'Cleaning up OTP tokens...'
DELETE FROM otp_tokens
WHERE user_id IN (SELECT id FROM test_user_ids);

\echo 'OTP tokens deleted.'
\echo ''

-- Reset wallet balances to 50,000,000 VND
\echo 'Resetting wallet balances to 50,000,000 VND...'
UPDATE wallets
SET balance = 50000000.00, updated_at = NOW()
WHERE user_id IN (SELECT id FROM test_user_ids);

\echo 'Balances reset.'
\echo ''

-- Show final state
\echo '========================================='
\echo 'Final state after cleanup:'
\echo '========================================='
SELECT
    u.username,
    u.email,
    u.status,
    w.balance,
    (SELECT COUNT(*) FROM transactions WHERE source_wallet_id = w.id OR target_wallet_id = w.id) as transaction_count
FROM users u
LEFT JOIN wallets w ON u.id = w.user_id
WHERE u.username IN ('user1', 'user2', 'user3', 'user4', 'user5')
ORDER BY u.username;

\echo ''
\echo '========================================='
\echo 'Cleanup Complete!'
\echo '========================================='
\echo 'Test accounts are ready for next performance test run.'

-- Cleanup temp tables
DROP TABLE test_user_ids;
DROP TABLE test_wallet_ids;
