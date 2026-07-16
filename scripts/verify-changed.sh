#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_REF="${1:-main}"

cd "$ROOT_DIR"

if git rev-parse --verify "$BASE_REF" >/dev/null 2>&1; then
  MERGE_BASE="$(git merge-base HEAD "$BASE_REF")"
  CHANGED_FILES="$(git diff --name-only "$MERGE_BASE" HEAD; git diff --name-only; git diff --name-only --cached; git ls-files --others --exclude-standard)"
else
  CHANGED_FILES="$(git status --short | sed 's/^...//')"
fi

if [[ -z "${CHANGED_FILES//[[:space:]]/}" ]]; then
  echo "No changed files detected."
  exit 0
fi

echo "Changed files:"
printf '%s\n' "$CHANGED_FILES" | sort -u

RUN_BACKEND=false
RUN_FRONTEND=false

if printf '%s\n' "$CHANGED_FILES" | grep -Eq '^(backend/|pom\.xml$)'; then
  RUN_BACKEND=true
fi

if printf '%s\n' "$CHANGED_FILES" | grep -Eq '^(frontend/|package(-lock)?\.json$)'; then
  RUN_FRONTEND=true
fi

if [[ "$RUN_BACKEND" == true ]]; then
  "$ROOT_DIR/scripts/verify-backend.sh"
fi

if [[ "$RUN_FRONTEND" == true ]]; then
  "$ROOT_DIR/scripts/verify-frontend.sh"
fi

if [[ "$RUN_BACKEND" == false && "$RUN_FRONTEND" == false ]]; then
  echo "Only documentation or project metadata changed; no build verification required."
fi
