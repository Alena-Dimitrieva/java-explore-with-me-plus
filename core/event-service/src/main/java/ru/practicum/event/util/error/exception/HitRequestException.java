package ru.practicum.event.util.error.exception;

public class HitRequestException extends RuntimeException {
    public HitRequestException(Exception ex) {
        super("Отправка статистики не удалась " + ex.getMessage(), ex);
    }
}
