package ru.practicum.request.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import ru.practicum.request.dto.ParticipationStatus;
import ru.practicum.request.util.entity.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "requests")
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParticipationRequest extends BaseEntity {

	@Column(nullable = false)
	LocalDateTime created;

	@Column(name = "event_id", nullable = false)
	Long eventId;

	@Column(name = "requester_id", nullable = false)
	Long requesterId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	ParticipationStatus status;
}
