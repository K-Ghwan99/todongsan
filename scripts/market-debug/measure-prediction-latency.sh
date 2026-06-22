#!/usr/bin/env bash
set -u

# WARNING: This script sends a real prediction POST and can deduct real points.
# It requires CONFIRM_DEDUCT_POINTS=YES. Dev test-range Markets are blocked
# unless ALLOW_DEV_TEST_RANGE=true is explicitly set in an add-only local setup.

MEMBER_ID="${MEMBER_ID:-}"
MARKET_ID="${MARKET_ID:-}"
OPTION_ID="${OPTION_ID:-}"
POINT_AMOUNT="${POINT_AMOUNT:-10}"
API_BASE_URL="${API_BASE_URL:-http://localhost:8082}"
GATEWAY_BASE_URL="${GATEWAY_BASE_URL:-}"
POINT_BASE_URL="${POINT_BASE_URL:-${GATEWAY_BASE_URL:-http://localhost:8080}}"
AUTH_TOKEN="${AUTH_TOKEN:-}"
MEMBER_ROLE="${MEMBER_ROLE:-USER}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-todongsan-mysql}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-1234}"
MARKET_DB="${MARKET_DB:-market}"
MEMBERPOINT_DB="${MEMBERPOINT_DB:-memberpoint}"
REPORT_DIR="${REPORT_DIR:-reports/market-debug}"
CONFIRM_DEDUCT_POINTS="${CONFIRM_DEDUCT_POINTS:-}"
ALLOW_DEV_TEST_RANGE="${ALLOW_DEV_TEST_RANGE:-false}"

usage() {
  echo "Usage: CONFIRM_DEDUCT_POINTS=YES MEMBER_ID=1 MARKET_ID=123 OPTION_ID=456 $0"
  echo "Optional: POINT_AMOUNT, API_BASE_URL, GATEWAY_BASE_URL, POINT_BASE_URL, AUTH_TOKEN, ALLOW_DEV_TEST_RANGE"
}

for value in "$MEMBER_ID" "$MARKET_ID" "$OPTION_ID"; do
  if [[ ! "$value" =~ ^[1-9][0-9]*$ ]]; then
    echo "MEMBER_ID, MARKET_ID and OPTION_ID must be positive integers." >&2
    usage >&2
    exit 2
  fi
done
if [[ ! "$POINT_AMOUNT" =~ ^[0-9]+([.][0-9]{1,2})?$ ]] || [[ "$POINT_AMOUNT" == "0" || "$POINT_AMOUNT" == "0.0" || "$POINT_AMOUNT" == "0.00" ]]; then
  echo "POINT_AMOUNT must be a positive number with at most two decimal places." >&2
  exit 2
fi

if [[ "$CONFIRM_DEDUCT_POINTS" != "YES" ]]; then
  echo "Execution blocked: set CONFIRM_DEDUCT_POINTS=YES to acknowledge the real point deduction." >&2
  exit 3
fi

if (( MARKET_ID >= 900001 && MARKET_ID <= 900099 )) && [[ "$ALLOW_DEV_TEST_RANGE" != "true" ]]; then
  echo "Execution blocked: Market ID $MARKET_ID is in the dev seed range 900001-900099." >&2
  echo "Use ALLOW_DEV_TEST_RANGE=true only with add-only seed and full-volume reset policy." >&2
  exit 4
fi

mkdir -p "$REPORT_DIR"
timestamp="$(date '+%Y%m%d-%H%M%S')"
report="$REPORT_DIR/latency-member-${MEMBER_ID}-market-${MARKET_ID}-${timestamp}.md"
idempotency_key="MARKET_PREDICTION_SPEND:market:${MARKET_ID}:member:${MEMBER_ID}"

cat >"$report" <<EOF
# Prediction Reflection Latency Report

> WARNING: This run sends a real prediction request and may deduct $POINT_AMOUNT points.

- Started at: $(date -Iseconds)
- Member ID: $MEMBER_ID
- Market / option: $MARKET_ID / $OPTION_ID
- API base: $API_BASE_URL
- Point base: $POINT_BASE_URL
- Idempotency key: $idempotency_key
- Dev test-range override: $ALLOW_DEV_TEST_RANGE
EOF

common_headers=(-H "X-Member-Id: $MEMBER_ID" -H "X-Member-Role: $MEMBER_ROLE")
if [[ -n "$AUTH_TOKEN" ]]; then
  common_headers+=(-H "Authorization: Bearer $AUTH_TOKEN")
fi

record_http() {
  local title="$1"
  local method="$2"
  local url="$3"
  local body="${4:-}"
  local tmp status curl_status
  tmp="$(mktemp)"

  if [[ "$method" == "POST" ]]; then
    status="$(curl -sS -o "$tmp" -w '%{http_code}' -X POST "$url" \
      "${common_headers[@]}" -H "Idempotency-Key: $idempotency_key" \
      -H 'Content-Type: application/json' --data "$body" 2>>"$tmp")"
    curl_status=$?
  else
    status="$(curl -sS -o "$tmp" -w '%{http_code}' "$url" "${common_headers[@]}" 2>>"$tmp")"
    curl_status=$?
  fi

  {
    printf '\n### %s\n\n' "$title"
    printf -- '- Captured: %s\n- HTTP status: %s\n- curl exit: %s\n\n' "$(date -Iseconds)" "${status:-unavailable}" "$curl_status"
    printf '```json\n'
    if [[ -s "$tmp" ]]; then cat "$tmp"; else printf '(empty response)'; fi
    printf '\n```\n'
  } >>"$report"
  rm -f "$tmp"
}

record_db_snapshot() {
  local label="$1"
  local output status sql
  sql="SELECT 'member' AS source, id, point_balance AS value1, updated_at FROM ${MEMBERPOINT_DB}.member WHERE id=${MEMBER_ID}; SELECT 'point_history' AS source, id, CONCAT(type,'/',status,'/',amount) AS value1, updated_at FROM ${MEMBERPOINT_DB}.point_history WHERE member_id=${MEMBER_ID} ORDER BY id DESC LIMIT 3; SELECT 'prediction' AS source, id, CONCAT(status,'/',point_amount,'/',attempt_no) AS value1, updated_at FROM ${MARKET_DB}.market_prediction WHERE member_id=${MEMBER_ID} AND market_id=${MARKET_ID} ORDER BY id DESC; SELECT 'market' AS source, id, total_pool AS value1, updated_at FROM ${MARKET_DB}.market WHERE id=${MARKET_ID}; SELECT 'option' AS source, id, CONCAT(real_pool_amount,'/',current_price,'/',prediction_count) AS value1, updated_at FROM ${MARKET_DB}.market_option WHERE market_id=${MARKET_ID} ORDER BY display_order,id;"

  if ! command -v docker >/dev/null 2>&1; then
    output="docker is not available"
    status=127
  else
    output="$(docker exec -e "MYSQL_PWD=$MYSQL_PASSWORD" -i "$MYSQL_CONTAINER" \
      mysql -u"$MYSQL_USER" --batch --raw --table -e "$sql" 2>&1)"
    status=$?
  fi
  {
    printf '\n### DB snapshot — %s\n\n```text\n%s\n```\n' "$label" "${output:-(no rows)}"
    if [[ $status -ne 0 ]]; then printf '\nDB snapshot failed (exit code %s).\n' "$status"; fi
  } >>"$report"
}

market_url="$API_BASE_URL/api/v1/markets/$MARKET_ID"
my_prediction_url="$API_BASE_URL/api/v1/markets/$MARKET_ID/predictions/me"
point_url="$POINT_BASE_URL/api/v1/points/balance"
post_url="$API_BASE_URL/api/v1/markets/$MARKET_ID/predictions"
request_body="{\"marketOptionId\":${OPTION_ID},\"pointAmount\":\"${POINT_AMOUNT}\"}"

echo "WARNING: sending a real prediction that may deduct $POINT_AMOUNT points." >&2
record_http "Before — Market detail" GET "$market_url"
record_http "Before — My prediction" GET "$my_prediction_url"
record_http "Before — Point balance (may require AUTH_TOKEN/gateway)" GET "$point_url"
record_db_snapshot "before POST"

record_http "Prediction POST" POST "$post_url" "$request_body"

start_epoch="$(date +%s)"
targets=(0 1 3 5 10 30)
for target in "${targets[@]}"; do
  now="$(date +%s)"
  elapsed=$((now - start_epoch))
  if (( elapsed < target )); then sleep $((target - elapsed)); fi

  {
    printf '\n## T+%ss\n' "$target"
  } >>"$report"
  record_http "Market detail" GET "$market_url"
  record_http "My prediction" GET "$my_prediction_url"
  record_http "Point balance (may require AUTH_TOKEN/gateway)" GET "$point_url"
  record_db_snapshot "T+${target}s"
done

echo "Report written: $report"
