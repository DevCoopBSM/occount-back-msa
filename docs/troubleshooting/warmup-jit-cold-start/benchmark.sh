#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../.." && pwd)
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.benchmark.yml"
WORKTREE_ROOT="${WORKTREE_ROOT:-${TMPDIR:-/tmp}/warmup-jit-cold-start}"
RUNS="${RUNS:-3}"
SETTLE_SECONDS="${SETTLE_SECONDS:-5}"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-180}"
TARGET_SERVICES="${TARGET_SERVICES:-item-api member-api}"
BENCHMARK_API_PORT="${BENCHMARK_API_PORT:-18084}"
ITEM_API_PORT="${ITEM_API_PORT:-18084}"
MEMBER_API_PORT="${MEMBER_API_PORT:-18083}"
RESULTS_FILE="${RESULTS_FILE:-$WORKTREE_ROOT/results.tsv}"
CREATED_WORKTREES_FILE="$WORKTREE_ROOT/created-worktrees.txt"
CASES_FILE="$WORKTREE_ROOT/cases.tsv"
VARIANTS_FILE="$WORKTREE_ROOT/variants.tsv"
JAVA_OPTS_VARIANTS="${JAVA_OPTS_VARIANTS:-default||기본}"

CASES='
before|97057405fee9886240b8f388f19c4e70e670f70b|warmbench-before|warmup 이전
current_warmup|7299aa6a592885d353c5dc1fd6f5f4817eec1965|warmbench-current|현재 warmup
'

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "missing command: $1" >&2
        exit 1
    fi
}

compose() {
    checkout_dir=$1
    project_name=$2
    java_opts=$3
    shift 2
    shift 1

    CHECKOUT_DIR="$checkout_dir" \
    BENCHMARK_API_PORT="$BENCHMARK_API_PORT" \
    ITEM_API_PORT="$ITEM_API_PORT" \
    MEMBER_API_PORT="$MEMBER_API_PORT" \
    JAVA_OPTS="$java_opts" \
        docker compose -p "$project_name" -f "$COMPOSE_FILE" "$@"
}

ensure_worktree() {
    case_name=$1
    commit=$2
    worktree_dir="$WORKTREE_ROOT/$case_name"

    if [ ! -e "$worktree_dir" ]; then
        git -C "$REPO_ROOT" worktree add --detach "$worktree_dir" "$commit" >/dev/null
        printf '%s\n' "$worktree_dir" >> "$CREATED_WORKTREES_FILE"
    fi

    printf '%s\n' "$worktree_dir"
}

wait_for_startup_line() {
    checkout_dir=$1
    project_name=$2
    java_opts=$3
    service_name=$4
    start_epoch=$(date +%s)

    while :; do
        line=$(compose "$checkout_dir" "$project_name" "$java_opts" logs "$service_name" 2>/dev/null | grep "Started .* in .* seconds" | tail -n 1 || true)
        if [ -n "$line" ]; then
            printf '%s\n' "$line"
            return 0
        fi

        now_epoch=$(date +%s)
        if [ $((now_epoch - start_epoch)) -ge "$STARTUP_TIMEOUT_SECONDS" ]; then
            echo "startup log timeout: $project_name" >&2
            return 1
        fi

        sleep 1
    done
}

extract_value() {
    line=$1
    pattern=$2
    printf '%s\n' "$line" | sed -n "s/$pattern/\\1/p" | tail -n 1
}

service_request_times() {
    checkout_dir=$1
    project_name=$2
    java_opts=$3
    service_name=$4

    case "$service_name" in
        item-api)
            compose "$checkout_dir" "$project_name" "$java_opts" exec -T \
                -e REQUEST_URL="http://localhost:8080/api/v3/items" \
                item-api \
                sh -lc 'for i in 1 2 3; do curl -s -o /dev/null -w "%{time_total}\n" "$REQUEST_URL"; done'
            ;;
        member-api)
            compose "$checkout_dir" "$project_name" "$java_opts" exec -T \
                -e REQUEST_METHOD="POST" \
                -e REQUEST_URL="http://localhost:8080/api/v3/auth/login" \
                -e REQUEST_CONTENT_TYPE="application/json" \
                -e REQUEST_BODY='{"email":"warmup@warmup.internal","password":"password1234"}' \
                member-api \
                sh -lc 'for i in 1 2 3; do curl -s -o /dev/null -w "%{time_total}\n" -X "$REQUEST_METHOD" -H "Content-Type: $REQUEST_CONTENT_TYPE" --data "$REQUEST_BODY" "$REQUEST_URL"; done'
            ;;
        *)
            echo "unsupported service: $service_name" >&2
            return 1
            ;;
    esac
}

print_case_summary() {
    case_name=$1
    variant_name=$2
    service_name=$3
    awk -F '\t' -v case_name="$case_name" -v variant_name="$variant_name" -v service_name="$service_name" '
        $1 == case_name && $2 == variant_name && $3 == service_name {
            startup += $7
            first += $8
            second += $9
            third += $10
            count += 1
        }
        END {
            if (count == 0) {
                exit 0
            }
            printf "%s\t%s\t%s\t%.3f\t%.1f\t%.2f\t%.2f\n",
                case_name,
                variant_name,
                service_name,
                startup / count,
                (first / count) * 1000,
                (second / count) * 1000,
                (third / count) * 1000
        }
    ' "$RESULTS_FILE"
}

write_case_and_variant_files() {
    printf '%s\n' "$CASES" | sed '/^$/d' > "$CASES_FILE"
    printf '%s\n' "$JAVA_OPTS_VARIANTS" | sed '/^$/d' > "$VARIANTS_FILE"
}

cleanup() {
    while IFS='|' read -r case_name _ project_name _; do
        [ -n "$case_name" ] || continue
        worktree_dir="$WORKTREE_ROOT/$case_name"
        while IFS='|' read -r variant_name _ _; do
            [ -n "$variant_name" ] || continue
            compose "$worktree_dir" "${project_name}-${variant_name}" "" down -v --remove-orphans >/dev/null 2>&1 || true
        done < "$VARIANTS_FILE"
    done < "$CASES_FILE"

    if [ "${KEEP_WORKTREES:-0}" != "1" ] && [ -f "$CREATED_WORKTREES_FILE" ]; then
        while IFS= read -r worktree_dir; do
            [ -n "$worktree_dir" ] || continue
            git -C "$REPO_ROOT" worktree remove --force "$worktree_dir" >/dev/null 2>&1 || true
        done < "$CREATED_WORKTREES_FILE"
    fi
}

require_command git
require_command docker
require_command awk
require_command sed
require_command grep
require_command curl

mkdir -p "$WORKTREE_ROOT"
: > "$CREATED_WORKTREES_FILE"
write_case_and_variant_files

trap cleanup EXIT INT TERM HUP

printf 'case\tvariant\tservice\trun\tlabel\tcommit\tstartup_seconds\tfirst_request_seconds\tsecond_request_seconds\tthird_request_seconds\tbusiness_warmup_ms\tservlet_warmup_ms\tjpa_warmup_ms\tjava_opts\n' > "$RESULTS_FILE"

while IFS='|' read -r case_name commit project_name label <&4; do
    [ -n "$case_name" ] || continue

    checkout_dir=$(ensure_worktree "$case_name" "$commit")

    while IFS='|' read -r variant_name variant_java_opts variant_label <&3; do
        [ -n "$variant_name" ] || continue

        variant_project_name="${project_name}-${variant_name}"
        full_label="$label"
        if [ -n "${variant_label:-}" ]; then
            full_label="$label / $variant_label"
        fi

        echo "== $full_label ($commit) =="
        compose "$checkout_dir" "$variant_project_name" "$variant_java_opts" down -v --remove-orphans >/dev/null 2>&1 </dev/null || true
        compose "$checkout_dir" "$variant_project_name" "$variant_java_opts" build $TARGET_SERVICES >/dev/null </dev/null

        run_index=1
        while [ "$run_index" -le "$RUNS" ]; do
            compose "$checkout_dir" "$variant_project_name" "$variant_java_opts" down -v --remove-orphans >/dev/null 2>&1 </dev/null || true
            compose "$checkout_dir" "$variant_project_name" "$variant_java_opts" up -d mysql kafka >/dev/null </dev/null
            compose "$checkout_dir" "$variant_project_name" "$variant_java_opts" up -d $TARGET_SERVICES >/dev/null </dev/null

            for service_name in $TARGET_SERVICES; do
                startup_line=$(wait_for_startup_line "$checkout_dir" "$variant_project_name" "$variant_java_opts" "$service_name")
                startup_seconds=$(extract_value "$startup_line" '.*Started .* in \([0-9.]*\) seconds.*')

                sleep "$SETTLE_SECONDS"

                logs=$(compose "$checkout_dir" "$variant_project_name" "$variant_java_opts" logs "$service_name" 2>/dev/null </dev/null || true)
                business_warmup_ms=$(extract_value "$logs" '.*business warmup completed .* in \([0-9][0-9]*\) ms.*')
                servlet_warmup_ms=$(extract_value "$logs" '.*Servlet startup warmup completed .* in \([0-9][0-9]*\) ms.*')
                jpa_warmup_ms=$(extract_value "$logs" '.*JPA startup warmup completed .* in \([0-9][0-9]*\) ms.*')

                request_times=$(service_request_times "$checkout_dir" "$variant_project_name" "$variant_java_opts" "$service_name")
                first_request=$(printf '%s\n' "$request_times" | sed -n '1p')
                second_request=$(printf '%s\n' "$request_times" | sed -n '2p')
                third_request=$(printf '%s\n' "$request_times" | sed -n '3p')

                printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
                    "$case_name" \
                    "$variant_name" \
                    "$service_name" \
                    "$run_index" \
                    "$full_label" \
                    "$commit" \
                    "${startup_seconds:-}" \
                    "${first_request:-}" \
                    "${second_request:-}" \
                    "${third_request:-}" \
                    "${business_warmup_ms:-}" \
                    "${servlet_warmup_ms:-}" \
                    "${jpa_warmup_ms:-}" \
                    "${variant_java_opts:-}" \
                    >> "$RESULTS_FILE"

                printf 'service=%s variant=%s run=%s startup=%ss first=%sms second=%sms third=%sms\n' \
                    "$service_name" \
                    "$variant_name" \
                    "$run_index" \
                    "${startup_seconds:-n/a}" \
                    "$(awk "BEGIN { printf \"%.1f\", ${first_request:-0} * 1000 }")" \
                    "$(awk "BEGIN { printf \"%.2f\", ${second_request:-0} * 1000 }")" \
                    "$(awk "BEGIN { printf \"%.2f\", ${third_request:-0} * 1000 }")"
            done

            echo
            run_index=$((run_index + 1))
        done

        compose "$checkout_dir" "$variant_project_name" "$variant_java_opts" down -v --remove-orphans >/dev/null </dev/null
        echo
    done 3< "$VARIANTS_FILE"
done 4< "$CASES_FILE"

echo "== Summary (services: $TARGET_SERVICES) =="
echo "case	variant	service	startup_avg_s	first_avg_ms	second_avg_ms	third_avg_ms"
while IFS='|' read -r case_name _ _ _ <&4; do
    [ -n "$case_name" ] || continue
    while IFS='|' read -r variant_name _ _ <&3; do
        [ -n "$variant_name" ] || continue
        for service_name in $TARGET_SERVICES; do
            print_case_summary "$case_name" "$variant_name" "$service_name"
        done
    done 3< "$VARIANTS_FILE"
done 4< "$CASES_FILE"

echo
echo "raw results: $RESULTS_FILE"
