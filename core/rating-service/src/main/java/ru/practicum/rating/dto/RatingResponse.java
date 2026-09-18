package ru.practicum.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.rating.model.enums.Reaction;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {
	private Long id;
	private Long userId;
	private Long eventId;
	private Reaction reaction;
}
