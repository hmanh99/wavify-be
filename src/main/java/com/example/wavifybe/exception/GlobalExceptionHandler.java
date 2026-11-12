package com.example.wavifybe.exception;

import com.example.wavifybe.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.Map;

@RestControllerAdvice
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse> handleMaxUploadSizeExceededException(org.springframework.web.multipart.MaxUploadSizeExceededException exception) {
        ApiResponse apiResponse = ApiResponse.builder()
                .code(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .message("Kích thước tệp vượt quá giới hạn tối đa cho phép (Tối đa 50MB)")
                .build();
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(apiResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handlingRuntimeException(RuntimeException exception) {
        ApiResponse body = new ApiResponse(
                HttpStatus.BAD_REQUEST.value(),
                null,
                exception.getMessage()
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handlingAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationException(HttpServletResponse res, MethodArgumentNotValidException ex) throws IOException {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.builder()
                        .code(ex.getStatusCode().value())
                        .result(null)
                        .message(message)
                        .build());
    }


    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));

        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
