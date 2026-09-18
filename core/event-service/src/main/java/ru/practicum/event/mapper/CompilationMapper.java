package ru.practicum.event.mapper;

import lombok.experimental.UtilityClass;
import org.springframework.lang.NonNull;
import ru.practicum.event.dto.compilation.CompilationDto;
import ru.practicum.event.dto.compilation.NewCompilationDto;
import ru.practicum.event.model.Compilation;
import ru.practicum.event.model.Event;
import ru.practicum.user.dto.UserShortDto;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class CompilationMapper {

	public CompilationDto toCompilationDto(
			@NonNull Compilation compilation,
			@NonNull Map<Long, UserShortDto> initiators,
			@NonNull Map<Long, Long> confirmedRequests,
			@NonNull Map<Long, Long> views) {

		return CompilationDto.builder()
				.id(compilation.getId())
				.pinned(compilation.isPinned())
				.title(compilation.getTitle())
				.events(
						compilation.getEvents()
								.stream()
								.map(event ->
										EventMapper.toEventShortDto(
												event,
												initiators.get(
														event.getInitiatorId()
												),
												confirmedRequests.getOrDefault(
														event.getId(),
														0L
												),
												views.getOrDefault(
														event.getId(),
														0L
												)
										)
								)
								.toList()
				)
				.build();
	}

	public Compilation toEntity(
			@NonNull NewCompilationDto dto,
			Set<Event> events) {

		return Compilation.builder()
				.title(dto.getTitle())
				.pinned(dto.isPinned())
				.events(
						events == null
								? new HashSet<>()
								: events
				)
				.build();
	}
}