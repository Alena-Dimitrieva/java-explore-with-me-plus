package ru.practicum.stat.server.util.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;

@Slf4j
@RestControllerAdvice
public class StatErrorHandler {

    // Обработка 400 Bad Request
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            IllegalArgumentException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class, // Отсутствие параметра
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class // Неверный формат
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(final Exception e) { // Здесь Exception
        log.error("400 Bad Request: {}", e.getMessage());
        return new ApiError(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                e.getMessage(),
                getStackTrace(e)
        );
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Exception e) {
        log.info("500 {}", e.getMessage(), e);
        String stackTrace = getStackTrace(e);
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Error ....", e.getMessage(), stackTrace);
    }
}
