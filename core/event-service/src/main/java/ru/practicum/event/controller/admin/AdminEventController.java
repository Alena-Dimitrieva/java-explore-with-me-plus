package ru.practicum.event.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.event.AdminGetDto;
import ru.practicum.event.dto.event.EventFullDto;
import ru.practicum.event.dto.event.UpdateEventAdminRequest;
import ru.practicum.event.model.enums.EventState;
import ru.practicum.event.service.event.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    /**
     * Поиск событий
     * <p>
     * Эндпоинт возвращает полную информацию обо всех событиях подходящих под переданные условия
     * В случае, если по заданным фильтрам не найдено ни одного события, возвращает пустой список
     *
     * @param users      список id пользователей, чьи события нужно найти
     * @param states     список состояний в которых находятся искомые события
     * @param categories список id категорий в которых будет вестись поиск
     * @param rangeStart дата и время не раньше которых должно произойти событие
     * @param rangeEnd   дата и время не позже которых должно произойти событие
     * @param from       количество событий, которые нужно пропустить для формирования текущего набора
     *                   Default value : 0
     * @param size       количество событий в наборе
     *                   Default value : 10
     * @return List<{ @ link EventFullDto }>
     */
    @GetMapping
    public List<EventFullDto> adminGetEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<EventState> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {

        AdminGetDto dto = AdminGetDto.builder()
                .users(users)
                .states(states)
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .from(from)
                .size(size)
                .build();

        return eventService.adminGetEvents(dto);
    }

    /**
     * Редактирование данных события и его статуса (отклонение/публикация)
     * <p>
     * Редактирование данных любого события администратором. Валидация данных не требуется. Обратите внимание:
     * - дата начала изменяемого события должна быть не ранее чем за час от даты публикации. (Ожидается код ошибки 409)
     * - событие можно публиковать, только если оно в состоянии ожидания публикации (Ожидается код ошибки 409)
     * - событие можно отклонить, только если оно еще не опубликовано (Ожидается код ошибки 409)
     *
     * @param eventId id события
     * @param request Данные HTTP-запроса
     * @return {@link EventFullDto}
     */
    @PatchMapping("/{eventId}")
    public EventFullDto adminUpdateEvent(
            @PathVariable Long eventId,
            @RequestBody @Valid UpdateEventAdminRequest request) {

        return eventService.adminUpdateEvent(
                eventId,
                request
        );
    }
}
