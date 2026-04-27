#!/bin/bash
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO=$(CDPATH= cd -- "$SCRIPT_DIR/../../.." && pwd)
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.thin.yml"
RESULTS=${RESULTS:-/tmp/gateway-warmup-bench-results.tsv}
RUNS=${RUNS:-3}
SETTLE=${SETTLE:-5}
PROJECT_PREFIX=warmgw

cd "$REPO"

if [ ! -d "$REPO/bench-jars" ]; then
  echo "Building host jars into $REPO/bench-jars/..."
  ./gradlew \
    :gateway:api-gateway:bootJar \
    :domains:member:member-bootstrap:bootJar \
    :domains:item:item-bootstrap:bootJar \
    :domains:order:order-bootstrap:bootJar \
    :domains:payment:payment-bootstrap:bootJar \
    --parallel -q
  mkdir -p bench-jars
  cp gateway/api-gateway/build/libs/api-gateway.jar bench-jars/
  cp domains/member/member-bootstrap/build/libs/member-api.jar bench-jars/
  cp domains/item/item-bootstrap/build/libs/item-api.jar bench-jars/
  cp domains/order/order-bootstrap/build/libs/order-api.jar bench-jars/
  cp domains/payment/payment-bootstrap/build/libs/payment-api.jar bench-jars/
fi

run_compose() {
  variant="$1"; shift
  proj="${PROJECT_PREFIX}-${variant}"
  if [ "$variant" = "off" ]; then
    APP_STARTUP_WARMUP_ENABLED=false docker compose --env-file /dev/null -f "$COMPOSE_FILE" -p "$proj" "$@"
  else
    APP_STARTUP_WARMUP_ENABLED=true docker compose --env-file /dev/null -f "$COMPOSE_FILE" -p "$proj" "$@"
  fi
}

wait_healthy() {
  variant="$1"; svc="$2"
  proj="${PROJECT_PREFIX}-${variant}"
  start=$(date +%s)
  while :; do
    state=$(docker inspect -f '{{.State.Health.Status}}' "${proj}-${svc}-1" 2>/dev/null || echo "missing")
    if [ "$state" = "healthy" ]; then return 0; fi
    if [ $(($(date +%s) - start)) -gt 240 ]; then echo "TIMEOUT waiting for ${svc} (state=${state})"; return 1; fi
    sleep 2
  done
}

measure_gateway() {
  variant="$1"
  proj="${PROJECT_PREFIX}-${variant}"
  for spec in \
      "POST|/api/v3/auth/login|{\"email\":\"warmup@warmup.invalid\",\"password\":\"x\"}" \
      "GET|/api/v3/items|" \
      "GET|/api/v3/orders/0|" \
      "GET|/api/v3/wallet/point|"; do
    method=$(echo "$spec" | cut -d'|' -f1)
    url=$(echo "$spec" | cut -d'|' -f2)
    body=$(echo "$spec" | cut -d'|' -f3)
    for i in 1 2 3; do
      if [ "$method" = "POST" ]; then
        t=$(docker exec "${proj}-api-gateway-1" sh -c "curl -s -o /dev/null -w '%{time_total}' -X POST -H 'Content-Type: application/json' --data '$body' http://localhost:8080$url")
      else
        t=$(docker exec "${proj}-api-gateway-1" sh -c "curl -s -o /dev/null -w '%{time_total}' http://localhost:8080$url")
      fi
      printf '%s\t%s\t%s\t%s\n' "$variant" "$url" "$i" "$t"
    done
  done
}

printf 'variant\trun\tpath\treq_idx\ttime_seconds\n' > "$RESULTS"

for variant in on off; do
  echo "== variant=$variant =="
  run_compose "$variant" down -v --remove-orphans >/dev/null 2>&1 || true
  echo "Building images (thin, host jars)..."
  run_compose "$variant" build api-gateway member-api item-api order-api payment-api >/dev/null
  for run in $(seq 1 $RUNS); do
    echo "-- variant=$variant run=$run/$RUNS --"
    run_compose "$variant" down -v --remove-orphans >/dev/null 2>&1 || true
    run_compose "$variant" up -d mysql kafka >/dev/null
    run_compose "$variant" up -d member-api item-api >/dev/null
    wait_healthy "$variant" "member-api" || { run_compose "$variant" logs member-api | tail -50; exit 1; }
    wait_healthy "$variant" "item-api" || { run_compose "$variant" logs item-api | tail -50; exit 1; }
    run_compose "$variant" up -d order-api payment-api >/dev/null
    wait_healthy "$variant" "order-api" || { run_compose "$variant" logs order-api | tail -50; exit 1; }
    wait_healthy "$variant" "payment-api" || { run_compose "$variant" logs payment-api | tail -50; exit 1; }
    run_compose "$variant" up -d api-gateway >/dev/null
    wait_healthy "$variant" "api-gateway" || { run_compose "$variant" logs api-gateway | tail -50; exit 1; }
    sleep "$SETTLE"
    measure_gateway "$variant" | while IFS=$'\t' read v url idx t; do
      printf '%s\t%s\t%s\t%s\t%s\n' "$v" "$run" "$url" "$idx" "$t" >> "$RESULTS"
      printf '  %s req=%s time=%ss\n' "$url" "$idx" "$t"
    done
  done
  run_compose "$variant" down -v --remove-orphans >/dev/null 2>&1 || true
done

echo ""
echo "== Summary (first request avg ms across runs) =="
echo "variant	path	first_avg_ms	first_min_ms	first_max_ms	n"
awk -F'\t' 'NR>1 && $4==1 {
  k=$1"|"$3
  sum[k]+=$5*1000; count[k]++
  if (!(k in min) || $5*1000 < min[k]) min[k]=$5*1000
  if (!(k in max) || $5*1000 > max[k]) max[k]=$5*1000
} END {
  for (k in sum) {
    split(k, p, "|")
    printf "%s\t%s\t%.1f\t%.1f\t%.1f\t%d\n", p[1], p[2], sum[k]/count[k], min[k], max[k], count[k]
  }
}' "$RESULTS" | sort

echo ""
echo "raw: $RESULTS"
