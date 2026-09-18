package ru.practicum.event.dto.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewCompilationDto {

	private Set<Long> events;

	@Builder.Default
	private boolean pinned = false;

	@NotBlank
	@Size(min = 1, max = 50)
	private String title;
}