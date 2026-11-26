package ru.practicum.shareit.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ShareItExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NotFoundException ex) {
        log.warn("404: {}", ex.getMessage());
        return new ErrorResponse("NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("400: {}", ex.getMessage());
        return new ErrorResponse("BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalStateException(IllegalStateException ex) {
        log.warn("400: {}", ex.getMessage());
        return new ErrorResponse("BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("400 validation: {}", message);
        return new ErrorResponse("BAD_REQUEST", message);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        ErrorResponse error = new ErrorResponse(
                ex.getStatusCode().toString(),
                ex.getReason() != null ? ex.getReason() : "Ошибка запроса");
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("400 missing header: {}", ex.getMessage());
        return new ErrorResponse("BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(WrongStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleWrongState(WrongStateException ex) {
        log.warn("400: {}", ex.getMessage());
        return new ErrorResponse("BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("400: {}", ex.getMessage());
        return new ErrorResponse("BAD_REQUEST",
                "Некорректное значение параметра: " + ex.getName());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception ex) {
        log.error("Непредвиденная ошибка при обработке запроса", ex);
        return new ErrorResponse("INTERNAL_SERVER_ERROR", "Непредвиденная ошибка");
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        ErrorResponse error = new ErrorResponse("CONFLICT", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbiddenException(ForbiddenException ex) {
        log.warn("403: {}", ex.getMessage());
        return new ErrorResponse("FORBIDDEN", ex.getMessage());
    }
}
