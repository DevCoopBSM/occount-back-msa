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
- **같은 도메인이 발행하는 결과 이벤트는 하나의 토픽**으로 묶는다 (`<domain>.event.<scope>.v1`).
- 분기는 페이로드가 아니라 `eventType` **헤더**로 한다 (`DomainEventHeaders.EVENT_TYPE`).
- 커맨드와 이벤트는 절대 같은 토픽에 섞지 않는다 (방향과 의도가 반대).

## 파티션·키 규칙

- **partitions=1**이 표준. 수평 확장 안 한다.
- **파티션 키는 애그리거트 식별자**로 일관 사용 (현재는 모든 토픽 `orderId.toString()`).
- 컨슈머 `concurrency` 미지정(기본 1) → 단일 스레드 직렬 처리.

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
