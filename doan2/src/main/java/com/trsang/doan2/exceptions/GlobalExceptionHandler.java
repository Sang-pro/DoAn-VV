package com.trsang.doan2.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @Data
    public static class ErrorResponse {
        private String message;
        private String path;
        private int status;
        private Date timestamp;

        public ErrorResponse(String message, String path, int status, Date timestamp) {
            this.message = message;
            this.path = path;
            this.status = status;
            this.timestamp = timestamp;
        }
    }

    @ExceptionHandler(RefreshTokenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleRefreshTokenException(RefreshTokenException ex, HttpServletRequest request) {
        log.error("Không thể làm mới: {}", ex.getMessage());
        return new ErrorResponse(
                ex.getMessage(),
                request.getRequestURI(),
                HttpStatus.FORBIDDEN.value(),
                new Date()
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        log.error("Thông tin đăng nhập không hợp lệ: {}", ex.getMessage());
        return new ErrorResponse(
                ex.getMessage(),
                request.getRequestURI(),
                HttpStatus.UNAUTHORIZED.value(),
                new Date()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception ex, HttpServletRequest request, WebRequest webRequest) {
        log.error("Không thể xử lý: {}", ex);
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", LocalDateTime.now().toString());
        map.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        map.put("message", ex.getMessage());
        map.put("path", request != null ? request.getRequestURI() : "unknown");
        
        if(ex != null && ex.getMessage() != null) {
            map.put("details", ex.getMessage());
        } else {
            map.put("details", "No additional details available");
        }

        return new ResponseEntity<>(map, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException
                (MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("timestamp", new Date());
        response.put("path", request.getRequestURI());
        response.put("errors", errors);
        response.put("message", "Lỗi xác thực dữ liệu.");

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("User đã tồn tại trên hệ thống: {}", ex.getMessage());
        
        String message = "Đã có User trong hệ thống: ";
        if (ex.getRootCause() != null && ex.getMessage().contains("UKofx66keruapi6vyqpv6f2or37")) {
            message = "User đã tồn tại trong hệ thống";
        }

        return new ErrorResponse(
            request.getRequestURI(),
            message,
            HttpStatus.CONFLICT.value(),
            new Date());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFoundException(UsernameNotFoundException ex, HttpServletRequest request) {
        log.error("Không tìm thấy người dùng: {}", ex.getMessage());
        return new ErrorResponse(
            request.getRequestURI(),
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            new Date());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.error("Lỗi tham số không hợp lệ: {}", ex.getMessage());
        return new ErrorResponse(
            request.getRequestURI(),
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            new Date());
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Object> handleWebClientResponseException(WebClientResponseException ex, HttpServletRequest httpRequest, WebRequest request) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("timestamp", LocalDateTime.now().toString());
        bodyMap.put("status", ex.getStatusCode().value());
        bodyMap.put("message", "Lỗi trong quá trình gọi API");
        bodyMap.put("path", httpRequest != null ? httpRequest.getRequestURI() : "unknown");
        
        bodyMap.put("details", ex.getMessage() != null ? ex.getMessage() : "Lỗi trong quá trình gọi API");

        return new ResponseEntity<>(bodyMap, ex.getStatusCode());
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Object> handleNullPointerException(NullPointerException ex, HttpServletRequest httpRequest, WebRequest request) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("timestamp", LocalDateTime.now().toString());
        bodyMap.put("message", "Lỗi không xác định");
        
        log.error("NullPointerException: ", ex);
        return new ResponseEntity<>(bodyMap, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest httpRequest, WebRequest request) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("timestamp", LocalDateTime.now().toString());
        bodyMap.put("message", "Bạn không có quyền truy cập vào tài nguyên này");
        
        if (ex.getMessage() != null) {
            bodyMap.put("details", ex.getMessage());
        } 

        return new ResponseEntity<>(bodyMap, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Object> handleAsycnRequestTimeoutException(AsyncRequestTimeoutException ex, HttpServletRequest httpRequest, WebRequest request) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("timestamp", LocalDateTime.now().toString());
        bodyMap.put("message", "Yêu cầu đã hết thời gian chờ");
        
        log.warn("Yêu cầu đã hết thời gian chờ.");
        return new ResponseEntity<>(bodyMap, HttpStatus.REQUEST_TIMEOUT);
    }

    @ExceptionHandler(RunTimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRunTimeException(RunTimeException ex, HttpServletRequest request) {
        Map <String, Object> bodyMap = new HashMap<>();
        bodyMap.put("timestamp", LocalDateTime.now().toString());
        bodyMap.put("message", "Lỗi hệ thống");
        log.warn("Lỗi luồng chạy dữ liệu", ex.getMessage());
        return new ErrorResponse(
            request.getRequestURI(),
            ex.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            new Date());
    }
}
