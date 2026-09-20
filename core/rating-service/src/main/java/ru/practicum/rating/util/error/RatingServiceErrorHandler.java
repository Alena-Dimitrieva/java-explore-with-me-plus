package ru.practicum.rating.util.error;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.practicum.rating.util.error.exception.ConflictException;
import ru.practicum.rating.util.error.exception.NotFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class RatingServiceErrorHandler {

    @ExceptionHandler
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ApiError handleSomeError(final Exception ex) {
        String stackTrace = getStackTrace(ex);
        return new ApiError(
                INTERNAL_SERVER_ERROR,
                "Непредвиденная ошибка",
                ex.getMessage(),
                stackTrace,
                LocalDateTime.now()
        );
    }

    @ExceptionHandler
    @ResponseStatus(BAD_REQUEST)
    public ApiError handleMissingServletRequestParameter(@NonNull final MissingServletRequestParameterException ex) {
        log.warn("400 Bad Request (MissingServletRequestParameter): {}", ex.getMessage());
        String stackTrace = getStackTrace(ex);
        return new ApiError(
                BAD_REQUEST,
                "MissingServletRequestParameter",
                ex.getMessage(),
                stackTrace,
                LocalDateTime.now()
        );
    }

    @ExceptionHandler
    @ResponseStatus(BAD_REQUEST)
    public ApiError handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex) {
        log.warn("400 Bad Request (ArgumentNotValid): {}", ex.getMessage());
        return new ApiError(
                BAD_REQUEST,
                "Incorrectly made request.",
                "Field: " + Objects.requireNonNull(ex.getBindingResult().getFieldError()).getField()
                        + ". Error: " + ex.getBindingResult().getFieldError().getDefaultMessage(),
                getStackTrace(ex),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler
    @ResponseStatus(BAD_REQUEST)
    public ApiError handleHandlerMethodValidationException(@NonNull HandlerMethodValidationException ex) {
        log.warn("400 Bad Request (HandlerMethodValidationException): {}", ex.getMessage());
        return new ApiError(
                BAD_REQUEST,
                "HandlerMethodValidationException",
                ex.getMessage(),
                getStackTrace(ex),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler
    @ResponseStatus(BAD_REQUEST)
    public ApiError handleIllegalArgumentException(@NonNull IllegalArgumentException ex) {
        log.warn("400 Bad Request (IllegalArgument): {}", ex.getMessage());
        return new ApiError(
                BAD_REQUEST,
                "Incorrectly made request.",
                ex.getMessage(),
                getStackTrace(ex),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler
    @ResponseStatus(BAD_REQUEST)
    public ApiError handleValidationException(@NonNull ValidationException ex) {
        log.warn("400 Bad Request (ValidationException): {}", ex.getMessage());
        return new ApiError(
                BAD_REQUEST,
                "Incorrectly made request.",
                ex.getMessage(),
                getStackTrace(ex),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler
    @ResponseStatus(NOT_FOUND)
    public ApiError handleNotFound(@NonNull NotFoundException ex) {
        log.warn("404 Not Found: {}", ex.getMessage());
        return new ApiError(
                NOT_FOUND,
                "The required object was not found.",
                ex.getMessage(),
                getStackTrace(ex),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler
    @ResponseStatus(CONFLICT)
    public ApiError handleConflict(@NonNull ConflictException ex) {
        log.warn("409 Conflict: {}", ex.getMessage());
        return new ApiError(
                CONFLICT,
                "For the requested operation the conditions are not met.",
                ex.getMessage(),
                getStackTrace(ex),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler
    @ResponseStatus(CONFLICT)
    public ApiError handleHttpMessageNotReadableException(@NonNull final HttpMessageNotReadableException ex) {
        log.warn("409 CONFLICT: Required request body is missing or invalid");
        return new ApiError(
                BAD_REQUEST,
                "Incorrectly made request.",
                ex.getMessage(),
                getStackTrace(ex),
                LocalDateTime.now()
        );
    }

    private String getStackTrace(@NonNull Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
