package ru.practicum.event.util.specification;

import jakarta.persistence.criteria.Join;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.event.model.Category;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

@UtilityClass
public class EventSpecifications {

    public Specification<Event> textContains(
            String text) {

        return (root, query, cb) -> {

            if (text == null || text.isBlank()) {
                return null;
            }

            String pattern =
                    "%" + text.toLowerCase() + "%";

            return cb.or(
                    cb.like(
                            cb.lower(
                                    root.get("annotation")
                            ),
                            pattern
                    ),
                    cb.like(
                            cb.lower(
                                    root.get("description")
                            ),
                            pattern
                    )
            );
        };
    }

    public Specification<Event> hasCategories(
            List<Long> categories) {

        return (root, query, cb) -> {

            if (categories == null
                    || categories.isEmpty()) {
                return null;
            }

            Join<Event, Category> categoryJoin =
                    root.join("category");

            return categoryJoin
                    .get("id")
                    .in(categories);
        };
    }

    public Specification<Event> isPaid(
            Boolean paid) {

        return (root, query, cb) ->
                paid == null
                        ? null
                        : cb.equal(
                        root.get("paid"),
                        paid
                );
    }

    public Specification<Event> dateAfter(
            LocalDateTime rangeStart) {

        return (root, query, cb) ->
                rangeStart == null
                        ? null
                        : cb.greaterThanOrEqualTo(
                        root.get("eventDate"),
                        rangeStart
                );
    }

    public Specification<Event> dateBefore(
            LocalDateTime rangeEnd) {

        return (root, query, cb) ->
                rangeEnd == null
                        ? null
                        : cb.lessThanOrEqualTo(
                        root.get("eventDate"),
                        rangeEnd
                );
    }

    public Specification<Event> isPublished() {

        return (root, query, cb) ->
                cb.equal(
                        root.get("state"),
                        EventState.PUBLISHED
                );
    }

    public Specification<Event> eventDateAfterNow(
            LocalDateTime now) {

        return (root, query, cb) ->
                cb.greaterThan(
                        root.get("eventDate"),
                        now
                );
    }

    public Specification<Event> hasUsers(
            List<Long> userIds) {

        return (root, query, cb) ->
                userIds == null
                        || userIds.isEmpty()
                        ? null
                        : root.get("initiatorId")
                        .in(userIds);
    }

    public Specification<Event> hasStates(
            List<EventState> states) {

        return (root, query, cb) ->
                states == null
                        || states.isEmpty()
                        ? null
                        : root.get("state")
                        .in(states);
    }
}