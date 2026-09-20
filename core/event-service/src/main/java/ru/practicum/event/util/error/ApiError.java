package ru.practicum.event.util.error;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * @param status    Код статуса HTTP-ответа
 * @param reason    Общее описание причины ошибки
 * @param message   Сообщение об ошибке
 * @param errors    Список стектрейсов или описания ошибок
 * @param timestamp Дата и время когда произошла ошибка (в формате "yyyy-MM-dd HH:mm:ss")
 */
public record ApiError(
        HttpStatus status,
        String reason,
        String message,
        String errors,
        LocalDateTime timestamp
) {
}
