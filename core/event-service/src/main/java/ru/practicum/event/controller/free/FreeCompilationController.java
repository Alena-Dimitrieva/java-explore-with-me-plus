package ru.practicum.event.controller.free;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.compilation.CompilationDto;
import ru.practicum.event.dto.compilation.CompilationSearchFilter;
import ru.practicum.event.service.compilation.CompilationService;


import java.util.List;

@RestController
@RequestMapping(path = "/compilations")
@RequiredArgsConstructor
public class FreeCompilationController {

	private final CompilationService compilationService;

	/**
	 * Получение подборок событий
	 * <p>
	 * В случае, если по заданным фильтрам не найдено ни одной подборки, возвращает пустой список
	 *
	 * @param pinned  искать только закрепленные/не закрепленные подборки
	 * @param from    количество элементов, которые нужно пропустить для формирования текущего набора
	 *                Default value : 0
	 * @param size    количество элементов в наборе
	 *                Default value : 10
	 * @param request Данные HTTP-запроса
	 * @return List<{@link CompilationDto}>
	 */
	@GetMapping
	public List<CompilationDto> getCompilations(
			@RequestParam(required = false) Boolean pinned,
			@RequestParam(defaultValue = "0") Integer from,
			@RequestParam(defaultValue = "10") Integer size,
			HttpServletRequest request) {

		CompilationSearchFilter filter = CompilationSearchFilter.builder()
				.pinned(pinned)
				.from(from)
				.size(size)
				.build();

		return compilationService.getByFilter(filter, request);
	}

	/**
	 * Получение подборки событий по его id
	 *
	 * @param compId  id подборки
	 * @param request Данные HTTP-запроса
	 * @return {@link CompilationDto}
	 */
	@GetMapping("/{compId}")
	public CompilationDto getCompilationById(@PathVariable Long compId,
	                                         HttpServletRequest request) {
		return compilationService.getById(compId, request);
	}
}
