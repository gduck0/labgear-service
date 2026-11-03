# LabGear — 동시성 문제 해결 시나리오

LabGear의 동시 요청 정합성 문제를 **문제 상황 → 실패 가능성 → 대안과 선택 → 구현 → 테스트 → 한계** 순서로 정리한 기술 기록입니다. 예시 응답은 요구되는 정상 결과를 뜻하며, 임의로 만들어 낸 운영 로그나 성능 측정치가 아닙니다.

## 공통 환경과 판단 기준

- Java 21, Spring Boot 4.1.1, Spring Data JPA, MySQL 8.4
- 동시 요청 검증에는 별도 스레드, `CountDownLatch`, 독립된 요청/트랜잭션, 실제 MySQL 테스트 DB 활용
- 응답 코드뿐 아니라 **최종 저장 상태와 기존 데이터 보존 여부**를 검증
- 단일 인스턴스의 자바 `synchronized`보다 **DB가 공유하는 제약·잠금**을 활용
- 테스트에서 출발 신호를 동시에 줘도 스케줄링에 따라 실제 임계 구간이 반드시 겹치는 것은 아님

## 시나리오 1 — 동일 기자재의 같은 기간 동시 예약

### 문제

사용자 A와 B가 같은 기자재의 겹치는 기간을 동시에 예약합니다.

`중복 조회 → 결과 없음 → 예약 저장` 순서만으로 처리하면, 두 요청이 모두 조회 시점에 '예약 없음'을 보고 각각 저장할 수 있습니다.

### 검토와 선택

- **애플리케이션 수준 동기화:** 여러 서버 인스턴스에서 공유되지 않음
- **중복 검사만 수행:** 조회와 저장 사이 경쟁을 막을 수 없음
- **기자재 행 비관적 잠금(선택):** 같은 기자재를 기준으로 예약 요청의 조회·저장 구간을 직렬화할 수 있음

`ReservationService.createReservation()`에서는 `READ_COMMITTED` 트랜잭션을 시작하고 `EquipmentRepository.findByIdForUpdate()`로 기자재 행의 `PESSIMISTIC_WRITE` 잠금을 얻습니다. 그 후 예약 기간 겹침을 검사하고 저장합니다.

기간 비교는 양 끝을 포함합니다.

`기존 시작일 <= 요청 종료일 AND 기존 종료일 >= 요청 시작일`

`CANCELED`를 제외한 예약은 기간 중복 검사 대상입니다.

### 검증

- 동일 기자재·동일 기간에 대한 동시 API 요청: **한 건 `201 Created`, 다른 한 건 `409 Conflict`, 저장된 예약 한 건**
- 코드상 30회 반복 검증
- 기간이 겹치지 않으면 같은 기자재에도 예약 두 건을 생성할 수 있음
- 다른 기자재의 예약은 잠긴 기자재와 별개로 진행 가능

**관련 코드:** [ReservationService](../src/main/java/com/gduck0/labgear/service/ReservationService.java) · [ReservationConcurrentApiIntegrationTest](../src/test/java/com/gduck0/labgear/ReservationConcurrentApiIntegrationTest.java) · [ReservationConcurrencyScopeIntegrationTest](../src/test/java/com/gduck0/labgear/ReservationConcurrencyScopeIntegrationTest.java)

## 시나리오 2 — 기자재 삭제와 예약 생성의 경쟁

### 문제

관리자 A가 기자재를 삭제하는 순간 사용자 B가 그 기자재를 예약합니다. 삭제 전 예약 여부 확인과 실제 삭제 사이에 예약이 생성되면 참조 정합성이 깨지거나 예상하지 않은 오류가 발생할 수 있습니다.

### 검토와 선택

**삭제와 예약 생성이 같은 기자재 행을 먼저 잠그도록 통일**했습니다.

- 삭제: 기자재 잠금 → 연결된 예약 존재 여부 확인 → 없을 때만 삭제
- 예약: 기자재 잠금 → 기간 겹침 확인 → 저장

어느 요청이 먼저 잠금을 얻느냐에 따라 결과는 달라질 수 있지만, 둘 다 성공해서 존재하지 않는 기자재에 예약이 남는 상태는 허용하지 않습니다.

### 허용되는 결과

| 선행 작업 | 삭제 응답 | 예약 응답 | 최종 상태 |
| --- | --- | --- | --- |
| 삭제가 먼저 성공 | `200` | `404` | 기자재 없음, 예약 없음 |
| 예약이 먼저 성공 | `409` | `201` | 기자재 존재, 예약 한 건 |

### 검증

`EquipmentDeletionReservationRaceIntegrationTest`는 두 API 요청을 별도 스레드에서 시작하며, 20회 반복해 **응답 조합과 DB의 실제 기자재·예약 존재 여부**가 위 두 경우 중 하나에 해당하는지 검사합니다.

**관련 코드:** [EquipmentService](../src/main/java/com/gduck0/labgear/service/EquipmentService.java) · [EquipmentDeletionReservationRaceIntegrationTest](../src/test/java/com/gduck0/labgear/EquipmentDeletionReservationRaceIntegrationTest.java)

## 시나리오 3 — 동일 ID 기자재 동시 등록과 덮어쓰기 방지

### 문제

두 관리자가 같은 기자재 번호로 서로 다른 이름의 기자재를 동시에 등록합니다.

`existsById()` 사전 검사는 사용자에게 친절한 충돌 응답을 주는 데 도움이 되지만, 두 요청이 모두 '존재하지 않음'으로 판단하는 경쟁을 완전히 막을 수는 없습니다.

### 검토와 선택

- 기자재 ID는 DB에서 유일해야 한다는 **기본키 제약**을 최종 방어선으로 사용
- 신규 기자재 생성에 `EntityManager.persist()`를 사용해 기존 행의 의도치 않은 덮어쓰기를 방지
- 중복 키 오류(MySQL 오류 코드 `1062`)는 예외 처리에서 `409 Conflict`로 변환

따라서 중복 등록은 첫 번째 요청만 성공하고, 다른 요청으로 기존 기자재 이름이 변경되지 않아야 합니다.

### 검증

`EquipmentConcurrentRegistrationIntegrationTest`는 같은 ID·서로 다른 이름으로 등록을 시도하는 두 요청을 60회 반복합니다.

매회 다음 세 조건을 확인합니다.

1. 응답은 `201` 한 건, `409` 한 건
2. 해당 기자재가 한 건 존재
3. DB에 저장된 이름은 실제 `201`을 받은 요청의 이름과 일치

**관련 코드:** [EquipmentService](../src/main/java/com/gduck0/labgear/service/EquipmentService.java) · [GlobalExceptionHandler](../src/main/java/com/gduck0/labgear/exception/GlobalExceptionHandler.java) · [EquipmentConcurrentRegistrationIntegrationTest](../src/test/java/com/gduck0/labgear/EquipmentConcurrentRegistrationIntegrationTest.java)

## 시나리오 4 — 날짜가 다른 예약의 이중 대여

### 문제

기자재 하나에 날짜가 겹치지 않는 예약 A와 B가 있습니다. 예약을 만드는 것은 모두 허용되지만, 두 예약을 **동시에 대여 중(`RENTED`)**으로 만드는 것은 물리적으로 불가능합니다.

초기 구현은 *대여 대상 예약 행*만 잠갔기 때문에, 서로 다른 예약을 순차적으로 대여하는 것조차 막지 못했습니다. 동시성뿐 아니라 **대여 상태 전체에 걸친 불변식**의 문제였습니다.

### 해결

`ReservationService.rentReservation()`의 순서를 다음과 같이 변경했습니다.

1. 예약 ID로 기자재 ID만 조회
2. 기자재 행에 `PESSIMISTIC_WRITE` 잠금 획득
3. 대상 예약 행 잠금
4. 같은 기자재에 `RENTED` 예약이 이미 있는지 확인
5. 없다면 `reservation.rent()`로 `ACTIVE → RENTED` 전이

이 순서의 목적은 **서로 다른 예약도 동일한 기자재 잠금을 공유**하도록 하는 것입니다.

### 검증

- 예약 A 대여 성공(`204`) 후 예약 B 대여 거부(`409`)
- 거부 후 A는 `RENTED`, B는 `ACTIVE` 유지
- A 반납(`204`) 후 B 대여 성공
- 서로 다른 예약의 동시 대여 요청에서 `204`와 `409`가 각각 하나씩 발생하고, 최종 `RENTED` 예약은 한 건인지 확인

**관련 코드:** [ReservationService](../src/main/java/com/gduck0/labgear/service/ReservationService.java) · [ReservationRentalReturnIntegrationTest](../src/test/java/com/gduck0/labgear/ReservationRentalReturnIntegrationTest.java) · [PR #15](https://github.com/gduck0/labgear-service/pull/15)

## 시나리오 5 — 잠금 범위가 불필요하게 넓지 않은가?

동시 요청의 정합성을 지키더라도 모든 기자재 작업을 한꺼번에 막으면 처리량과 사용자 경험에 문제가 생길 수 있습니다.

현재 구현은 **기자재 행을 잠금 단위**로 사용합니다. `ReservationConcurrencyScopeIntegrationTest`는 한 기자재에 잠금을 걸고 유지하는 동안 다음을 검증합니다.

- **다른 기자재**에 대한 예약 생성은 진행됨
- 잠긴 기자재를 대상으로 한 일반 조회도 진행됨

이 검증은 잠금을 과도하게 전역화하지 않았다는 근거입니다. 다만 전체 시스템의 처리량이나 지연 시간을 측정한 성능 시험은 아닙니다.

**관련 테스트:** [ReservationConcurrencyScopeIntegrationTest](../src/test/java/com/gduck0/labgear/ReservationConcurrencyScopeIntegrationTest.java)

## 검증 범위와 남은 과제

- GitHub Actions에서 Spring Boot 통합 테스트가 MySQL 8.4 서비스 컨테이너를 사용해 실행됩니다. [CI 설정](../.github/workflows/ci.yml)
- 동시 출발 테스트에서 사용한 `CountDownLatch`는 두 작업의 **출발 시점**을 맞출 뿐, DB의 특정 임계 구간 중첩을 결정적으로 강제하지는 않습니다.
- 위 시나리오는 정합성에 초점을 맞춘 통합 테스트입니다. 운영 환경의 대규모 부하, 장애 복구, 복수 애플리케이션 인스턴스 성능을 검증한 것은 아닙니다.
- 대여 가능 날짜 정책, 인증 만료에 대한 화면 처리, DB 스키마 변경 관리는 별도 개선 과제입니다.

## 면접에서 설명할 핵심

이 프로젝트의 동시성 문제는 단순히 '락을 사용했다'가 핵심이 아닙니다.

**무엇을 동일한 자원으로 볼지 결정하고(기자재 ID), 어떤 불변식을 지켜야 하는지 정의한 뒤(중복 기간·중복 대여·참조 보존), 해당 자원을 공유하는 요청에서 잠금 순서를 일관되게 설계하고, 응답 코드와 최종 DB 상태를 함께 검증한 과정**이 핵심입니다.

