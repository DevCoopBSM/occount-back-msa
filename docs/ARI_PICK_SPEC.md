# AriPick 기능 상세 명세

## 1. 도메인 개요
- 목적: 사용자 상품 제안(아직 확정되지 않은 상품 요청) 관리
- 상태값:
  - `검토중`
  - `승인됨`
  - `거절됨`
- 기본 등록 상태: `검토중`

## 2. 인증/권한 정책
### PERMIT_ALL
- `GET /api/v3/ari-pick`
- `GET /api/v3/ari-pick/{proposalId}`
- `GET /api/v3/ari-pick/stats`

### AUTHENTICATED
- `GET /api/v3/ari-pick/foods`
- `POST /api/v3/ari-pick`
- `DELETE /api/v3/ari-pick/{proposalId}`
- `POST /api/v3/ari-pick/{proposalId}/like`

### ADMIN_ONLY
- `PATCH /api/v3/ari-pick/{proposalId}/approve`
- `PATCH /api/v3/ari-pick/{proposalId}/reject`
- `PATCH /api/v3/ari-pick/{proposalId}/pending`
- `DELETE /api/v3/ari-pick/{proposalId}/admin`
- `GET /api/v3/ari-pick/blocked-keywords`
- `POST /api/v3/ari-pick/blocked-keywords`
- `DELETE /api/v3/ari-pick/blocked-keywords/{keywordId}`

## 3. API 상세
### 3.1 제안 목록 조회
- Method/Path: `GET /api/v3/ari-pick`
- 설명: 제안 목록 조회
- 응답:
  - `aripickItems[]`
  - 각 항목: `proposalId`, `name`, `reason`, `proposerId`, `proposalDate`, `status`, `likeCount`

### 3.2 제안 단일 조회
- Method/Path: `GET /api/v3/ari-pick/{proposalId}`
- 설명: 제안 단건 조회
- 오류:
  - `404`: 제안 없음

### 3.3 제안 통계 조회
- Method/Path: `GET /api/v3/ari-pick/stats`
- 응답: `totalProposals`, `approved`, `pending`, `rejected`

### 3.4 식약처 제품 검색
- Method/Path: `GET /api/v3/ari-pick/foods?keyword={keyword}`
- 설명: 키워드로 식약처 제품 검색
- 응답:
  - `items[]`
  - 각 항목: `typeNSeq`, `name`, `company`, `kcalInfo`
- 비고: 외부 HTML 응답을 내부에서 파싱 후 JSON으로 반환

### 3.5 제안 등록
- Method/Path: `POST /api/v3/ari-pick`
- 요청:
```json
{
  "typeNSeq": 14116,
  "reason": "학생 수요가 많습니다."
}
```
- 등록 검증 조건:
  1. 식약처 상세 조회 성공
  2. 상세 판정 문구가 `고열량ㆍ저영양 식품이 아닙니다.`일 것
  3. 관리자 금지 키워드가 제품명에 포함되지 않을 것
- 성공 시: 생성된 제안 응답
- 실패 시:
  - 메시지: `매점 물품 수칙과 맞지 않습니다.`
  - HTTP: `400`

### 3.6 제안 승인
- Method/Path: `PATCH /api/v3/ari-pick/{proposalId}/approve`
- 설명: 상태를 `승인됨`으로 변경

### 3.7 제안 거절
- Method/Path: `PATCH /api/v3/ari-pick/{proposalId}/reject`
- 설명: 상태를 `거절됨`으로 변경

### 3.8 제안 검토중 변경
- Method/Path: `PATCH /api/v3/ari-pick/{proposalId}/pending`
- 설명: 상태를 `검토중`으로 변경

### 3.9 제안 삭제(작성자)
- Method/Path: `DELETE /api/v3/ari-pick/{proposalId}`
- 설명: 작성자 본인만 삭제 가능
- 오류:
  - `403`: 본인 제안 아님
  - `404`: 제안 없음

### 3.10 제안 삭제(관리자)
- Method/Path: `DELETE /api/v3/ari-pick/{proposalId}/admin`
- 설명: 관리자 강제 삭제

### 3.11 좋아요 토글
- Method/Path: `POST /api/v3/ari-pick/{proposalId}/like`
- 설명: 좋아요 토글
- 응답:
```json
{
  "proposalId": 1,
  "liked": true,
  "likeCount": 3
}
```

### 3.12 금지 키워드 목록 조회(관리자)
- Method/Path: `GET /api/v3/ari-pick/blocked-keywords`
- 응답:
  - `keywords[]`
  - 각 항목: `keywordId`, `keyword`, `registeredDate`

### 3.13 금지 키워드 등록(관리자)
- Method/Path: `POST /api/v3/ari-pick/blocked-keywords`
- 요청:
```json
{
  "keyword": "에너지"
}
```
- 설명: 등록된 키워드가 제품명에 포함되면 제안 등록 차단

### 3.14 금지 키워드 삭제(관리자)
- Method/Path: `DELETE /api/v3/ari-pick/blocked-keywords/{keywordId}`

## 4. 에러 코드/응답 기준
- `400`: 정책 위반(식약처 판정 실패/금지 키워드 포함 등)
- `401`: 인증 실패
- `403`: 권한 없음
- `404`: 제안 없음

## 5. 외부 연동 설정
- 설정 키: `app.food-safety.base-url`
- 기본값:
  - `https://www.foodsafetykorea.go.kr`
- 용도:
  - 제품 검색: `/hilow/qfood/sfoodlist.do?searchValue=...`
  - 상세 조회: `/hilow/qfood/sfoodview.do?typenseq=...`
