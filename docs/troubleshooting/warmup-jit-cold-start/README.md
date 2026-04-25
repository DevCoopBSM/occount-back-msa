# Warmup/JIT Cold Start 비교

## 배경

프로젝트에는 cold start 직후 첫 요청 지연을 줄이기 위해 두 종류의 최적화가 들어가 있었다.

- 공통 startup warmup
- 서비스별 business warmup + JIT trigger

추가로 GitOps `prod` 값에는 아래 JVM 플래그가 들어가 있다.

```text
-XX:Tier3InvocationThreshold=10 -XX:Tier4InvocationThreshold=150
```

이번 문서는 이 변경이 실제로 얼마나 효과가 있는지 `docker compose`로 `item-api`와 `member-api`를 같이 띄운 뒤 실측한 결과를 정리한다.

측정일:
- `2026-04-26`

## 비교 대상

비교 커밋:

1. `97057405fee9886240b8f388f19c4e70e670f70b`
   warmup 도입 이전 기준점
2. `53d56051ad1dc9bb3c7ff79940437f1f1ba7497b`
   공통 startup warmup 도입
3. `0d215b16515e190450ae27b5943a601183a5270b`
   business warmup + JIT trigger가 실제로 빌드 가능한 첫 상태

비교 JVM 옵션:

1. `threshold off`
   추가 JIT threshold 플래그 없음
2. `threshold on`
   `-XX:Tier3InvocationThreshold=10 -XX:Tier4InvocationThreshold=150`

주의:
- `0bb50bbc7d18c5146be32cf68511a3f680446263`는 의도상 JIT trigger 변경점이 맞지만 단독 상태에서는 `item-bootstrap`이 컴파일되지 않아 실측 비교에서 제외했다.
- 따라서 business warmup 비교는 바로 다음 빌드 가능 커밋인 `0d215b1`을 사용했다.

## 측정 대상과 방법

같은 compose 스택에서 아래 서비스를 함께 띄웠다.

- `item-api`: `GET /api/v3/items`
- `member-api`: `POST /api/v3/auth/login`

측정 절차:

1. 각 커밋을 `git worktree`로 분리한다.
2. 전용 compose로 `mysql`, `kafka`, `item-api`, `member-api`를 함께 띄운다.
3. Spring Boot 로그의 `Started ... in ... seconds`를 startup 시간으로 기록한다.
4. 각 API 컨테이너 내부에서 요청을 3회 호출한다.
5. 첫 번째 요청을 cold start 첫 실요청으로 기록하고, 두 번째와 세 번째는 steady state 참고값으로 사용한다.
6. 각 조합마다 3회 반복 측정한다.

환경 메모:

- 호스트 loopback 대신 컨테이너 내부 loopback으로 측정했다.
- 모든 비교군이 같은 방식이라 상대 비교에는 문제 없다.
- `OTEL_SDK_DISABLED=true`로 observability 노이즈를 줄였다.

## 결과 요약

### item-api

| 커밋 | JVM 옵션 | startup 평균 | 첫 `/items` 평균 | 해석 |
|---|---|---:|---:|---|
| `9705740` | threshold off | 10.443s | 191.8ms | 기준값 |
| `53d5605` | threshold off | 10.778s | 287.6ms | 오히려 49.9% 악화 |
| `0d215b1` | threshold off | 12.078s | 146.9ms | 기준 대비 23.4% 개선 |
| `9705740` | threshold on | 17.265s | 283.4ms | 기준 대비 startup, 첫 요청 모두 악화 |
| `53d5605` | threshold on | 18.262s | 350.4ms | 가장 나쁨 |
| `0d215b1` | threshold on | 17.382s | 290.2ms | `threshold off`보다 확실히 나쁨 |

### member-api

`member-api`는 사용자 요청대로 "직전 커밋(HEAD) vs 현재 작업 트리"를 같은 조건에서 다시 분리 측정했다.

| 기준 | startup 평균 | 첫 `/auth/login` 평균 | 해석 |
|---|---|---:|---:|---|
| 직전 커밋 `311e89a` | 7.916s | 184.6ms | 기준값 |
| 현재 작업 트리 | 7.093s | 246.9ms | startup은 10.4% 단축됐지만 첫 로그인은 33.8% 악화 |

참고용으로 앞서 측정했던 과거 커밋 비교는 아래와 같았다.

| 과거 커밋 | JVM 옵션 | startup 평균 | 첫 `/auth/login` 평균 |
|---|---|---:|---:|
| `9705740` | threshold off | 10.277s | 217.4ms |
| `53d5605` | threshold off | 10.465s | 343.8ms |
| `0d215b1` | threshold off | 11.366s | 337.1ms |
| `9705740` | threshold on | 17.346s | 348.8ms |
| `53d5605` | threshold on | 18.024s | 258.6ms |
| `0d215b1` | threshold on | 17.650s | 284.8ms |

steady state는 대부분 한 자리 ms대라 차이가 작았다. 개선 또는 악화는 거의 전부 첫 요청에서 나타났다.

## 해석

### 1. item-api는 business warmup이 실제로 효과가 있었다

`item-api`의 `threshold off` 기준 결과는 명확하다.

- 공통 warmup만 넣은 `53d5605`는 첫 요청이 `191.8ms -> 287.6ms`로 악화됐다.
- business warmup + JIT trigger가 들어간 `0d215b1`은 `191.8ms -> 146.9ms`로 23.4% 개선됐다.

이 결과는 `ItemBusinessWarmup`이 실제 측정 경로와 가깝기 때문이다. warmup 코드가 `getAllItems()`, `getItemsWithoutBarcode()`, `findByBarcode()`, `findById()`를 직접 호출한다. 근거: [ItemBusinessWarmup.kt](/Users/hyunwoo/IdeaProjects/occount-back-msa/domains/item/item-bootstrap/src/main/kotlin/devcoop/occount/item/bootstrap/warmup/ItemBusinessWarmup.kt:11)

대신 비용은 startup으로 이동했다.

- `item-api threshold off`
  - startup: `10.443s -> 12.078s`
  - first request: `191.8ms -> 146.9ms`
- business warmup 로그 평균: 약 `1.93s`
- servlet warmup 로그 평균: 약 `178ms`

즉 `item-api`에서는 "기동 시 더 오래 준비하고 첫 요청을 더 빠르게 받는" 구조가 실제로 재현됐다.

### 2. member-api는 직전 커밋과 현재 작업 트리를 직접 분리 측정해야 했다

이전에는 과거 커밋들과 현재 작업 트리를 섞어서 해석했는데, `member-api`는 그 방식보다 `HEAD`와 현재 작업 트리를 바로 붙여 재는 편이 훨씬 직관적이다.

같은 조건에서 다시 분리 측정한 결과는 다음과 같다.

- 직전 커밋 `311e89a`: `184.6ms`
- 현재 작업 트리: `246.9ms`

즉 이번 직접 비교 기준으로는 현재 수정본이 `member-api` 첫 로그인 latency를 개선하지 못했다.

코드상으로는 현재 `MemberBusinessWarmup`가 `LoginUserUseCase.login(MemberLoginRequest(...))`를 직접 반복 호출하도록 바뀌어 있다. 근거: [MemberBusinessWarmup.kt](/Users/hyunwoo/IdeaProjects/occount-back-msa/domains/member/member-bootstrap/src/main/kotlin/devcoop/occount/member/bootstrap/warmup/MemberBusinessWarmup.kt:1)

하지만 같은 조건에서 다시 붙여 측정한 결과를 우선하면, 이번 변경은 적어도 현재 로컬 Docker 재현에선 startup은 조금 줄였지만 첫 `/auth/login`은 오히려 더 느렸다.

### 3. JIT threshold 하향 플래그는 이번 로컬 재현 조건에서 순효과가 없었다

`threshold on`은 두 서비스 모두 startup을 크게 늘렸다.

- `item-api`
  - before: `10.443s -> 17.265s`
  - jit warmup: `12.078s -> 17.382s`
- `member-api`
  - before: `10.277s -> 17.346s`
  - jit warmup: `11.366s -> 17.650s`

첫 요청도 전반적으로 좋아지지 않았다.

- `item-api`는 모든 커밋에서 `threshold on`이 더 느렸다.
- `member-api`는 `threshold on` 내부 비교에선 일부 개선이 있었지만, 가장 중요한 비교인 `before threshold off` 기준으로는 여전히 느렸다.

이번 실측 기준으로는 `threshold on`이 warmup 비용만 더 키우고, cold request 개선으로 충분히 회수되지는 못했다.

## 결론

1. `item-api`에서는 business warmup + JIT trigger가 실제 first request latency 개선으로 이어졌다.
2. `member-api`는 `HEAD`와 현재 작업 트리를 직접 분리 비교했을 때, 현재 수정본이 첫 `/auth/login`을 더 빠르게 만들지는 못했다.
3. 공통 warmup만으로는 두 서비스 모두 첫 요청 개선이 확인되지 않았다.
4. `Tier3/Tier4` threshold 하향 플래그는 이번 로컬 Docker 재현에선 순효과가 없었다.
5. 따라서 현재 기준으로는 "`item-api`의 business warmup은 유효했고, `member-api`는 직접 분리 측정상 추가 검증이 더 필요하며, threshold 플래그는 로컬 재현상 비권장"으로 정리하는 것이 맞다.

## 재현 방법

재현용 파일:

- [docker-compose.benchmark.yml](/Users/hyunwoo/IdeaProjects/occount-back-msa/docs/troubleshooting/warmup-jit-cold-start/docker-compose.benchmark.yml)
- [benchmark.sh](/Users/hyunwoo/IdeaProjects/occount-back-msa/docs/troubleshooting/warmup-jit-cold-start/benchmark.sh)
- [results-2026-04-26.tsv](/Users/hyunwoo/IdeaProjects/occount-back-msa/docs/troubleshooting/warmup-jit-cold-start/results-2026-04-26.tsv)
- [results-member-head-vs-current-2026-04-26.tsv](/Users/hyunwoo/IdeaProjects/occount-back-msa/docs/troubleshooting/warmup-jit-cold-start/results-member-head-vs-current-2026-04-26.tsv)

기본 실행:

```sh
./docs/troubleshooting/warmup-jit-cold-start/benchmark.sh
```

`threshold off/on`을 함께 비교하는 실행:

```sh
JAVA_OPTS_VARIANTS='default||threshold off
threshold_on|-XX:Tier3InvocationThreshold=10 -XX:Tier4InvocationThreshold=150|threshold on' \
./docs/troubleshooting/warmup-jit-cold-start/benchmark.sh
```

주요 옵션:

- `RUNS=5`: 반복 횟수 변경
- `SETTLE_SECONDS=8`: startup 로그 이후 추가 대기 시간 변경
- `WORKTREE_ROOT=/tmp/custom-bench`: worktree 경로 변경
- `RESULTS_FILE=/tmp/warmup-results.tsv`: 결과 파일 경로 변경
- `KEEP_WORKTREES=1`: 종료 후 worktree 유지
- `ITEM_API_PORT=28084`: item-api 외부 포트 변경
- `MEMBER_API_PORT=28083`: member-api 외부 포트 변경
- `TARGET_SERVICES='item-api member-api'`: 함께 측정할 서비스 지정
- `JAVA_OPTS_VARIANTS=...`: 같은 커밋을 여러 JVM 옵션으로 반복 측정

주의:

- 스크립트는 `docker`, `git worktree`, `curl`이 설치된 환경을 전제로 한다.
- 스크립트는 `mysql`, `kafka`, `item-api`, `member-api`를 같은 compose 스택으로 올린다.
- `threshold on` 비교는 startup 비용이 커서 측정 시간이 꽤 길다.
- 측정값은 Docker 캐시, 로컬 CPU 상태, 백그라운드 프로세스 영향으로 일부 달라질 수 있다.
