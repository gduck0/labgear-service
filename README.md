# LabGear

학과 실습·캡스톤 프로젝트에서 사용하는 기자재의 대여·예약을 관리하는 시스템입니다. Spring Boot와 MySQL 기반 백엔드에 React 화면을 연결해 기자재 관리와 기간 예약 기능을 구현하고 있습니다.

## 프로젝트 정보

- 기간: 2026.09 ~ 현재
- 형태: 개인 프로젝트 / 개발 중
- 주요 구성: Java · Spring Boot · MySQL · React

## 주요 기능

- 기자재 등록·목록 및 상세 조회·이름 수정·삭제
- 예약 생성·번호 조회 및 예약 기간 겹침 검사
- 예약이 연결된 기자재의 삭제 제한
- 회원가입·비밀번호 해시 저장·세션 기반 로그인·로그아웃
- 요청 입력 검증과 400·401·404·409 오류 응답 처리
- React 화면을 통한 기자재 관리·예약·인증 API 연동

## 기술 스택

- Backend: Java 21, Spring Boot, Spring Data JPA, Spring Security
- Database: MySQL
- Frontend: React, Vite
- Test: JUnit 5, MockMvc, Spring Security Test
