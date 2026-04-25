# Warmup/JIT Cold Start 비교

## 배경

프로젝트에는 cold start 직후 첫 요청 지연을 줄이기 위해 두 종류의 최적화가 들어가 있었다.

- 공통 startup warmup (JPA 쿼리 사전 실행)
- 서비스별 business warmup + servlet warmup

추가로 GitOps `prod` 값에는 아래 JVM 플래그가 들어가 있다.

```text
-XX:Tier3InvocationThreshold=10 -XX:Tier4InvocationThreshold=150
```

이번 문서는 아래 3가지 버전을 실측 비교한 결과를 정리한다.

1. warmup 이전 기준값
2. 현재 warmup 적용
3. 현재 warmup + JIT 임계치 하향

측정일: `2026-04-26`

## 비교 대상

| 버전 | 커밋 | 설명 |
|---|---|---|
| warmup 이전 | `97057405fee9886240b8f388f19c4e70e670f70b` | warmup 도입 이전 기준점 |
| 현재 warmup | `7299aa6a592885d353c5dc1fd6f5f4817eec1965` | business warmup + servlet warmup 적용 |

비교 JVM 옵션:

- `기본`: 추가 JIT threshold 플래그 없음
- `JIT 임계치 하향`: `-XX:Tier3InvocationThreshold=10 -XX:Tier4InvocationThreshold=150`

## 측정 대상과 방법

같은 compose 스택에서 아래 서비스를 함께 띄웠다.

- `item-api`: `GET /api/v3/items`
- `member-api`: `POST /api/v3/auth/login`

측정 절차:

1. 각 커밋을 `git worktree`로 분리한다.
2. 각 run 전 `compose down -v --remove-orphans`로 전체 teardown 후 `mysql`, `kafka`, API 서비스를 모두 새로 기동한다.
3. Spring Boot 로그의 `Started ... in ... seconds`를 startup 시간으로 기록한다.
4. startup 완료 후 8초 대기한 뒤 각 API 컨테이너 내부에서 요청을 3회 호출한다.
5. 첫 번째 요청을 cold start 첫 실요청으로 기록하고, 두 번째와 세 번째는 steady state 참고값으로 사용한다.
6. 각 조합마다 3회 반복 측정 후 평균을 낸다.

환경 메모:

- 호스트 loopback 대신 컨테이너 내부 loopback으로 측정했다.
- 모든 비교군이 같은 방식이라 상대 비교에는 문제 없다.
- `OTEL_SDK_DISABLED=true`로 observability 노이즈를 줄였다.
- 각 run 전 `compose down -v`로 MySQL 볼륨까지 완전 초기화해 DB 캐시 영향을 제거했다.

## 결과 요약

### item-api

| 버전 | JVM 옵션 | startup 평균 | 첫 `/items` 평균 | 비고 |
|---|---|---:|---:|---|
| warmup 이전 | 기본 | 10.4s | 235ms | 기준값 |
| warmup 이전 | JIT 임계치 하향 | 22.3s | 344ms | startup +11.9s, 첫 요청 악화 |
| 현재 warmup | 기본 | 14.3s | 183ms | 첫 요청 22% 개선, startup +3.9s |
| 현재 warmup | JIT 임계치 하향 | 17.5s | 209ms | startup +7.1s, 첫 요청 개선 없음 |

### member-api

| 버전 | JVM 옵션 | startup 평균 | 첫 `/auth/login` 평균 | 비고 |
|---|---|---:|---:|---|
| warmup 이전 | 기본 | 10.5s | 355ms | 기준값 |
| warmup 이전 | JIT 임계치 하향 | 22.5s | 444ms | startup +12.0s, 첫 요청 악화 |
| 현재 warmup | 기본 | 14.5s | 786ms¹ | 이상치 포함 — 해석 참고 |
| 현재 warmup | JIT 임계치 하향 | 17.8s | 437ms¹ | 이상치 포함 — 해석 참고 |

> ¹ 3회 측정 중 run 2에서 이상치 발생. `현재 warmup / 기본` run 2: 1434ms (servlet warmup 2406ms), `현재 warmup / JIT 임계치 하향` run 2: 783ms. 이상치 제외 시 각각 461ms, 264ms.

steady state(2nd, 3rd 요청)는 모든 버전에서 한 자리 ~ 수십 ms대로 차이가 작았다. 개선 또는 악화는 거의 전부 첫 요청에서 나타났다.

## 해석

### 1. item-api는 현재 warmup이 실제로 효과가 있었다

`item-api / 기본` 기준 결과는 명확하다.

- 첫 요청: `235ms → 183ms` (22.1% 개선)
- startup 비용: `10.4s → 14.3s` (+3.9s)
- business warmup 평균: 약 1.2s, servlet warmup 평균: 약 1.4s (run2 이상치 포함)

"기동 시 더 오래 준비하고 첫 요청을 더 빠르게 받는" 구조가 item-api에서는 재현됐다.

### 2. member-api는 측정 결과 신뢰도가 낮다

`현재 warmup / 기본` run 2에서 첫 요청 1434ms, servlet warmup 2406ms가 기록됐다. 같은 조건의 run 1(693ms)·run 3(229ms)과 비교하면 명확한 이상치다.

이상치를 제외한 2회 평균:

- `현재 warmup / 기본`: 461ms (기준 355ms 대비 29.9% 악화)
- `현재 warmup / JIT 임계치 하향`: 264ms (기준 355ms 대비 25.6% 개선)

그러나 run 수가 적어 신뢰 구간이 넓다. member-api에 대해서는 추가 측정이 필요하다.

추정 원인: `MemberBusinessWarmup`이 `LoginUserUseCase.login()`을 반복 호출하는데, 이 경로가 DB 인증·토큰 발급까지 포함한다. 일부 run에서 MySQL 초기화나 커넥션 풀 경쟁이 겹치면 warmup 자체가 느려지고 첫 실 요청도 늦어지는 것으로 추정된다.

### 3. JIT 임계치 하향 플래그는 이번 실측에서 순효과가 없었다

`threshold on`은 두 서비스 모두 startup을 크게 늘렸다.

| 서비스 | warmup 이전 startup | warmup 이전 + threshold startup |
|---|---:|---:|
| item-api | 10.4s | 22.3s (+11.9s) |
| member-api | 10.5s | 22.5s (+12.0s) |

첫 요청도 두 서비스 모두 `threshold off` 대비 개선되지 않았다.

- `item-api`: `235ms → 344ms` (warmup 이전 기준)
- `item-api / 현재 warmup`: `183ms → 209ms`
- `member-api`: `355ms → 444ms` (warmup 이전 기준)

이번 로컬 Docker 재현 조건에서는 threshold 플래그가 startup 비용만 늘리고 cold request 개선으로 회수되지 않았다.

## 결론

1. **item-api** business warmup은 유효하다. 첫 요청 22% 개선, 비용은 startup +3.9s.
2. **member-api**는 측정 이상치로 인해 결론을 내리기 어렵다. 추가 측정 필요.
3. **JIT 임계치 하향 플래그**는 이번 로컬 Docker 조건에서 비권장. startup을 ~12s 늘리고 첫 요청 개선은 확인되지 않았다.

## 재현 방법

재현용 파일:

- [docker-compose.benchmark.yml](docker-compose.benchmark.yml)
- [benchmark.sh](benchmark.sh)
- [results-2026-04-26.tsv](results-2026-04-26.tsv)

기본 실행 (warmup 이전 vs 현재 warmup, 기본 JVM):

```sh
./docs/troubleshooting/warmup-jit-cold-start/benchmark.sh
```

JIT 임계치 비교 포함:

```sh
JAVA_OPTS_VARIANTS=$'default||기본\nthreshold_on|-XX:Tier3InvocationThreshold=10 -XX:Tier4InvocationThreshold=150|JIT 임계치 하향' \
./docs/troubleshooting/warmup-jit-cold-start/benchmark.sh
```

주요 옵션:

- `RUNS=5`: 반복 횟수 변경
- `SETTLE_SECONDS=8`: startup 로그 이후 추가 대기 시간 변경
- `WORKTREE_ROOT=/tmp/custom-bench`: worktree 경로 변경
- `RESULTS_FILE=/tmp/warmup-results.tsv`: 결과 파일 경로 변경
- `KEEP_WORKTREES=1`: 종료 후 worktree 유지
- `ITEM_API_PORT=18084`: item-api 외부 포트 변경
- `MEMBER_API_PORT=18083`: member-api 외부 포트 변경
- `TARGET_SERVICES='item-api member-api'`: 함께 측정할 서비스 지정

주의:

- 스크립트는 `docker`, `git worktree`, `curl`이 설치된 환경을 전제로 한다.
- 각 run 전 `compose down -v`로 MySQL 볼륨까지 완전 초기화하므로 run당 MySQL 기동 시간이 포함된다.
- 측정값은 Docker 캐시, 로컬 CPU 상태, 백그라운드 프로세스 영향으로 일부 달라질 수 있다.
- `threshold on` 비교는 startup 비용이 커서 측정 시간이 꽤 길다.
