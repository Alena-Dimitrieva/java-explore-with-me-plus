package ru.practicum.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record NewUserRequest(

		@Size(min = 2, max = 250)
		@NotBlank
		String name,

		@Email
		@Size(min = 6, max = 254)
		@NotBlank
		String email
) {
}
