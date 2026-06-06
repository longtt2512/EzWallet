#!/usr/bin/env python3
"""
Fix JMeter test plans: Change 'email' to 'identifier' in login payloads
"""

import os
import sys

def fix_jmx_file(filepath):
    """Fix login payload in JMeter JMX file"""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Fix 1: Change CSV variable names from "email,password" to "identifier,password"
    content = content.replace(
        '<stringProp name="variableNames">email,password</stringProp>',
        '<stringProp name="variableNames">identifier,password</stringProp>'
    )

    # Fix 2: Change login payload from "email": "${email}" to "identifier": "${identifier}"
    content = content.replace(
        '&quot;email&quot;: &quot;${email}&quot;',
        '&quot;identifier&quot;: &quot;${identifier}&quot;'
    )

    # Write back
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"✓ Fixed: {os.path.basename(filepath)}")

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    testplans_dir = os.path.join(script_dir, 'testplans')

    test_files = [
        'Transfer-Performance-Test.jmx',
        'QR-Performance-Test.jmx',
        'Transaction-History-Performance-Test.jmx'
    ]

    print("Fixing JMeter test plans...")
    print("=" * 50)

    for filename in test_files:
        filepath = os.path.join(testplans_dir, filename)
        if os.path.exists(filepath):
            fix_jmx_file(filepath)
        else:
            print(f"✗ Not found: {filename}")

    print("=" * 50)
    print("✓ All test plans fixed!")
    print("")
    print("Changes made:")
    print("  1. CSV variable: email → identifier")
    print("  2. Login payload: \"email\": \"${email}\" → \"identifier\": \"${identifier}\"")

if __name__ == '__main__':
    main()
