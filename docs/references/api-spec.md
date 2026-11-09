# API Specification

`docs/API_SPEC.yaml` 은 프론트엔드와 소통하는 **유일한 API 계약서**입니다.

아래 변경이 발생하면 **반드시** 해당 파일을 함께 수정해야 합니다:

- 컨트롤러에 엔드포인트 추가 / 삭제 / 경로 변경
- 요청(Request) 또는 응답(Response) 필드 추가 / 삭제 / 타입 변경
- 헤더 요구사항 변경 (`X-Kiosk-Id`, `Authorization` 등)
- 인증 방식 변경 (PERMIT_ALL / OPTIONAL_AUTH / AUTHENTICATED / ADMIN_ONLY)
- HTTP 상태 코드 변경

## 수정 대상 컨트롤러

- `domains/member/member-api/.../AuthController.kt`
- `domains/member/member-api/.../MemberController.kt`
- `domains/item/item-api/.../ItemController.kt`
- `domains/order/order-api/.../OrderController.kt`
- `domains/payment/payment-api/.../PaymentController.kt`
- `domains/payment/payment-api/.../WalletController.kt`
- `gateway/api-gateway/.../AuthenticationPolicy.kt` (인증 정책 변경 시)

## JSON 네이밍 규약 (외부 계약)

- **외부 API 계약(요청/응답 JSON)의 필드 키는 `snake_case`다.**
  - 모든 `*-api` 모듈은 `application.yaml`에 `spring.jackson.property-naming-strategy: SNAKE_CASE`를 설정해 이를 강제한다 (member/item/order/payment/suggestion).
  - Kotlin DTO 프로퍼티는 평소대로 `camelCase`로 작성하고, 와이어 포맷으로 직렬화/역직렬화될 때 Jackson이 `snake_case`로 변환한다. `@JsonProperty`로 키를 수동 지정하지 않는다.
  - 예: `proposalId` → `proposal_id`, `likeCount` → `like_count`. 연속 대문자는 분리되지 않는다 — `typeNSeq` → `type_nseq`(❌ `type_n_seq` 아님).
- **컨트롤러 테스트도 실제 계약과 동일한 규칙으로 검증한다.**
  - standalone `MockMvc`는 기본 컨버터가 camelCase이므로, `jacksonMapperBuilder().propertyNamingStrategy(SNAKE_CASE)` 매퍼를 주입한 `JacksonJsonHttpMessageConverter`를 명시적으로 설정한다.
  - 요청 본문과 `jsonPath` 단언은 `snake_case` 키로 작성한다. 선례: `MemberApiTestSupport`, `AripickControllerTest`.
  - 이를 위해 테스트 의존성에 `tools.jackson.module:jackson-module-kotlin`이 필요하다.
- `docs/API_SPEC.yaml`의 필드명도 `snake_case`로 기술한다.

## 새 API endpoint 추가 체크리스트

1) 테스트 추가:
    - Web layer: `WebTestClient` (`src/test/.../api/controller/`)
    - Unit tests: `src/test/.../application/usecase/`
2) 실행:
    - `./gradlew test` and `./gradlew check`
3) API 문서 반영:
    - API를 추가하거나 수정한 경우 `docs/API_SPEC.yaml`을 반드시 함께 수정한다. (자세한 규약은 `api-spec.md` 참고)