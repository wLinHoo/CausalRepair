#!/usr/bin/env bash
# Run the full CausalRepair pipeline with the paper's settings.
#
# Stages: iterative repair (5 rounds x 3 attempts) -> collect plausible patches
#         -> patch augmentation (10 attempts).
#
# Requires an API key in the environment (SILICONFLOW_API_KEY by default).
# Results are written under $RESULTS_DIR (default /results; mount a host dir).
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/causalrepair}"
OUT="${RESULTS_DIR:-/results}"
PROVIDER="${CAUSALREPAIR_PROVIDER:-siliconflow}"
MODEL="${CAUSALREPAIR_MODEL:-deepseek-ai/DeepSeek-V3}"

# Repair budget (paper settings; override via env if desired).
D4J_VERSION="${D4J_VERSION:-all}"        # 1.2 | 2.0 | all
MAX_ROUNDS="${MAX_ROUNDS:-5}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-3}"
AUG_ATTEMPTS="${AUG_ATTEMPTS:-10}"

if [[ -z "${SILICONFLOW_API_KEY:-}" && -z "${OPENAI_API_KEY:-}" && -z "${ZHIPU_API_KEY:-}" ]]; then
    echo "ERROR: no API key set. Provide one, e.g.:" >&2
    echo "  docker run -e SILICONFLOW_API_KEY=sk-... ... run" >&2
    exit 1
fi

mkdir -p "$OUT"

echo "============================================================"
echo " Stage 1/3: Iterative repair (rounds=$MAX_ROUNDS attempts=$MAX_ATTEMPTS d4j=$D4J_VERSION)"
echo "============================================================"
python "$APP_DIR/iterative_repair.py" \
    --provider "$PROVIDER" --model "$MODEL" \
    --folder "$OUT" \
    --d4j_version "$D4J_VERSION" \
    --max_rounds "$MAX_ROUNDS" \
    --max_attempts_per_round "$MAX_ATTEMPTS" \
    --use_slice

echo "============================================================"
echo " Stage 2/3: Collect plausible patches"
echo "============================================================"
python "$APP_DIR/collect_plausible_patches.py" \
    --input_file "$OUT/repair_result_iterative.json" \
    --output_file "$OUT/plausible_patches.json"

echo "============================================================"
echo " Stage 3/3: Patch augmentation (attempts=$AUG_ATTEMPTS)"
echo "============================================================"
python "$APP_DIR/augment_patches.py" \
    --input_plausible "$OUT/plausible_patches.json" \
    --output_folder "$OUT/augmentation" \
    --d4j_version "$D4J_VERSION" \
    --max_attempts "$AUG_ATTEMPTS"

echo
echo "Done. Results written to: $OUT"
