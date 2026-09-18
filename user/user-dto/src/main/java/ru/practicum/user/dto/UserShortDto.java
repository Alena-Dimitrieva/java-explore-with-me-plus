package ru.practicum.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@NotNull
@Builder
public record UserShortDto(

		Long id,

		@NotBlank
		String name,

		@Email
		@Size(min = 6, max = 255)
		@NotBlank
		String email
) {
}
