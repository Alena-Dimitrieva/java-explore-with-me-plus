package ru.practicum.rating.util.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (org.hibernate.Hibernate.getClass(this)
                != org.hibernate.Hibernate.getClass(o)) {
            return false;
        }

        BaseEntity other = (BaseEntity) o;

        return id != null && id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        return org.hibernate.Hibernate.getClass(this).hashCode();
    }
}
