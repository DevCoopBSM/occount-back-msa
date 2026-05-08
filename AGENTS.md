# Repository Guidelines

## Project Structure & Module Organization
This is a Gradle multi-module Kotlin/Spring Boot backend.

- `core/core-common`: shared types, errors, and cross-cutting utilities.
- `domains/<domain>/`: domain-layer modules such as `*-domain`, `*-application`, `*-infrastructure`, and runnable `*-api` services.
- `gateway/api-gateway`: auth gateway service for login and registration.
- `modules/db`, `modules/kafka`: shared infrastructure modules.
- Source files live under `src/main/kotlin`; tests live under `src/test/kotlin`.

## Build, Test, and Development Commands
- `./gradlew compileKotlin --console=plain`: fast compile check across modules.
- `./gradlew test --console=plain`: runs JUnit 5 test suites.
- `./gradlew clean build --console=plain`: full build, including packaging.

### Local Development (Docker Compose)
- `docker compose up -d`: start all services (MySQL, Kafka, all APIs, gateway).
- `docker compose up -d api-gateway`: start only the gateway.
- `docker compose up -d member-api`: start only the member API.
- `docker compose up -d item-api`: start only the item API.
- `docker compose up -d order-api`: start only the order API.
- `docker compose up -d payment-api`: start only the payment API.
- `docker compose down`: stop all services.
- `docker compose logs -f <service>`: tail logs for a specific service.

Use module-scoped tasks when changing one area, for example `./gradlew :domains:payment:payment-api:compileKotlin`.

## Coding Style & Naming Conventions
- Follow `.editorconfig`: UTF-8, LF, final newline.
- Java files use tabs with width 4; keep Kotlin formatting consistent with the existing codebase.
- Use lowercase package names such as `devcoop.occount.payment.api.kiosk.payment`.
- Use PascalCase for classes, `*Controller`, `*Service`, `*Config`, `*Response`, `*Request` suffixes where applicable.
- Keep module boundaries explicit: domain logic in `domain` or `application`, HTTP adapters in `*-api`, infrastructure integrations in `*-infrastructure` or `modules/*`.

## Testing Guidelines
- Tests use JUnit 5 via Gradle.
- Place tests beside the module they verify under `src/test/kotlin`.
- Prefer names ending in `Test` or `Tests`, matching existing examples like `ArchitectureBoundaryTest` and `DbKioskApplicationTests`.
- Add focused module-level tests for controller wiring, boundary rules, and service behavior when changing public flows.

### TDD 규칙

- 기능 추가나 버그 수정은 가능하면 TDD 순서로 진행한다: 실패하는 테스트 작성 → 최소 구현 → 리팩터링.
- 구현 코드보다 테스트를 먼저 작성해 요구사항과 기대 동작을 고정한다.
- 테스트 없이 동작을 추측하며 구현을 넓히지 않는다. 먼저 실패하는 테스트로 범위를 잠근다.
- 버그 수정은 재현 테스트를 먼저 추가하고, 그 테스트가 실패하는 것을 확인한 뒤 수정한다.
- 리팩터링은 기존 테스트가 보호하고 있는 상태에서만 진행한다. 리팩터링 중 동작 변경이 필요하면 테스트 기대값부터 갱신한다.

### 테스트/도메인 설계 원칙

- 테스트는 내부 구현을 드러내지 않는다.
    - private/helper 메서드, 내부 호출 순서, 위임 여부 자체를 검증하지 않는다.
    - 사용자가 관찰 가능한 입력/출력, 상태 변화, 발행 이벤트, 외부 계약을 기준으로 검증한다.

- 테스트에서는 바깥만 모킹하고 안쪽은 진짜를 사용한다.
    - DB, 외부 API, 메시징, 시간, 랜덤, 파일 I/O 같은 프로세스 밖 경계만 test double로 대체한다.
    - application/domain 내부 협력 객체는 가능하면 실제 구현을 조합해서 테스트한다.
    - Mockito/MockK 남용보다 fake/in-memory 구현을 우선한다.

- 비즈니스 로직은 서비스보다 데이터 자체에 우선 배치한다.
    - 상태 전이, 불변식, 계산 규칙, 유효성 규칙은 domain object/entity/value object가 직접 표현하도록 설계한다.
    - use case/service는 오케스트레이션과 경계 연결에 집중하고, 핵심 업무 규칙을 과도하게 소유하지 않는다.
    - getter 확인 위주의 빈약한 도메인보다, 행위를 가진 객체와 그 행위를 검증하는 테스트를 우선한다.

## Compile Verification (REQUIRED)
After **every** code change, always run a compile check before considering the task done:
```
./gradlew compileKotlin compileTestKotlin --console=plain
```
For changes scoped to a single domain, prefer the module-scoped variant, e.g.:
```
./gradlew :domains:order:order-application:compileKotlin :domains:order:order-application:compileTestKotlin --console=plain
```
Do not skip this step. Compile errors in related files (imports, constructor changes, deleted classes) are a common source of breakage that must be caught before finishing.

For pull requests:
- describe the changed modules and affected flows,
- link the related issue or ticket,
- include test/compile results,
- add request/response examples when changing API behavior.

## API Specification

`docs/API_SPEC.yaml` 은 프론트엔드와 소통하는 **유일한 API 계약서**입니다.

아래 변경이 발생하면 **반드시** 해당 파일을 함께 수정해야 합니다:
- 컨트롤러에 엔드포인트 추가 / 삭제 / 경로 변경
- 요청(Request) 또는 응답(Response) 필드 추가 / 삭제 / 타입 변경
- 헤더 요구사항 변경 (`X-Kiosk-Id`, `Authorization` 등)
- 인증 방식 변경 (PERMIT_ALL / OPTIONAL_AUTH / AUTHENTICATED / ADMIN_ONLY)
- HTTP 상태 코드 변경

수정 대상 컨트롤러:
- `domains/member/member-api/.../AuthController.kt`
- `domains/member/member-api/.../MemberController.kt`
- `domains/item/item-api/.../ItemController.kt`
- `domains/order/order-api/.../OrderController.kt`
- `domains/payment/payment-api/.../PaymentController.kt`
- `domains/payment/payment-api/.../WalletController.kt`
- `gateway/api-gateway/.../AuthenticationPolicy.kt` (인증 정책 변경 시)

## Security & Configuration Tips
- Copy `.env.example` when preparing local configuration.
- Do not commit secrets from `.env`.
- When adding new controllers, keep auth in `gateway/api-gateway`. Each `domains/*/*-api` module is its own service and owns its own `SecurityConfig`.

### 트랜잭션 규칙 (엄격)

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

## 새 API endpoint 추가 체크리스트

1) 테스트 추가:
    - Web layer: `WebTestClient` (`src/test/.../api/controller/`)
    - Unit tests: `src/test/.../application/usecase/`
2) 실행:
    - `./gradlew test` and `./gradlew check`
3) API 문서 반영:
    - API를 추가하거나 수정한 경우 `docs/API_SPEC.yaml`을 반드시 함께 수정한다.
    - 
## 안전한 변경 정책 (agent guidance)

- 무조건 기존 코드베이스의 구조, 패턴, 아키텍처를 따른다.
- 환경변수의 기본값은 코드, 설정, 문서에 임의로 명시하지 않는다.
- 새 프레임워크를 도입하기보다 기존 패턴을 확장하는 방식을 우선한다.
- 제안보다 저장소 규칙이 우선한다.
- 위 CLI 명령으로 build/tests 통과를 항상 확인한다.

## 커밋 규칙

- 사용자가 커밋을 요청하면 반드시 `COMMIT.md`를 먼저 확인하고 그 규칙을 따른다.
- 커밋은 코드 변경 역할과 책임에 따라 작고 명확한 단위로 나눈다.
- 서로 다른 성격의 변경(예: 리팩터링, 기능 추가, 버그 수정, 테스트, 문서 수정)은 가능하면 분리해서 커밋한다.
- 커밋 메시지는 반드시 한국어로 작성한다.
- 사용자가 명시적으로 요청하지 않으면 `commit`, `amend`, `push`를 수행하지 않는다.
