package ru.practicum.ewm.stats.analyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_event_interactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEventInteraction {

    @EmbeddedId
    private UserEventId id;

    @Column(nullable = false)
    private int weight;

    @Column(name = "last_interaction_at", nullable = false)
    private Instant lastInteractionAt;
}
