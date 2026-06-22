#!/usr/bin/env bash
set -u

# Read-only consistency snapshot. This script never mutates DB data.

MEMBER_ID="${MEMBER_ID:-}"
MARKET_ID="${MARKET_ID:-}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-todongsan-mysql}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-1234}"
MARKET_DB="${MARKET_DB:-market}"
MEMBERPOINT_DB="${MEMBERPOINT_DB:-memberpoint}"
REPORT_DIR="${REPORT_DIR:-reports/market-debug}"

usage() {
  echo "Usage: $0 --member-id ID --market-id ID"
  echo "   or: MEMBER_ID=ID MARKET_ID=ID $0"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --member-id) MEMBER_ID="${2:-}"; shift 2 ;;
    --market-id) MARKET_ID="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

if [[ ! "$MEMBER_ID" =~ ^[1-9][0-9]*$ ]] || [[ ! "$MARKET_ID" =~ ^[1-9][0-9]*$ ]]; then
  echo "MEMBER_ID and MARKET_ID must be positive integers." >&2
  usage >&2
  exit 2
fi

mkdir -p "$REPORT_DIR"
timestamp="$(date '+%Y%m%d-%H%M%S')"
report="$REPORT_DIR/consistency-member-${MEMBER_ID}-market-${MARKET_ID}-${timestamp}.md"

cat >"$report" <<EOF
# Market / Member-Point Consistency Snapshot

- Captured at: $(date -Iseconds)
- Member ID: $MEMBER_ID
- Market ID: $MARKET_ID
- MySQL container: $MYSQL_CONTAINER
- Schemas: $MARKET_DB, $MEMBERPOINT_DB

This report was produced with read-only SELECT statements.
EOF

run_query() {
  local title="$1"
  local sql="$2"
  local heading="${3:-##}"
  local output status

  output="$(docker exec -e "MYSQL_PWD=$MYSQL_PASSWORD" -i "$MYSQL_CONTAINER" \
    mysql -u"$MYSQL_USER" --batch --raw --table -e "$sql" 2>&1)"
  status=$?

  {
    printf '\n%s %s\n\n' "$heading" "$title"
    printf '```text\n'
    if [[ -n "$output" ]]; then
      printf '%s\n' "$output"
    elif [[ $status -eq 0 ]]; then
      printf '(no rows)\n'
    else
      printf '(query failed without output)\n'
    fi
    printf '```\n'
    if [[ $status -ne 0 ]]; then
      printf '\nQuery failed (exit code %s).\n' "$status"
    fi
  } >>"$report"
}

if ! command -v docker >/dev/null 2>&1; then
  printf '\n## Execution error\n\n`docker` is not available. No queries were run.\n' >>"$report"
  echo "Report written with error: $report" >&2
  exit 1
fi

printf '\n## Environment diagnostics\n' >>"$report"

run_query "Database clock" "SELECT NOW();" "###"

run_query "Market row count" "SELECT COUNT(*) FROM ${MARKET_DB}.market;" "###"

run_query "Seed-range Market row count" "SELECT COUNT(*) FROM ${MARKET_DB}.market WHERE id BETWEEN 900001 AND 900099;" "###"

run_query "Market Prediction row count" "SELECT COUNT(*) FROM ${MARKET_DB}.market_prediction;" "###"

run_query "Member row count" "SELECT COUNT(*) FROM ${MEMBERPOINT_DB}.member;" "###"

run_query "Point History row count" "SELECT COUNT(*) FROM ${MEMBERPOINT_DB}.point_history;" "###"

run_query "Member balance" "SELECT id, point_balance, created_at, updated_at FROM ${MEMBERPOINT_DB}.member WHERE id = ${MEMBER_ID};"

run_query "Recent point history" "SELECT id, member_id, type, amount, balance_snapshot, reference_type, reference_id, idempotency_key, status, fail_reason, created_at, updated_at FROM ${MEMBERPOINT_DB}.point_history WHERE member_id = ${MEMBER_ID} ORDER BY id DESC LIMIT 20;"

run_query "Recent Market predictions for member" "SELECT id, market_id, option_id, member_id, point_amount, status, point_spend_idempotency_key, attempt_no, price_snapshot, contract_quantity, fail_reason, created_at, updated_at FROM ${MARKET_DB}.market_prediction WHERE member_id = ${MEMBER_ID} ORDER BY id DESC LIMIT 20;"

run_query "Predictions for member and Market" "SELECT id, market_id, option_id, member_id, point_amount, status, point_spend_idempotency_key, attempt_no, price_snapshot, contract_quantity, fail_reason, created_at, updated_at FROM ${MARKET_DB}.market_prediction WHERE member_id = ${MEMBER_ID} AND market_id = ${MARKET_ID} ORDER BY id DESC;"

run_query "Market state" "SELECT id, title, status, total_pool, created_at, updated_at FROM ${MARKET_DB}.market WHERE id = ${MARKET_ID};"

run_query "Market options" "SELECT id, market_id, option_code, option_text, real_pool_amount, total_contract_quantity, current_price, prediction_count, created_at, updated_at FROM ${MARKET_DB}.market_option WHERE market_id = ${MARKET_ID} ORDER BY display_order, id;"

run_query "Recent Market price history" "SELECT id, market_id, option_id, prediction_id, price_before, price_after, real_pool_before, real_pool_after, event_type, created_at FROM ${MARKET_DB}.market_price_history WHERE market_id = ${MARKET_ID} ORDER BY id DESC LIMIT 20;"

echo "Report written: $report"
