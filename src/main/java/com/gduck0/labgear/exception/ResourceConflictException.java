package com.gduck0.labgear.exception;

// 예약 기간 중복이나 삭제 제한 등 자원 상태 충돌을 HTTP 409로 전달한다.
public class ResourceConflictException extends RuntimeException {

        public ResourceConflictException(String message) {
        super(message);
    }
}
