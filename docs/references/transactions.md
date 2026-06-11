# 트랜잭션 규칙 (엄격)

- 트랜잭션은 기본값이 아니다. 무분별한 트랜잭션 사용을 금지한다.
    - 단일 insert/update/delete 1회, 단순 CRUD, read-only 흐름에는 트랜잭션을 사용하지 않는다.
    - 트랜잭션은 리소스/락/컨텍스트 비용이 있으므로 "필요한 곳에만 최소 범위로" 적용한다.

- 원자성(atomicity)이 반드시 필요한 경우에만 트랜잭션을 사용한다.
    - "A가 실패하면 B도 반드시 롤백"되어야 하는 원자성(all-or-nothing)이 요구될 때만 트랜잭션을 사용한다.
    - 예시 (트랜잭션 필요):
        - 2개 이상 DB write가 하나의 업무 단위로 묶여야 함
          (예: 주문 생성 + 재고 차감 + 결제 상태 기록)
        - 비즈니스 불변식(invariant)을 지키기 위한 다중 쓰기
          (예: 포인트 차감 + 이력 적재)

- 예시 (트랜잭션 불필요):
    - 단일 저장/업데이트 1회
    - 조회-only
    - 캐시 갱신, 로그 적재, 알림 발송, 메시지 발행 같은 부수효과
      → 실패해도 본 DB 작업을 롤백시키면 안 되는 작업은 트랜잭션 밖으로 분리
      (이벤트/비동기/아웃박스 패턴 등 고려)

- Reactive 트랜잭션 작성 방식:
    - R2DBC/WebFlux에서는 `TransactionalOperator`를 사용한다.
    - DB 트랜잭션 안에 원격 호출(HTTP), 메시지 publish(Kafka), 파일 I/O를 포함하지 않는다.
    - 트랜잭션 경계는 "원자성이 필요한 DB write"까지만 최소화한다.

- 롤백 정책:
    - 트랜잭션 파이프라인에서 에러가 발생하면 error 신호가 전파되어 롤백되도록 작성한다.
    - 트랜잭션 내부에서 `onErrorResume`로 에러를 삼키지 않는다(명시적 보상 트랜잭션 제외).

- 트랜잭션 추가 전 체크리스트:
    1) 2개 이상 DB write가 있으며 all-or-nothing이 반드시 필요하다.
    2) 부분 성공이 비즈니스 불변식을 깨거나 잘못된 상태를 외부에 노출한다.
    3) 트랜잭션 경계에 원격 호출/메시징/블로킹 I/O가 포함되지 않는다.

## Security & Configuration Tips

- Copy `.env.example` when preparing local configuration.
- Do not commit secrets from `.env`.
- When adding new controllers, keep auth in `gateway/api-gateway`. Each `domains/*/*-api` module is its own service and owns its own `SecurityConfig`.
