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

- `item-api`: `GET /api/v3/items` — `item-api` + `member-api` 동시 기동 환경에서 3회 측정
- `member-api`: `POST /api/v3/auth/login` — 동시 기동 시 이상치가 많아 단독 환경에서 5회 재측정

측정 절차:

1. 각 커밋을 `git worktree`로 분리한다.
2. 각 run 전 `compose down -v --remove-orphans`로 전체 teardown 후 `mysql`, `kafka`, API 서비스를 모두 새로 기동한다.
3. Spring Boot 로그의 `Started ... in ... seconds`를 startup 시간으로 기록한다.
4. startup 완료 후 8초 대기한 뒤 각 API 컨테이너 내부에서 요청을 3회 호출한다.
5. 첫 번째 요청을 cold start 첫 실요청으로 기록하고, 두 번째와 세 번째는 steady state 참고값으로 사용한다.

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

### member-api (단독 측정, 5회)

item-api와 동시 기동 시 CPU 경쟁으로 이상치가 많아 단독 환경에서 재측정했다.

| 버전 | JVM 옵션 | startup 평균 | 첫 `/auth/login` 평균 | 비고 |
|---|---|---:|---:|---|
| warmup 이전 | 기본 | 6.0s | 185ms | 기준값 |
| warmup 이전 | JIT 임계치 하향 | 7.7s | 191ms | startup +1.7s, 첫 요청 거의 동일 |
| 현재 warmup | 기본 | 7.4s | 155ms¹ | 첫 요청 16% 개선, startup +1.4s |
| 현재 warmup | JIT 임계치 하향 | 7.6s | 159ms | 첫 요청 14% 개선, startup +1.6s |

> ¹ run 3에서 business warmup 1589ms 이상치 발생 (startup 14.3s). 제외 시 평균 133ms (28% 개선).

steady state(2nd, 3rd 요청)는 모든 버전에서 한 자리 ~ 수십 ms대로 차이가 작았다. 개선 또는 악화는 거의 전부 첫 요청에서 나타났다.

## 해석

### 1. item-api는 현재 warmup이 실제로 효과가 있었다

`item-api / 기본` 기준 결과는 명확하다.

- 첫 요청: `235ms → 183ms` (22.1% 개선)
- startup 비용: `10.4s → 14.3s` (+3.9s)
- business warmup 평균: 약 1.2s, servlet warmup 평균: 약 1.4s (run2 이상치 포함)

"기동 시 더 오래 준비하고 첫 요청을 더 빠르게 받는" 구조가 item-api에서는 재현됐다.

### 2. member-api는 단독 측정으로 warmup 효과 확인

item-api와 동시 기동 시 두 서비스가 JIT 컴파일 CPU를 경쟁해 startup과 첫 요청 모두 수치가 크게 왜곡됐다. 단독 측정 결과는 명확하다.

- 첫 요청: `185ms → 155ms` (16.3% 개선)
- startup 비용: `6.0s → 7.4s` (+1.4s)
- run 3에서 business warmup 1589ms 이상치 발생. 제외 시 첫 요청 평균 133ms (28.3% 개선).

이상치 원인: `MemberBusinessWarmup`이 `LoginUserUseCase.login()`을 반복 호출하는데, 해당 run에서 MySQL 초기화와 겹쳐 인증 쿼리가 지연된 것으로 추정된다. warmup 자체가 느려진 만큼 JIT 준비가 덜 됐고, 첫 실 요청도 늦어졌다.

### 3. JIT 임계치 하향 플래그는 이번 실측에서 순효과가 없었다

item-api(동시 기동)에서 threshold 플래그는 startup을 크게 늘렸다.

- `warmup 이전`: `10.4s → 22.3s` (+11.9s)
- `현재 warmup`: `14.3s → 17.5s` (+3.2s)

두 서비스가 동시에 JIT 컴파일하면 CPU 경쟁이 심해져 startup이 폭증하는 구조다. member-api 단독 기동에서는 `+1.7s` 수준으로 훨씬 낮았다.

첫 요청 개선도 확인되지 않았다.

- `item-api / 기본`: `235ms → 344ms` (threshold만 적용, warmup 없음)
- `item-api / 현재 warmup`: `183ms → 209ms`
- `member-api / 기본`: `185ms → 191ms` (단독 측정)
- `member-api / 현재 warmup`: `155ms → 159ms` (단독 측정, 차이 미미)

이번 로컬 Docker 재현 조건에서는 threshold 플래그가 startup 비용만 늘리고 cold request 개선으로 회수되지 않았다.

## 결론

1. **item-api** warmup은 유효하다. 첫 요청 22% 개선 (`235ms → 183ms`), 비용은 startup +3.9s.
2. **member-api** warmup도 유효하다. 단독 측정 기준 첫 요청 16% 개선 (`185ms → 155ms`), 비용은 startup +1.4s. 이상치 제외 시 28% 개선.
3. **JIT 임계치 하향 플래그**는 이번 로컬 Docker 조건에서 비권장. 다중 서비스 동시 기동 시 startup을 최대 +12s 늘리고 첫 요청 개선은 확인되지 않았다.

## 재현 방법

재현용 파일:

- [docker-compose.benchmark.yml](docker-compose.benchmark.yml)
- [benchmark.sh](benchmark.sh)
- [results-2026-04-26.tsv](results-2026-04-26.tsv) — item-api + member-api 동시 기동, 3회
- [results-member-2026-04-26.tsv](results-member-2026-04-26.tsv) — member-api 단독, 5회

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

---

## 후속 (2026-04-27): 워밍업 모듈 리팩토링 + service-layer 워밍업 + AppCDS 시도

### 1. 리팩토링 (코드 품질, 성능 변화 없음)

`modules/warmup` 인프라와 4개 도메인 `*BusinessWarmup`을 정리했다. 알고리즘 동등.

- `StartupWarmupProperties`에 매직 상수 승격 (`jpaRepeat`, `servletRepeat`, `businessRepeat`)
- `ServletStartupWarmup` 두 갈래(`warmupEndpoints` / `warmupLegacy`) → 단일 경로
- 도메인 레이어 보일러플레이트(`measureTimeMillis { repeat(N) {...} }` + 로깅) → `BusinessWarmupRunner`로 흡수
- 4개 도메인은 `BusinessWarmup` 구현체로 단순화, 매직 상수는 `WarmupProbe`로 일원화
- 깨진 `JpaStartupWarmupTest` 단언 정정
- `member-api/application.yaml`의 평문 자격증명 + `/auth/register` 워밍업 → invalid login(`warmup@warmup.invalid`/`x`) 1건으로 안전화

이 변경은 운영 안전성 향상이며 cold-start 알고리즘은 변경 없음. **첫 요청 시간 변화는 워밍업이 자극하는 코드 경로 자체가 valid → invalid login으로 바뀐 부수 효과**(BCrypt match + JWT signing 경로가 빠짐 = ~50–100ms 단축)이지 본 리팩토링의 직접 효과가 아니다.

### 2. service-layer 워밍업 추가 (`MemberBusinessWarmup` ← `LoginUserUseCase`)

invalid login 기반 HTTP 워밍업이 BCrypt/JWT 경로를 자극하지 못하는 부분을 보완하기 위해 `MemberBusinessWarmup`에 `LoginUserUseCase.login(...)` 직접 호출을 추가했다. invalid 자격증명이라 `UserNotFoundException`을 throw하지만 use case 메서드 바이트코드 + Spring service-layer 호출 체인이 자극된다.

| case | startup avg | **first req avg** | 2nd avg | 3rd avg | business warmup |
|---|---:|---:|---:|---:|---:|
| service-layer 워밍업 미적용 | 9.15s | **42.0ms** | 22.4ms | 10.2ms | 2811ms |
| service-layer 워밍업 적용 | 8.97s | **25.9ms** | 7.5ms | 5.7ms | 2198ms |

(member-api 단독, RUNS=3, invalid login 측정)

해석:
- 첫 요청 평균 ~38% 단축 (42 → 26ms). 단 RUNS=3이라 통계적 신뢰도는 약함.
- 분산 감소가 더 의미 있음: 미적용은 19–69ms (3.6×), 적용은 16–32ms (2×) — tail latency 안정화 신호.
- startup / business warmup 시간은 사실상 동일 (LoginUseCase 2회 추가의 비용 무시 가능).

### 3. AppCDS 시도와 실패

Spring Boot 4.0.3 + Kotlin 2.3.10 + Spring Data JPA 환경에서 AppCDS archive 사용 시 부팅 실패가 재현된다.

```
QueryCreationException: Cannot create query for method
  [UserJpaRepository.findByEmail(java.lang.String)];
  Cannot invoke "java.lang.Class.getSimpleName()" because "it" is null
  at KotlinReflectionUtils.findKotlinFunction
  at AbstractRepositoryMetadata.getReturnType
```

원인: Spring Data가 메서드 파생 쿼리(`findByEmail`)를 분석할 때 `KotlinReflectionUtils.isSuspend`로 Kotlin suspend 함수 여부를 체크. 그 과정에서 `KClasses.getFunctions` → `KClassImpl.getMembers`가 walk 중 inherited Java parameterized interface(`JpaRepository<User, Long>`) 메서드의 declaring `Class<?>`를 archive에서 못 찾고 null 반환 → `getSimpleName()` NPE.

시도한 조합 모두 동일 재현:
- fat jar + `-XX:SharedArchiveFile`
- extracted layered jar + archive (warmup OFF로 학습)
- extracted layered jar + archive (warmup ON으로 학습)

archive에는 "실행된 클래스 비트맵"이 들어가지 archive 시점의 generic resolution 결과는 들어가지 않으므로, 학습 시 reflection 경로를 다 자극해도 해결 안 됨. 단순 옵션 추가로는 본 스택에서 도입 불가.

대안 카드 (별도 검토):
1. **Spring Boot AOT (`processAot`)** — repository proxy를 빌드 타임에 생성해 reflection lookup 자체를 줄임. 정공법.
2. **CRaC** — 워밍업 이후 JVM 스냅샷. cold-start ms 단위. 인프라 부담 큼.
3. **GraalVM native-image** — 부팅 100ms대. Kotlin reflection 메타데이터 직접 작성 부담.

### 추가 자료

- [results-member-service-warmup-2026-04-27.tsv](results-member-service-warmup-2026-04-27.tsv) — service-layer 워밍업 미적용 vs 적용, member-api 단독, 3회
