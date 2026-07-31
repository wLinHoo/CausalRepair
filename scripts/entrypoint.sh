#!/usr/bin/env bash
# Entrypoint dispatcher for the CausalRepair image.
set -euo pipefail
SCRIPTS="/opt/causalrepair/scripts"

usage() {
cat <<'EOF'
CausalRepair

Usage:  docker run --rm -e SILICONFLOW_API_KEY=sk-... \
            -v "$PWD/results:/results" causalrepair:latest <command>

Commands:
  run          Run the full pipeline with the paper settings
               (iterative 5x3 -> collect -> augment 10). Requires an API key.
               Override scope/budget via env: D4J_VERSION (1.2|2.0|all),
               MAX_ROUNDS, MAX_ATTEMPTS, AUG_ATTEMPTS.
  d4j-check    Sanity-check the Defects4J installation (defects4j info -p Lang).
  bash         Open an interactive shell.
  help         Show this message.

Results are written under /results (mount a host dir to keep them).
EOF
}

cmd="${1:-help}"; shift || true
case "$cmd" in
    run)           exec "$SCRIPTS/run.sh" "$@" ;;
    d4j-check)
        echo "Defects4J: $(command -v defects4j)"
        defects4j info -p Lang | head -n 20
        echo "OK: Defects4J is usable."
        ;;
    bash|sh)       exec /bin/bash "$@" ;;
    help|-h|--help) usage ;;
    *)
        echo "Unknown command: $cmd" >&2
        usage >&2
        exit 1
        ;;
esac
