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
@Table(name = "event_similarity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventSimilarity {

    @EmbeddedId
    private EventPairId id;

    @Column(nullable = false)
    private double score;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
