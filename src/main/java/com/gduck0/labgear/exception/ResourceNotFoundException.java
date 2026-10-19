package com.gduck0.labgear.exception;

// 요청한 자원이 없을 때 발생하며 공통 예외 처리에서 HTTP 404로 응답한다.
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}