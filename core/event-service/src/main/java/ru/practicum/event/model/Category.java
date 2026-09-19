package ru.practicum.event.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ru.practicum.event.util.entity.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "categories")
@SuperBuilder
@NoArgsConstructor
public class Category extends BaseEntity {

    @Column(nullable = false, length = 50, unique = true)
    private String name;
}
