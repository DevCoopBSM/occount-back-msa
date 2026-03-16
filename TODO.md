# 리팩토링 TODO (legacy -> v3)

기준:
- `Occount-back` -> `occount-back-v3`
- `Occount-self-server` -> `kiosk-server-v3`
- 비교 기준은 현재 컨트롤러/서비스/도메인 파일 존재 여부

## 1. Occount-back -> occount-back-v3

### P0 (핵심 기능 공백)
- [ ] `account` 도메인 전체 마이그레이션
- [ ] `verify` 도메인(메일 발송/본인인증) 마이그레이션
- [ ] `pg` 도메인 마이그레이션
- [ ] `transaction` 도메인 마이그레이션
- [ ] `investment` 도메인 마이그레이션

### P1 (운영 기능)
- [ ] `inquiry` 도메인(문의 등록/조회/답변) 마이그레이션
- [ ] `inventory` 도메인(재고 스냅샷/일자 조회/수동 생성) 마이그레이션
- [ ] `receipt` 도메인(기간별 조회/재고 편차 집계) 마이그레이션
- [ ] `notice` 도메인 CRUD 마이그레이션
- [ ] `admin/migration`(암호화 마이그레이션) 기능 이관

### P2 (연동/정리)
- [ ] `portone` pending verification 흐름 정리 및 재설계
- [ ] `toss` 수동 동기화 API 범위 정리(`POST /toss/` vs `POST /items/sync`)
- [ ] role/권한 정책 재정의(레거시 권한 + v3 JWT 구조 통합)

### 레거시 대비 누락으로 보이는 주요 API 묶음
- [ ] 계정/학생 관리: `/account/user/*`, `/account/student/*`, `/account/users/search`
- [ ] 비밀번호 재설정/인증: `/verify/send`, `/verify/identity`, `/auth/pwChange/{resetToken}`
- [ ] 투자금: `/investment/*` (등록/목록/상세/승인/취소/삭제/일괄업데이트)
- [ ] 문의: `/inquiry/new`, `/inquiry/`, `/inquiry/user`, `/inquiry/{id}/answer`
- [ ] 재고/영수증: `/inventory/*`, `/receipt/receiptcheck`, `/receipt/stockvariance`
- [ ] 트랜잭션: `/transaction/barcode|scan|charge|charges|pay|chargelog|paylog|admin/*`
- [ ] 결제/웹훅: `/pg/confirm|webhook|refund|self-charge/*`
- [ ] 공지: `/notices`

## 2. Occount-self-server -> kiosk-server-v3

### P0 (기능 유지 필요 여부 우선 확인)
- [ ] 비밀번호 변경/초기화 플로우 정리 후 반영
- [ ] 사용자 상세 조회 API 정합성 점검
- [ ] 아이템 조회 API 호환성 점검

### P1 (레거시 전용 기능 중 v3 미반영 추정)
- [ ] 이벤트 상품 조회 API 이관 검토 (`/event-item/get-item`)
- [ ] AI 추천 API 이관 검토 (`/item/ai/suggest`)
- [ ] 논바코드 조회 엔드포인트 호환 레이어 추가 검토 (`/items/no-barcode`, `/non-barcode-item`)
- [ ] 포인트 결제 단일 API(`/pay`) 필요 여부 검토

### P2 (운영/데이터)
- [ ] charge/pay log 조회 요구사항 점검(현재 v3는 `/payments/history`, `/payments/charges` 중심)
- [ ] 레거시 엔드포인트 alias 유지 여부 결정(프론트 하위호환)

## 3. 멀티 모듈 구조 전환 작업

### 완료
- [x] `occount-backend` 루트를 멀티 모듈로 전환
- [x] `GGEE-Backend` 패턴을 참고해 레이어드 모듈 골격 생성
- [x] `occount`/`kiosk` 각각 `api/application/domain/infrastructure/bootstrap` 모듈 분리
- [x] 공통 모듈 `common` 추가

### 다음 작업
- [ ] `occount-back-v3` 소스 이관 (패키지/의존성 정리)
- [ ] `kiosk-server-v3` 소스 이관 (패키지/의존성 정리)
- [ ] 공통 유틸/보안/에러 모델을 `common`으로 추출
- [ ] module boundary 테스트 작성 (레이어 역참조 방지)
- [ ] CI 파이프라인을 멀티 모듈 빌드 기준으로 변경
