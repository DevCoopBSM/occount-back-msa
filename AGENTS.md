# Repository Guidelines

Gradle 멀티모듈 Kotlin/Spring Boot 백엔드. 상세 규칙은 `docs/references/`의 주제별 문서에 분해되어 있다. 작업 성격에 맞는 문서를 먼저 읽고 진행한다.

## 핵심 규칙 (항상 적용)

### Compile Verification (REQUIRED)

**모든** 코드 변경 후 작업 완료 전 반드시 컴파일 검증을 실행한다:

```
./gradlew compileKotlin compileTestKotlin --console=plain
```

단일 도메인 범위 변경이면 모듈 스코프 변형을 우선한다, 예:

```
./gradlew :domains:order:order-application:compileKotlin :domains:order:order-application:compileTestKotlin --console=plain
```

이 단계를 건너뛰지 않는다. import, 생성자 변경, 삭제된 클래스 등 연관 파일의 컴파일 에러는 흔한 파손 원인이므로 마무리 전에 잡아야 한다.

### 기본 명령

- `./gradlew compileKotlin --console=plain`: 모듈 전체 빠른 컴파일 체크.
- `./gradlew test --console=plain`: JUnit 5 테스트 실행.
- `./gradlew clean build --console=plain`: 패키징 포함 전체 빌드.

### 변경 정책

- 무조건 기존 코드베이스의 구조, 패턴, 아키텍처를 따른다. 새 프레임워크 도입보다 기존 패턴 확장을 우선한다.
- 제안보다 저장소 규칙이 우선한다.
- API 동작을 추가/변경하면 `docs/API_SPEC.yaml`을 반드시 함께 수정한다 (→ `docs/references/api-spec.md`).

## 상세 참조 문서 (`docs/references/`)

| 문서 | 언제 읽나 |
|---|---|
| [`project-structure.md`](docs/references/project-structure.md) | 모듈 구성·경계, 어디에 무엇을 둘지 |
| [`build-and-dev.md`](docs/references/build-and-dev.md) | 빌드/테스트 명령, Docker Compose 로컬 실행, 컴파일 검증, PR 요건 |
| [`coding-style.md`](docs/references/coding-style.md) | 코딩 스타일·네이밍 컨벤션, 안전한 변경 정책 |
| [`testing.md`](docs/references/testing.md) | 테스트 가이드, TDD 규칙, 테스트/도메인 설계 원칙, 새 API endpoint 체크리스트 |
| [`api-spec.md`](docs/references/api-spec.md) | `docs/API_SPEC.yaml` 계약, JSON snake_case 네이밍 규약, 수정 대상 컨트롤러 |
| [`kafka.md`](docs/references/kafka.md) | Kafka 토픽 네이밍·통합 원칙, event/command 구분, Producer/Consumer 규칙, 새 토픽 체크리스트 |
| [`transactions.md`](docs/references/transactions.md) | 트랜잭션 사용 규칙(엄격), Reactive 트랜잭션, Security & Configuration |
| [`commits.md`](docs/references/commits.md) | 커밋 분리·메시지 규칙 (`COMMIT.md` 준수) |
