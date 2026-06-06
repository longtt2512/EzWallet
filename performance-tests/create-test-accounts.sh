#!/bin/bash

# Script tạo 5 test accounts cho performance testing
# Chạy script này khi backend đang chạy

API_URL="http://localhost:8080/api/v1"

echo "========================================="
echo "Creating 5 Test Accounts for Performance Testing"
echo "========================================="
echo ""

# Array of test users
declare -a emails=("user1@test.com" "user2@test.com" "user3@test.com" "user4@test.com" "user5@test.com")
declare -a phones=("0901111111" "0901111112" "0901111113" "0901111114" "0901111115")
declare -a names=("Test User 1" "Test User 2" "Test User 3" "Test User 4" "Test User 5")

PASSWORD="Password123!"

# Function to register a user
register_user() {
    local email=$1
    local phone=$2
    local name=$3
    local username=$(echo "$email" | cut -d'@' -f1)  # Extract username from email

    echo "Registering: $email"

    response=$(curl -s -X POST "$API_URL/auth/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"$username\",
            \"email\": \"$email\",
            \"password\": \"$PASSWORD\",
            \"phone\": \"$phone\",
            \"fullName\": \"$name\"
        }")

    if echo "$response" | grep -q "success"; then
        echo "✓ Registered: $email"
        return 0
    else
        echo "✗ Failed to register: $email"
        echo "  Response: $response"
        return 1
    fi
}

# Function to verify account with OTP
verify_account() {
    local email=$1

    echo "  Verifying account: $email (using OTP: 123456)"

    response=$(curl -s -X POST "$API_URL/auth/otp/verify" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"$email\",
            \"otp\": \"123456\",
            \"purpose\": \"REGISTER\"
        }")

    if echo "$response" | grep -q "success"; then
        echo "  ✓ Verified: $email"
        return 0
    else
        echo "  ✗ Failed to verify: $email"
        echo "    Response: $response"
        return 1
    fi
}

# Check if backend is running
if ! curl -s "$API_URL/actuator/health" > /dev/null; then
    echo "ERROR: Backend is not running at $API_URL"
    echo "Start backend with: make dev-perf"
    exit 1
fi

echo "Backend is running. Starting account creation..."
echo ""

# Create accounts
for i in {0..4}; do
    echo "[$((i+1))/5] Creating account..."
    register_user "${emails[$i]}" "${phones[$i]}" "${names[$i]}"

    # Wait 2 seconds for OTP to be generated
    sleep 2

    # Verify account
    verify_account "${emails[$i]}"

    echo ""
    sleep 1
done

echo "========================================="
echo "Account Creation Complete!"
echo "========================================="
echo ""
echo "Created accounts:"
for email in "${emails[@]}"; do
    echo "  - $email / $PASSWORD"
done
echo ""
echo "Note: You may need to manually top-up balance for these accounts"
echo "      or run SQL script to add initial balance."
