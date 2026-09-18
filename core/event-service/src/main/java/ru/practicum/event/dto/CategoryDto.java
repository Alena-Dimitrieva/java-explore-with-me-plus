package ru.practicum.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CategoryDto(

		Long id,

		@NotBlank
		@Size(min = 1, max = 50)
		String name
) {
}
