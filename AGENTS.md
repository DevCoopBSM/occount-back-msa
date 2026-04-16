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
- `./gradlew :gateway:api-gateway:bootRun --console=plain`: starts the auth gateway locally.
- `./gradlew :domains:member:member-api:bootRun --console=plain`: starts the member API locally.
- `./gradlew :domains:item:item-api:bootRun --console=plain`: starts the item API locally.
- `./gradlew :domains:order:order-api:bootRun --console=plain`: starts the order API locally.
- `./gradlew :domains:payment:payment-api:bootRun --console=plain`: starts the payment API locally.
- `./gradlew clean build --console=plain`: full build, including packaging.

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

## Safe Change Policy
- Keep diffs minimal and changes localized.
- Follow the existing codebase structure, patterns, and architecture.
- Do not introduce arbitrary default values for environment variables in code, config, or docs.
- Prefer extending existing patterns over introducing new frameworks.
- Repository rules take precedence over suggestions.

## Commit & Pull Request Guidelines
- If a commit is requested, check `COMMIT.md` first and follow its rules.
- **커밋은 반드시 관심사별로 분리해서 여러 개로 나눠야 한다. 하나의 커밋에 여러 관심사를 절대 섞지 말 것.**
  - fix, refactor, feat, test, chore 등 타입이 다르면 무조건 별도 커밋으로 분리한다.
  - 영향받는 모듈이나 레이어가 다르면 별도 커밋으로 분리한다.
  - 관련 테스트 코드도 구현 커밋과 별도로 분리한다.
- Split commits into small, clear units by role and responsibility.
- Write commit messages in Korean.
- Do not run `commit`, `amend`, or `push` unless the user explicitly asks.

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
