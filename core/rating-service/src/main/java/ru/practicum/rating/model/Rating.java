package ru.practicum.rating.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import ru.practicum.rating.model.enums.Reaction;
import ru.practicum.rating.util.entity.BaseEntity;

@Entity
@Getter
@Setter
@Table(
        name = "ratings",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {
                        "user_id",
                        "event_id"
                }
        )
)
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Rating extends BaseEntity {

    @Column(
            name = "user_id",
            nullable = false
    )
    Long userId;

    @Column(
            name = "event_id",
            nullable = false
    )
    Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 10
    )
    Reaction reaction;
}