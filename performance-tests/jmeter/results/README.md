# Performance Test Results Directory

This directory stores JMeter test results including:

- `*.jtl` - Raw test result files (JTL format)
- `*-html-*/` - HTML dashboard reports with charts and statistics
- `*-summary.csv` - Summary statistics from tests

## Generated Reports

Each test run creates timestamped results:
- `transfer-YYYYMMDD_HHMMSS.jtl` and `transfer-html-YYYYMMDD_HHMMSS/`
- `qr-YYYYMMDD_HHMMSS.jtl` and `qr-html-YYYYMMDD_HHMMSS/`
- `transaction-history-YYYYMMDD_HHMMSS.jtl` and `transaction-history-html-YYYYMMDD_HHMMSS/`

## Viewing Results

Open the HTML reports in a browser:
```bash
open transfer-html-YYYYMMDD_HHMMSS/index.html
```

Or from project root:
```bash
cd performance-tests/jmeter/results
open */index.html
```

## Cleanup

To remove old test results:
```bash
# Remove results older than 7 days
find . -name "*.jtl" -mtime +7 -delete
find . -name "*-html-*" -type d -mtime +7 -exec rm -rf {} +
```
