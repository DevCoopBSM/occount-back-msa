# Testing Guidelines

- Tests use JUnit 5 via Gradle.
- Place tests beside the module they verify under `src/test/kotlin`.
- Prefer names ending in `Test` or `Tests`, matching existing examples like `ArchitectureBoundaryTest` and `DbKioskApplicationTests`.
- Add focused module-level tests for controller wiring, boundary rules, and service behavior when changing public flows.

## TDD 규칙

- 기능 추가나 버그 수정은 가능하면 TDD 순서로 진행한다: 실패하는 테스트 작성 → 최소 구현 → 리팩터링.
- 구현 코드보다 테스트를 먼저 작성해 요구사항과 기대 동작을 고정한다.
- 테스트 없이 동작을 추측하며 구현을 넓히지 않는다. 먼저 실패하는 테스트로 범위를 잠근다.
- 버그 수정은 재현 테스트를 먼저 추가하고, 그 테스트가 실패하는 것을 확인한 뒤 수정한다.
- 리팩터링은 기존 테스트가 보호하고 있는 상태에서만 진행한다. 리팩터링 중 동작 변경이 필요하면 테스트 기대값부터 갱신한다.

## 테스트/도메인 설계 원칙

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