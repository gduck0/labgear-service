# LabGear Frontend

React + Vite로 만든 LabGear 기자재 관리·예약 클라이언트입니다. Spring Boot 백엔드와 함께 실행하는 실제 API 연동 화면입니다.

## 제공 기능
- 기자재 목록·상세 조회, 검색 및 정렬 (`GET /equipment`, `GET /equipment/{id}`)
- 기자재 등록·이름 변경·삭제 (`POST /equipment`, `PATCH /equipment/{id}/name`, `DELETE /equipment/{id}`)
- 예약 생성 및 번호로 조회 (`POST /reservations`, `GET /reservations/{id}`)
- 회원가입·세션 기반 로그인·로그아웃 및 로그인 상태 확인
- 서버의 400·401·404·409 오류 표시, 예약 날짜 범위 확인 및 삭제 확인 창

## 실행
백엔드를 8080 포트에서 실행한 다음, 별도 터미널에서:

```bash
cd frontend
npm install
npm run dev
```

브라우저: http://localhost:5173

Vite 프록시는 /api 요청을 http://127.0.0.1:8080 으로 전달하면서 /api 접두어를 제거합니다. API 주소를 바꾸려면 `VITE_API_BASE_URL` 환경변수를 설정하세요.

## API 연동
- 학생 등록/조회 API가 아직 없어 예약 생성에는 **DB에 미리 존재하는 학생 ID**가 필요합니다.
- 예약 전체 목록 API가 없어 현재는 **예약 번호 단건 조회**만 제공합니다.
- 기자재 목록·상세 조회는 로그인 없이 이용할 수 있으며, 기자재 변경과 예약 API는 로그인 후 이용합니다. 세션 쿠키와 CSRF 토큰을 함께 전달합니다.
- 예약 생성 요청 필드는 `{ id, studentId, equipmentId, startDate, endDate }`이며 날짜는 `YYYY-MM-DD` 형식입니다.
- `POST /equipment`와 `POST /reservations`는 성공 시 201과 빈 본문을 반환합니다.
