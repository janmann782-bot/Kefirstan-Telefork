#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_output="$(mktemp -d)"
trap 'rm -rf "$test_output"' EXIT
javac -encoding UTF-8 -d "$test_output" \
    TMessagesProj/src/main/java/com/kefirgram/aurelia/AureliaTextRules.java \
    tests/aurelia/AureliaTextRulesTest.java
java -cp "$test_output" AureliaTextRulesTest
