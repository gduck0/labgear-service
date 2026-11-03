# LabGear

**학과 기자재 예약·대여 관리 서비스**

[![LabGear CI](https://github.com/gduck0/labgear-service/actions/workflows/ci.yml/badge.svg)](https://github.com/gduck0/labgear-service/actions/workflows/ci.yml)

학과 실습과 캡스톤디자인에서 사용하는 기자재를 조회하고, 원하는 기간에 예약하며, 관리자가 대여·반납을 처리할 수 있는 웹 서비스입니다.

**핵심 관심사:** 기능 구현에 그치지 않고, 동일 기자재에 대한 *중복 예약과 이중 대여*를 트랜잭션·비관적 잠금·통합 테스트로 다루는 데 집중했습니다.

> 개인 프로젝트 · 2026.09 ~ 진행 중 · 핵심 기능 및 CI 구축 완료 · 공개 배포 준비 중

## 주요 기능

| 구분 | 기능 |
| --- | --- |
| 사용자 | 기자재 검색·정렬, 예약 현황 달력 조회, 기간 예약, 내 예약 조회·취소 |
| 관리자 | 기자재 등록·이름 수정·삭제, 전체 예약 조회, 대여·반납 처리 |
| 인증·인가 | 회원가입, BCrypt 비밀번호 해시, 세션 로그인·로그아웃, 역할별 접근 제어 |
| 정합성 | 예약 기간 중복 방지, 동일 기자재 이중 대여 방지, 상태 전이 검증, 예약 이력 보존 |

회원가입 계정은 기본적으로 `USER` 역할을 받으며, 관리자 API는 `ADMIN` 역할만 사용할 수 있습니다. 본인 예약 조회·취소는 요청 본문의 사용자 ID가 아니라 인증된 계정 정보를 기준으로 처리합니다.

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1.1, Spring MVC, Spring Data JPA, Spring Security |
| Database | MySQL 8.4 |
| Frontend | React 19, Vite 6 |
| Test | JUnit 5, Spring Boot Test, MockMvc, Spring Security Test |
| Build / CI | Gradle 9.7.1, npm, GitHub Actions |

### 구조

`React (Vite) → Spring MVC Controller → Service → Spring Data JPA Repository → MySQL`

- `Account`: 이메일, 비밀번호 해시, 역할(`USER`/`ADMIN`)
- `Equipment`: 기자재 번호와 이름
- `Reservation`: 예약자, 기자재, 시작일·종료일, 예약 상태

비즈니스 규칙은 주로 Service와 엔티티의 상태 변경 메서드에서 처리합니다. 개발 환경에서는 Vite의 `/api` 프록시를 통해 Spring Boot(8080)에 요청을 전달합니다.

## 주요 기술 문제와 해결

> [동시성 문제 해결 시나리오 상세 기록](docs/concurrency-scenarios.md) — 예약 중복·삭제 충돌·동일 ID 등록·이중 대여·잠금 범위 검증

### 1. 동시 예약 요청에서 기간 중복 방지

**문제:** 같은 기자재에 대해 두 사용자가 겹치는 날짜를 거의 동시에 예약하면, 각각의 요청이 중복 여부를 조회할 때 모두 '예약 없음'으로 판단할 수 있습니다. 단순한 조회 후 저장만으로는 이를 막을 수 없습니다.

**해결:** `createReservation()`에 `READ_COMMITTED` 트랜잭션을 적용하고, **기자재 행을 `PESSIMISTIC_WRITE`로 잠근 다음** 겹침 여부를 조회하고 저장합니다. 같은 기자재에 대한 예약 생성은 이 잠금을 기준으로 순서대로 처리됩니다.

예약 기간은 양 끝 날짜를 포함합니다.

`기존 시작일 <= 요청 종료일 AND 기존 종료일 >= 요청 시작일`

- `CANCELED` 예약의 기간은 재예약 가능
- `ACTIVE`, `RENTED`, `RETURNED` 예약의 기간은 기존 예약 기록에 따라 차단
- 서로 다른 기자재는 각자의 기자재 행을 기준으로 처리

**코드:** [ReservationService](src/main/java/com/gduck0/labgear/service/ReservationService.java), [ReservationRepository](src/main/java/com/gduck0/labgear/repository/ReservationRepository.java)

### 2. 서로 다른 예약의 이중 대여 방지

**문제:** 예약 기간이 겹치지 않아도 하나의 *실제 기자재*를 동시에 두 건의 대여 상태로 만들 수는 없습니다. 초기 구현은 요청한 예약 행만 잠갔기 때문에 서로 다른 예약을 차례로 `RENTED`로 변경할 수 있었습니다.

**해결:** 대여 처리에서 기자재 단위로 직렬화하고, 기존 대여 상태를 검사합니다.

1. 예약 ID로 기자재 ID 조회
2. 해당 **기자재 행 잠금**
3. 대여 대상 예약 행 잠금
4. 같은 기자재의 `RENTED` 예약 존재 여부 확인
5. 존재하면 `409 Conflict`, 없으면 `ACTIVE → RENTED` 전이

이 순서를 통해 서로 다른 예약에 대한 요청도 같은 기자재의 잠금을 공유합니다. 첫 대여를 반납하면 다음 예약을 대여 처리할 수 있습니다.

**검증:** 두 예약의 순차 대여 거부, 반납 후 대여 성공, 동시 대여 요청의 성공/충돌 응답 및 최종 DB 상태를 테스트했습니다. 동시 출발 테스트는 실제 임계 구간 경쟁을 매번 강제하지는 않는다는 한계가 있습니다.

[이중 대여 방지 PR #15](https://github.com/gduck0/labgear-service/pull/15) · [ReservationRentalReturnIntegrationTest](src/test/java/com/gduck0/labgear/ReservationRentalReturnIntegrationTest.java)

### 3. 예약 상태 전이와 기록 보존

예약을 바로 삭제하지 않고 상태로 관리합니다.

`ACTIVE → CANCELED`  
`ACTIVE → RENTED → RETURNED`

| 상태 | 의미 | 예약 기간 처리 |
| --- | --- | --- |
| `ACTIVE` | 예약 완료 | 다른 예약과 겹칠 수 없음 |
| `CANCELED` | 예약 취소 | 해당 기간 재예약 가능 |
| `RENTED` | 기자재 대여 중 | 기존 예약 기간 유지 |
| `RETURNED` | 반납 완료 | 기존 예약 기간 유지 |

허용하지 않은 상태 변경은 `409 Conflict`를 반환합니다. 예약 내역이 연결된 기자재의 삭제도 제한해 참조 이력을 보존합니다.

### 4. 인증·인가

- Spring Security 세션 인증 및 CSRF 보호
- BCrypt를 이용한 비밀번호 해시 저장
- 기자재 등록·수정·삭제 및 대여·반납은 `ADMIN` 전용
- 본인 예약 조회·취소 시 로그인 계정과 예약자 비교
- 잘못된 요청·없는 리소스·상태 충돌에 대한 HTTP 오류 처리

## 테스트와 CI

JUnit 5와 MockMvc 통합 테스트로 인증·인가, 기자재 CRUD, 예약 기간 검증, 동시 예약 생성, 기자재 삭제와 예약 생성 간 경쟁, 예약 취소·대여·반납, 이중 대여 방지 등을 검증합니다.

GitHub Actions는 `main` 대상 Pull Request와 `main` Push에서 아래 작업을 수행합니다.

| 작업 | 자동 검증 |
| --- | --- |
| **Backend Tests** | Java 21 + MySQL 8.4 서비스 컨테이너에서 `./gradlew test` |
| **Frontend Build** | Node.js 22에서 `npm ci` 및 `npm run build` |

[CI 설정](.github/workflows/ci.yml) · [최초 성공한 main CI 실행](https://github.com/gduck0/labgear-service/actions/runs/41607336845)

CI 통과는 자동화된 검증 결과이며, 실제 사용자 환경에서의 운영 안정성이나 부하 성능까지 증명하는 것은 아닙니다.

## 로컬 실행

### 요구 환경

- Java 21, MySQL 8.4, Node.js 22
- Gradle은 저장소의 Wrapper 사용

### 1. MySQL 데이터베이스 생성

로컬 MySQL에서 개발 DB를 생성합니다.

```sql
CREATE DATABASE IF NOT EXISTS labgear
  CHARACTER SET utf8mb4;
```

현재 개발 설정(`src/main/resources/application.properties`)은 `localhost:3306/labgear`에 `root` 계정으로 연결하며, 비밀번호는 `DB_PASSWORD` 환경변수에서 읽습니다. **root 계정은 현재 로컬 개발 설정이며 운영 배포용 권장 구성은 아닙니다.**

### 2. 백엔드 실행

프로젝트 루트에서:

```bash
export DB_PASSWORD='로컬 MySQL 비밀번호'
./gradlew bootRun
```

기본 API 주소: `http://localhost:8080`

### 3. 프론트엔드 실행

다른 터미널에서:

```bash
cd frontend
npm ci
npm run dev
```

개발 화면: `http://localhost:5173`

Vite 개발 서버는 `/api` 접두어를 제거해 Spring Boot 8080 포트로 프록시합니다. 현재 구성은 **로컬 개발용**이며, 공개 배포 시에는 프론트엔드·백엔드의 출처와 쿠키/세션 설정을 별도로 검토해야 합니다.

### 4. 백엔드 테스트

통합 테스트는 개발 DB와 별개인 `labgear_test` 데이터베이스와 `labgear_test` 계정을 사용합니다. 먼저 해당 DB와 계정을 만들고 필요한 권한을 부여해야 합니다.

```sql
CREATE DATABASE IF NOT EXISTS labgear_test
  CHARACTER SET utf8mb4;

CREATE USER IF NOT EXISTS 'labgear_test'@'localhost'
  IDENTIFIED BY '테스트용 비밀번호';

GRANT ALL PRIVILEGES ON labgear_test.*
  TO 'labgear_test'@'localhost';
```

```bash
export LABGEAR_TEST_DB_PASSWORD='테스트용 비밀번호'
./gradlew test
```

테스트 DB 연결 설정: [`src/test/resources/application.properties`](src/test/resources/application.properties). CI에서는 테스트용 MySQL 컨테이너와 계정을 자동 구성합니다.

> 일반 회원가입으로 생성한 계정은 `USER`입니다. 관리자 셀프 가입 기능은 없으므로 로컬 관리자 기능 확인 시 테스트용 계정에 `ADMIN` 역할을 별도로 부여해야 합니다.

## 현재 한계와 다음 단계

이 프로젝트는 **실제 운영 사용자와 운영 실적이 없는 개인 프로젝트**입니다. 현재 코드에서 확인되는 후속 과제는 다음과 같습니다.

- 공개 데모 배포 및 운영 환경용 DB 계정·설정 분리
- 배포를 위한 스키마 변경 관리(현재 개발 환경의 `ddl-auto=update` 개선)
- 세션 만료 시 프론트엔드의 인증 상태 복구 처리
- 예약 날짜와 실제 대여 가능 시점의 정책 정리
- 실제 DB 경쟁 구간을 더 결정적으로 검증하는 테스트 보강

현재 우선순위는 새 기능을 무작정 추가하기보다 **기존 예약·대여 흐름의 정확성, 테스트 재현성, 배포 가능성**을 높이는 것입니다.

---

**Repository:** https://github.com/gduck0/labgear-service

