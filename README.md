# LabGear

학과 실습과 캡스톤 프로젝트를 위한 기자재 관리 웹 서비스입니다.

## 기술 구성

- Backend: Java 21, Spring Boot, Spring Data JPA, MySQL
- Frontend: React, Vite
- Build: Gradle, npm

## 실행 방법

Java 21, Node.js와 MySQL이 필요합니다.

### 서버

MySQL에 `labgear` 데이터베이스를 준비하고 `DB_PASSWORD` 환경 변수를 설정한 뒤 저장소 루트에서 실행합니다.

```bash
./gradlew bootRun
```

API 기본 주소: `http://localhost:8080`

### 웹 클라이언트

```bash
cd frontend
npm install
npm run dev
```

웹 화면: `http://localhost:5173`

로컬 개발 서버의 `/api` 요청은 `http://127.0.0.1:8080`의 Spring Boot 서버로 전달됩니다.

## 주요 기능

- 기자재 목록 조회 및 검색
- 기자재 상세 조회
- 기자재 정렬 및 목록 새로고침

현재 웹 화면은 등록된 기자재의 조회 기능을 제공합니다. [프론트엔드 안내](frontend/README.md)에 실행·배포 연결 방식이 정리되어 있습니다.

## 빌드

```bash
./gradlew build
cd frontend
npm install
npm run build
```

정적 웹 파일을 별도 배포할 때는 `/api` 경로가 Spring 서버로 전달되도록 웹 서버를 구성해야 합니다.
