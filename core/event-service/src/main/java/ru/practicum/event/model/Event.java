package ru.practicum.event.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import ru.practicum.event.model.enums.EventState;
import ru.practicum.event.util.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "events")
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event extends BaseEntity {

    @Column(nullable = false, length = 2000)
    String annotation;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @Column(nullable = false)
    LocalDateTime createdOn;

    @Column(nullable = false, length = 7000)
    String description;

    @Column(nullable = false)
    LocalDateTime eventDate;

    @Column(name = "initiator_id", nullable = false)
    Long initiatorId;

    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    Location location;

    @Column(nullable = false)
    boolean paid;

    @Column(nullable = false)
    int participantLimit;

    LocalDateTime publishedOn;

    @Column(nullable = false)
    boolean requestModeration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    EventState state;

    @Column(nullable = false, length = 120)
    String title;

    @ManyToMany(mappedBy = "events")
    @lombok.Builder.Default
    Set<Compilation> compilations = new HashSet<>();

    @Column(nullable = false)
    long rate;
}