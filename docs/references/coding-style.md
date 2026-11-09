# Coding Style & Naming Conventions

- Follow `.editorconfig`: UTF-8, LF, final newline.
- Java files use tabs with width 4; keep Kotlin formatting consistent with the existing codebase.
- Use lowercase package names such as `devcoop.occount.payment.api.kiosk.payment`.
- Use PascalCase for classes, `*Controller`, `*Service`, `*Config`, `*Response`, `*Request` suffixes where applicable.
- Keep module boundaries explicit: domain logic in `domain` or `application`, HTTP adapters in `*-api`, infrastructure integrations in `*-infrastructure` or `modules/*`.

## 안전한 변경 정책 (agent guidance)

- 무조건 기존 코드베이스의 구조, 패턴, 아키텍처를 따른다.
- 환경변수의 기본값은 코드, 설정, 문서에 임의로 명시하지 않는다.
- 새 프레임워크를 도입하기보다 기존 패턴을 확장하는 방식을 우선한다.
- 제안보다 저장소 규칙이 우선한다.
- CLI 명령으로 build/tests 통과를 항상 확인한다.
