#!/bin/bash

# Cleanup Performance Test Data Script
# This script removes all performance test data and resets accounts

echo "========================================="
echo "Performance Test Data Cleanup"
echo "========================================="
echo ""

# Check if PostgreSQL container is running
if ! docker ps | grep ezwallet-postgres > /dev/null; then
    echo "ERROR: PostgreSQL container is not running"
    echo "Start with: make up"
    exit 1
fi

echo "This script will:"
echo "  1. Delete all transactions from test accounts"
echo "  2. Delete all QR codes from test accounts"
echo "  3. Delete all OTP tokens from test accounts"
echo "  4. Reset wallet balances to 50,000,000 VND"
echo ""

read -p "Are you sure you want to continue? (y/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cleanup cancelled."
    exit 0
fi

echo ""
echo "Running cleanup script..."
echo ""

# Run SQL cleanup script
docker exec -i ezwallet-postgres psql -U ezwallet -d ezwallet < cleanup-performance-data.sql

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Cleanup completed successfully!"
    echo ""
    echo "Test accounts are ready for the next performance test run."
    echo "You can now run: cd jmeter && ./run-all-tests.sh"
else
    echo ""
    echo "✗ Cleanup failed!"
    echo "Check the error messages above."
    exit 1
fi
