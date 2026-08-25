#!/usr/bin/env bash
set -euo pipefail

echo "Running test suite..."

failures=0

echo "Checking repository structure..."
for required in README.md .gitignore tests/run.sh; do
  if [[ -f "$required" ]] || [[ -d "$required" ]]; then
    echo "  [OK] $required exists"
  else
    echo "  [FAIL] $required missing"
    failures=$((failures + 1))
  fi
done

echo "Checking git repo state..."
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "  [OK] inside a git work tree"
else
  echo "  [FAIL] not a git repository"
  failures=$((failures + 1))
fi

if [[ "$failures" -eq 0 ]]; then
  echo "All tests passed."
else
  echo "$failures check(s) failed."
  exit 1
fi
