package ru.practicum.event.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.event.model.enums.AdminStateAction;
import ru.practicum.event.model.enums.EventState;
import ru.practicum.event.model.enums.UserStateAction;

@UtilityClass
public class StateMapper {

    public EventState mapUserEventAction(
            UserStateAction action) {

        if (action == null) {
            return null;
        }

        return switch (action) {
            case SEND_TO_REVIEW -> EventState.PENDING;
            case CANCEL_REVIEW -> EventState.CANCELED;
            case PUBLISH_EVENT, REJECT_EVENT -> null;
        };
    }

    public EventState mapAdminEventAction(
            AdminStateAction action) {

        if (action == null) {
            return null;
        }

        return switch (action) {
            case PUBLISH_EVENT -> EventState.PUBLISHED;
            case REJECT_EVENT -> EventState.CANCELED;
        };
    }
}