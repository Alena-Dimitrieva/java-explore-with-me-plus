package ru.practicum.event.util.error.exception;

public class StatResponseException extends RuntimeException {
    private static final String MESSAGE = "Не удалось получить статистику";

    public StatResponseException(Exception ex) {
        super(MESSAGE, ex);
    }

    public StatResponseException() {
        super(MESSAGE);
    }
}
