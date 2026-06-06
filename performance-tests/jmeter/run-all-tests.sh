#!/bin/bash

# Run all JMeter performance tests for EzWallet modules
# Usage: ./run-all-tests.sh [threads] [ramp_up] [duration]

THREADS=${1:-50}
RAMP_UP=${2:-30}
DURATION=${3:-300}

echo "========================================="
echo "EzWallet Performance Test Suite"
echo "========================================="
echo "Configuration:"
echo "  Threads: $THREADS"
echo "  Ramp-up: $RAMP_UP seconds"
echo "  Duration: $DURATION seconds"
echo "========================================="

# Check if JMeter is installed
if ! command -v jmeter &> /dev/null; then
    echo "ERROR: JMeter is not installed"
    echo "Install with: brew install jmeter"
    exit 1
fi

# Check if backend is running
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "ERROR: Backend is not running at http://localhost:8080"
    echo "Start with: make dev-perf"
    exit 1
fi

# Check if backend is in PERF_MODE
echo ""
echo "⚠️  IMPORTANT: Make sure backend is running with PERF_MODE=true"
echo "   This ensures all OTPs are 123456 for consistent testing"
echo "   Start with: make dev-perf"
echo ""
read -p "Press ENTER to continue or Ctrl+C to abort..."
echo ""

# Create results directory
mkdir -p results
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo ""
echo "Starting tests at $(date)"
echo ""

# Test 1: Transfer Module
echo "========================================="
echo "Running Transfer Module Performance Test"
echo "========================================="
jmeter -n -t testplans/Transfer-Performance-Test.jmx \
  -JTHREADS=$THREADS \
  -JRAMP_UP=$RAMP_UP \
  -JDURATION=$DURATION \
  -l results/transfer-${TIMESTAMP}.jtl \
  -e -o results/transfer-html-${TIMESTAMP}

if [ $? -eq 0 ]; then
    echo "✓ Transfer test completed successfully"
else
    echo "✗ Transfer test failed"
fi

echo ""
echo "Waiting 30 seconds before next test..."
sleep 30

# Test 2: QR Payment Module
echo "========================================="
echo "Running QR Payment Performance Test"
echo "========================================="
jmeter -n -t testplans/QR-Performance-Test.jmx \
  -JTHREADS=$THREADS \
  -JRAMP_UP=$RAMP_UP \
  -JDURATION=$DURATION \
  -l results/qr-${TIMESTAMP}.jtl \
  -e -o results/qr-html-${TIMESTAMP}

if [ $? -eq 0 ]; then
    echo "✓ QR test completed successfully"
else
    echo "✗ QR test failed"
fi

echo ""
echo "Waiting 30 seconds before next test..."
sleep 30

# Test 3: Transaction History Module
echo "========================================="
echo "Running Transaction History Performance Test"
echo "========================================="
jmeter -n -t testplans/Transaction-History-Performance-Test.jmx \
  -JTHREADS=$THREADS \
  -JRAMP_UP=$RAMP_UP \
  -JDURATION=$DURATION \
  -l results/transaction-history-${TIMESTAMP}.jtl \
  -e -o results/transaction-history-html-${TIMESTAMP}

if [ $? -eq 0 ]; then
    echo "✓ Transaction History test completed successfully"
else
    echo "✗ Transaction History test failed"
fi

echo ""
echo "========================================="
echo "All tests completed at $(date)"
echo "========================================="
echo ""
echo "Results:"
echo "  Transfer:            results/transfer-html-${TIMESTAMP}/index.html"
echo "  QR Payment:          results/qr-html-${TIMESTAMP}/index.html"
echo "  Transaction History: results/transaction-history-html-${TIMESTAMP}/index.html"
echo ""
echo "Open reports with:"
echo "  open results/transfer-html-${TIMESTAMP}/index.html"
echo "  open results/qr-html-${TIMESTAMP}/index.html"
echo "  open results/transaction-history-html-${TIMESTAMP}/index.html"
echo ""
