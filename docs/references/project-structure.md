# Project Structure & Module Organization

This is a Gradle multi-module Kotlin/Spring Boot backend.

- `core/core-common`: shared types, errors, and cross-cutting utilities.
- `domains/<domain>/`: domain-layer modules such as `*-domain`, `*-application`, `*-infrastructure`, and runnable `*-api` services.
- `gateway/api-gateway`: auth gateway service for login and registration.
- `modules/db`, `modules/kafka`: shared infrastructure modules.
- Source files live under `src/main/kotlin`; tests live under `src/test/kotlin`.

## 모듈 경계 원칙

- 도메인 로직은 `domain` 또는 `application`에 둔다.
- HTTP 어댑터는 `*-api`에 둔다.
- 인프라 연동은 `*-infrastructure` 또는 `modules/*`에 둔다.
- 각 `domains/*/*-api`는 독립 서비스이며 자체 `SecurityConfig`를 소유한다. 인증은 `gateway/api-gateway`에서 관리한다.
