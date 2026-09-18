package ru.practicum.rating.service;

import ru.practicum.rating.dto.RatingRequest;
import ru.practicum.rating.dto.RatingResponse;

public interface RatingService {

	RatingResponse addOrUpdateReaction(
			Long userId,
			Long eventId,
			RatingRequest request
	);

	void removeReaction(
			Long userId,
			Long eventId
	);
}