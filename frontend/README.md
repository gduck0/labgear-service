# LabGear Frontend

React + Vite로 구현한 LabGear 기자재 예약 화면입니다. Spring Boot API와 연동합니다.

## 제공 기능

- 기자재 목록·상세 조회, 화면 내 검색 및 정렬 (`GET /equipment`, `GET /equipment/{id}`)
- 관리자 전용 기자재 등록·이름 변경·삭제 (`POST /equipment`, `PATCH /equipment/{id}/name`, `DELETE /equipment/{id}`)
- 로그인한 사용자의 예약 생성·내 예약 목록·본인 예약 단건 조회
- 회원가입, 세션 기반 로그인·로그아웃 및 로그인 상태 확인
- 날짜 범위 검증, API 오류 안내 및 기자재 삭제 확인

현재 예약 취소 기능은 지원하지 않습니다.

## 로컬 실행

백엔드를 8080 포트에서 실행한 후 별도 터미널에서:

```bash
cd frontend
npm ci
npm run dev
```

접속 주소: http://localhost:5173

Vite 개발 서버는 `/api` 요청을 `http://127.0.0.1:8080`으로 전달하며 `/api` 접두어를 제거합니다. API 기본 주소는 `VITE_API_BASE_URL`로 변경할 수 있습니다.

프로덕션 빌드: `npm run build`

## API 연동

- `POST /accounts`: `{ "email": "...", "password": "..." }`로 회원가입
- `POST /login`: 이메일·비밀번호를 폼 데이터로 전송하여 세션 로그인
- `GET /accounts/me`: 로그인한 계정의 이메일과 역할(`USER`/`ADMIN`) 확인
- `GET /csrf`: 변경 요청 및 로그인에 필요한 CSRF 토큰 확인
- `POST /reservations`: `{ "equipmentId": 1, "startDate": "YYYY-MM-DD", "endDate": "YYYY-MM-DD" }`로 예약 생성. 성공 시 HTTP 201과 예약 정보를 담은 JSON 반환
- `GET /reservations/my`: 로그인한 사용자의 예약 목록
- `GET /reservations/{id}`: 본인 예약 단건 조회 (다른 사용자 예약은 접근 불가)

기자재 조회는 로그인 없이 가능하며, 예약은 로그인 후 이용합니다. 기자재 변경은 `ADMIN` 역할이 필요합니다. 요청에는 세션 쿠키와 CSRF 토큰을 사용합니다.

`POST /equipment`는 성공 시 HTTP 201과 빈 본문을 반환하지만, `POST /reservations`는 예약 정보를 반환합니다.
