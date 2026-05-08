# 결제 완료 후 주문 응답 지연 (~2.7s)

## 증상

키오스크에서 카드 결제 단말기 응답이 화면에 표시된 직후, 주문 완료(SSE `COMPLETED`) 도달까지 거의 3초가 걸림. Datadog 트레이스에서 `order.payment-requested.v1` consume span이 단독으로 2.73초로 측정됨.

## 진단 절차

1. Datadog에서 단일 결제 trace 확인 — `order.payment-requested.v1` process span 2.73초, 다른 span은 짧음
2. 해당 process span 내부에 별도 child span이 없음 → 시간 분포 불가시
3. payment 측 outbox 인덱스/누적 점검 — 정상(`outbox_event` 369행, `published=false` 0건). 인덱스/CDC 가설 기각
4. 코드 정적 분석으로 process span 안의 DB 트랜잭션 횟수 카운트 → **메시지 1건 처리에 별도 commit 6번**

## 원인

`OrderPaymentEventListener.executeVanPayment()` 한 번 처리 시 발생하던 트랜잭션:

| # | 위치 | 작업 |
|---|---|---|
| 1 | `OrderPaymentEventListener.executeVanPayment` | `consumedEventRepository.existsById` (멱등성 SELECT) |
| 2 | `OrderPaymentExecutionRepositoryImpl.startProcessing` | `@Transactional` + `SELECT … FOR UPDATE` + INSERT |
| 3 | `OrderPaymentExecutionRepositoryImpl.isCancellationRequested` | `@Transactional(readOnly)` + SELECT |
| — | `paymentFacade.execute` | VAN 단말기 동기 통신 |
| 4 | `OrderPaymentExecutionRepositoryImpl.markCompleted` | `@Transactional` + `SELECT … FOR UPDATE` + UPDATE |
| 5 | `OutboxEventPublisher.publish` | outbox INSERT (자체 트랜잭션) |
| 6 | `OrderPaymentEventListener.markProcessed` | `consumed_event` INSERT (자체 트랜잭션) |

- 각 commit마다 Hikari connection acquire/release + InnoDB redo log fsync (디스크/네트워크 따라 1~100ms)
- `FOR UPDATE` row lock 추가 비용
- `ExecuteVanPaymentUseCase`에 클래스 레벨 `@Transactional`이 없어 메서드별로 따로 commit
- VAN child span이 없어서 시간이 어디로 새는지 트레이스에서 식별 불가

## 해결

### 1. 트랜잭션 통합 (`ExecuteVanPaymentUseCase`)

VAN 통신을 사이에 두고 두 개의 명시적 트랜잭션으로 정리. VAN 통신은 트랜잭션 밖이라 connection을 점유하지 않음.

- tx1: `recordConsumption` + `startProcessing` (멱등성 키 + 결제 시작 상태 기록)
- VAN 통신 (tx 외부)
- tx2: `markCompleted` + outbox `publish` (또는 실패 시 `markFailed`/`markCancelled` + 실패 이벤트)

`TransactionTemplate`을 통해 명시적으로 트랜잭션 경계 지정 (다른 도메인 use case와 동일한 패턴).

**효과**: 별도 commit 6회 → 2회.

### 2. `SELECT … FOR UPDATE` 제거

`startProcessing` / `markCompleted` 등에서 비관적 락 제거. 사유:

- 멱등성 키(`consumed_event` PK)가 같은 메시지의 동시 처리를 막음
- 같은 row UPDATE 자체가 InnoDB row lock으로 직렬화되므로 추가 락 불필요
- 결제 시작/취소 race는 `requestCancellation`(별도 트랜잭션, FOR UPDATE 유지)이 보호

`findByOrderIdForUpdate` 호출은 `requestCancellation` 한 곳만 남김.

### 3. listener의 `isProcessed` SELECT 제거

`saveConsumedEvent`가 PK 충돌(`DataIntegrityViolationException`) 시 `DuplicateEventException`을 던지도록 변경. listener가 SELECT로 사전 체크하지 않아도 INSERT 단계에서 자동 감지됨.

`executeVanPayment` 흐름은 콜백(`recordConsumption`) 패턴으로 트랜잭션 첫 단계에 멱등성 키 INSERT를 묶음. 다른 listener(`cancelPendingPayment` / `compensatePayment`)는 보수적으로 기존 패턴 유지.

### 4. `VanTerminalClient`에 OTel child span 추가

`io.micrometer.tracing.Tracer`를 nullable로 주입(다른 도메인의 트레이싱 패턴과 동일). 다음 구간을 child span으로 노출:

- `van.approve` / `van.refund` (전체 트랜잭션)
- `van.connect` (소켓 연결/재연결)
- `van.send` (요청 전송)
- `van.wait_response` (응답 대기 + ACK 처리)

이제 Datadog에서 process span 내부의 시간 분포가 즉시 보이며, 추가 회귀 발생 시 어느 단계인지 식별 가능.

## 변경 파일

- `domains/payment/payment-application/.../usecase/payment/ExecuteVanPaymentUseCase.kt` — 트랜잭션 통합 + `recordConsumption` 콜백 수신
- `domains/payment/payment-application/.../exception/DuplicateEventException.kt` — 신규
- `domains/payment/payment-infrastructure/.../persistence/execution/OrderPaymentExecutionRepositoryImpl.kt` — `@Transactional`/`FOR UPDATE` 제거 (결제 흐름)
- `domains/payment/payment-infrastructure/.../event/OrderPaymentEventListener.kt` — `isProcessed` 제거 + 콜백 패턴
- `domains/payment/payment-infrastructure/.../client/van/VanTerminalClient.kt` — OTel child span 추가
- `domains/payment/payment-infrastructure/.../client/van/VanTerminalRegistry.kt` — `Tracer` 주입
- `domains/payment/payment-infrastructure/build.gradle` — `io.micrometer:micrometer-tracing` 추가

## 검증

```bash
./gradlew :domains:payment:payment-application:test \
          :domains:payment:payment-infrastructure:test
```

Datadog에서 다음을 확인:

- `order.payment-requested.v1` process span 안에 `van.approve`(필요시 `van.connect`/`van.send`/`van.wait_response`) child span이 보일 것
- VAN 응답 이후 `van.approve` 종료 → process span 종료까지 차이가 줄었을 것 (commit 횟수 감소 효과)

## 추가 고려 (이번 변경 범위 밖)

- 같은 outbox 패턴을 쓰는 모든 도메인 listener의 `isProcessed` 사전 SELECT를 콜백 패턴으로 일괄 정리
- `@Scheduled` 기본 단일 스레드 풀(`spring.task.scheduling.pool.size=1`)이 `OutboxRelay` + 도메인 sweeper와 충돌 가능 — 풀 사이즈 분리 검토
- `OutboxRelay`의 직렬 동기 `send().get()` → 비동기 배치 + `markPublished` 일괄 UPDATE
- 장기적으로 polling outbox → Debezium CDC 전환
