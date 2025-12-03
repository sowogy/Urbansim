package com.example.UrbanismWebSite.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {
    private final LocalDateTime timestamp;
    private final int status;       // HTTP 상태 코드
    private final String error;     // HttpStatus 이름 혹은 커스텀 코드
    private final String message;   // 에러 메시지
    private final String path;      // 요청 URI
}
