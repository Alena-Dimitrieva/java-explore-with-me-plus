package ru.practicum.event.controller.free;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.event.dto.event.EventFullDto;
import ru.practicum.event.dto.event.EventShortDto;
import ru.practicum.event.dto.event.FreeGetDto;
import ru.practicum.event.service.event.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class FreeEventController {

	private final EventService eventService;


	/**
	 * Получение событий с возможностью фильтрации
	 * <p>
	 * Обратите внимание:
	 * - это публичный эндпоинт, соответственно в выдаче должны быть только опубликованные события
	 * - текстовый поиск (по аннотации и подробному описанию) должен быть без учета регистра букв
	 * - если в запросе не указан диапазон дат [rangeStart-rangeEnd], то нужно выгружать события,
	 * которые произойдут позже текущей даты и времени
	 * - информация о каждом событии должна включать в себя количество просмотров и количество
	 * уже одобренных заявок на участие
	 * - информацию о том, что по этому эндпоинту был осуществлен и обработан запрос, нужно
	 * сохранить в сервисе статистики
	 * В случае, если по заданным фильтрам не найдено ни одного события, возвращает пустой список
	 *
	 * @param text          текст для поиска в содержимом аннотации и подробном описании события
	 * @param categories    список идентификаторов категорий в которых будет вестись поиск
	 * @param paid          поиск только платных/бесплатных событий
	 * @param rangeStart    дата и время не раньше которых должно произойти событие
	 * @param rangeEnd      дата и время не позже которых должно произойти событие
	 * @param onlyAvailable только события у которых не исчерпан лимит запросов на участие Default value : false
	 * @param sort          Вариант сортировки: по дате события или по количеству просмотров Available values : EVENT_DATE, VIEWS
	 * @param from          количество событий, которые нужно пропустить для формирования текущего набора Default value : 0
	 * @param size          количество событий в наборе Default value : 10
	 * @param request       Данные HTTP-запроса
	 * @return List<{@link EventShortDto}>
	 */
	@GetMapping
	public List<EventShortDto> getFreeEvents(
			@RequestParam(required = false) String text,
			@RequestParam(required = false) List<Long> categories,
			@RequestParam(required = false) Boolean paid,
			@RequestParam(required = false)
			@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
			LocalDateTime rangeStart,
			@RequestParam(required = false)
			@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
			LocalDateTime rangeEnd,
			@RequestParam(defaultValue = "false") Boolean onlyAvailable,
			@RequestParam(required = false)
			FreeGetDto.FreeEventSort sort,
			@RequestParam(defaultValue = "0") Integer from,
			@RequestParam(defaultValue = "10") Integer size,
			HttpServletRequest request) {

		FreeGetDto dto = FreeGetDto.builder()
				.text(text)
				.categories(categories)
				.paid(paid)
				.rangeStart(rangeStart)
				.rangeEnd(rangeEnd)
				.onlyAvailable(onlyAvailable)
				.sort(sort)
				.from(from)
				.size(size)
				.build();

		return eventService.getFreeEvents(dto, request);
	}

	/**
	 * Получение подробной информации об опубликованном событии по его идентификатору.
	 *
	 * @param eventId id события
	 * @param request Данные HTTP-запроса
	 * @return {@link EventFullDto}
	 */
	@GetMapping("/{eventId}")
	public EventFullDto getFreeEventById(
			@PathVariable Long eventId,
			HttpServletRequest request) {

		return eventService.getFreeEventById(
				eventId,
				request
		);
	}
}
