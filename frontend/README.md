# LabGear 웹 클라이언트

학과 실습·캡스톤 프로젝트를 위한 기자재 관리 서비스의 React 프론트엔드입니다.

## 구현 기능

- 기자재 목록 조회 및 이름·번호 검색
- 기자재 상세 조회
- 목록 정렬과 새로고침
- 데이터 로딩, 빈 목록 및 오류 상태 표시
- 반응형 레이아웃

조회 데이터는 실제 Spring Boot API의 응답을 사용하며, 별도 샘플 데이터로 대체하지 않습니다.

## 개발 환경 실행

Spring Boot API와 MySQL을 먼저 실행한 뒤, 저장소 루트에서:

```bash
cd frontend
npm install
npm run dev
```

Vite 개발 서버는 포트 `5173`에서 실행됩니다. `/api` 경로는 기본 설정으로 `http://127.0.0.1:8080`의 Spring Boot 서버에 전달됩니다.

## 주요 API

| 메서드 | 경로 | 응답 |
| --- | --- | --- |
| GET | `/equipment` | `[{ "id": 1, "name": "CAMERA" }]` 형태의 목록 |
| GET | `/equipment/{id}` | `{ "id": 1, "name": "CAMERA" }` 형태의 상세 |

백엔드 오류 응답은 서버에서 정의한 HTTP 상태 코드에 따라 표시합니다. 존재하지 않는 ID의 상태 코드와 세부 오류 구조는 서버 구현에 따릅니다.

## 배포 빌드

```bash
cd frontend
npm run build
npm run preview
```

빌드 결과는 `frontend/dist/`에 생성됩니다.

배포 시 API 서버와 같은 출처에서 `/api`를 Spring Boot로 전달하는 리버스 프록시 구성을 권장합니다. 분리 배포한다면 `VITE_API_BASE_URL`을 실제 API 주소로 설정하고 서버의 CORS 정책을 구성해야 합니다. 개발용 Vite 프록시는 배포된 정적 파일에 자동으로 적용되지 않습니다.

백엔드 접속 정보나 비밀번호를 프론트 환경 변수에 입력하지 마세요. `VITE_*` 환경 변수 값은 브라우저 번들에 포함됩니다.
