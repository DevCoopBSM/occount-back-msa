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

## Commit & Pull Request Guidelines
Git history is not available in this workspace, so no repository-specific commit pattern could be verified. Use short imperative commit messages such as `Move service controllers into domain api modules`.

For pull requests:
- describe the changed modules and affected flows,
- link the related issue or ticket,
- include test/compile results,
- add request/response examples when changing API behavior.

## Security & Configuration Tips
- Copy `.env.example` when preparing local configuration.
- Do not commit secrets from `.env`.
- When adding new controllers, keep auth in `gateway/api-gateway`. Each `domains/*/*-api` module is its own service and owns its own `SecurityConfig`.
