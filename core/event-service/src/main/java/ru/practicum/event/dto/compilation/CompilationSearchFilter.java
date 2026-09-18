package ru.practicum.event.dto.compilation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationSearchFilter {

	Boolean pinned;

	@Builder.Default
	Integer from = 0;

	@Builder.Default
	Integer size = 10;
}
