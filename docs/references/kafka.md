# Kafka 토픽 / 이벤트 규칙

## 토픽 네이밍 컨벤션

```
<도메인>.<event|command>.<설명>.v<버전>
```

**구성 요소**

| 세그먼트 | 의미 | 규칙 |
|---|---|---|
| 도메인 | 시스템 영역 | 제품명/팀명 사용 금지. 시스템의 근본 영역(`member`, `order`, `payment`, `item`)으로만 표현 |
| 분류 | 메시지 성격 | `event` = 발행자가 알리는 사실(과거형), `command` = 수신자가 처리해야 할 요청(명령형) |
| 설명 | 토픽이 담는 데이터 | 단일 메시지면 구체 명사(`registered`, `requested`), 통합 토픽이면 공통 스코프(`stock`, `compensation`). 도메인이 한 종류만 발행/수신해 모호함이 없으면 생략 가능 |
| 버전 | 스키마 버전 | `.v1`. 호환 깨지는 변경 시 `.v2` 새 토픽으로 발행하고 컨슈머가 듀얼 구독으로 무중단 마이그레이션 |

**event vs command 구분 기준**

- **event** — 발행자가 "이런 일이 일어났다"는 사실을 통보. 이미 일어난 일이라 거부 불가. 발행자는 누가 듣는지 모름.
  - 예: `payment.event.v1` (결제 완료/실패), `order.event.requested.v1` (주문 발생)
- **command** — 발행자가 특정 도메인에 "X 해줘"라고 요청. 수신자가 거부/실패할 수 있음. 발행자는 수신 도메인을 의식.
  - 예: `payment.command.v1` (결제 요청/취소/보상), `item.command.compensation.v1` (재고 보상)

## 토픽 통합 원칙

- **같은 애그리거트가 받는 커맨드는 하나의 토픽**으로 묶는다 (`<domain>.command.v1`).
  - 토픽이 분리되면 컨슈머 스레드가 달라 같은 애그리거트에 대한 요청이 직렬화되지 않음 → 동시성 충돌 위험.
  - **예외 — 결제 취소(`payment.command.cancel.v1`)는 의도적으로 분리한다.** 취소는 진행 중 결제를
    "선점"해야 하는데, 같은 토픽이면 취소 커맨드가 진행 중 결제(30초 블로킹 VAN 호출) 뒤에 큐잉되어
    단말 중단이 항상 늦는다. 별도 토픽·리스너(독립 스레드)에서 처리해야 결제 대기 중 즉시 단말을
    중단시킬 수 있다. 순서 역전(취소가 결제보다 먼저 소비)은 `OrderPaymentExecution` 상태머신이
    `CANCELLED_BEFORE_START`로 흡수하므로 안전하다.
- **같은 도메인이 발행하는 결과 이벤트는 하나의 토픽**으로 묶는다 (`<domain>.event.<scope>.v1`).
- 분기는 페이로드가 아니라 `eventType` **헤더**로 한다 (`DomainEventHeaders.EVENT_TYPE`).
- 커맨드와 이벤트는 절대 같은 토픽에 섞지 않는다 (방향과 의도가 반대).

## 파티션·키 규칙

- **partitions=1**이 기본. 키는 **애그리거트 식별자(`orderId.toString()`)**, 컨슈머 `concurrency` 미지정(기본 1) → 단일 스레드 직렬 처리. 대부분의 토픽은 이 기본을 따른다.
- **예외 — `payment.command.v1` (VAN 결제 단말 병목 대응):**
  - 키 = **`kioskId`** (애그리거트가 아니라 물리적 경합 자원인 VAN 단말 단위). 같은 단말 결제는 직렬, 다른 단말은 병렬.
  - **partitions=6**, 컨슈머 `concurrency=6`.
  - 사유: VAN 카드결제는 동기식(타임아웃 30초)이라, partitions=1이면 한 단말의 느린/실패 결제가 단일 컨슈머를 막아(head-of-line) 전 키오스크 결제가 멈춘다(2026-06 인시던트). kioskId 파티셔닝으로 단말 간 격리.
  - 결과 이벤트(`payment.event.v1` 등)는 여전히 `orderId` 키 — 변경 대상은 커맨드 토픽뿐.

## 에러 처리 (재시도·DLT)

- 리스너 공통 에러 핸들러(`KafkaConfig.kt`)는 **재시도 0회 + 즉시 `<topic>.DLT` 격리**(`DefaultErrorHandler(DeadLetterPublishingRecoverer, FixedBackOff(0, 0))`).
- 사유: Spring 기본값 `FixedBackOff(0, 9)`(10회 재시도)가 실패할 결제를 반복 시도하며 컨슈머를 장시간 점유(30초×10≈5분)했다. 비즈니스성 실패(결제거절·타임아웃)는 재시도해도 무의미하므로 즉시 DLT로 격리해 파티션을 비운다.
- DLT 토픽은 `<원본토픽>.DLT` 규약. 재처리는 DLT를 검사 후 수동/배치로.

## Consumer / Producer 규칙

- **Consumer name**(= `groupId` = `consumed_event` PK prefix)은 토픽 단위로 일관 명명.
  - 예: `payment.command.v1` → `payment-command-v1`
- **멱등성**은 `IdempotencyTracker`(`modules/db/.../outbox/IdempotencyTracker.kt`)로 처리. 직접 `consumed_event` 다루지 말 것.
- **Producer 발행**은 항상 `EventPublisher.publish(topic, key, eventType, payload)`를 통해 outbox에 저장. `KafkaTemplate` 직접 호출 금지.
- Producer는 `enable.idempotence=true`, `acks=all` 설정 (`KafkaConfig.kt`).

## 새 토픽 추가 체크리스트

1. 같은 애그리거트의 기존 토픽이 있으면 통합 가능한지 먼저 검토. 새 토픽 추가는 마지막 수단.
2. `event`/`command` 방향이 명확한가?
3. `DomainTopics.kt`에 상수 추가, 네이밍 컨벤션 준수
4. `DomainEventTypes.kt`에 eventType 상수 추가 (페이로드 클래스명과 일치)
5. 페이로드 데이터 클래스를 `core/core-common/.../event/`에 정의
6. Producer는 `EventPublisher.publish(...)`로 발행
7. Consumer는 통합 listener에 `when (eventType)` 분기 추가, `IdempotencyTracker` 사용
