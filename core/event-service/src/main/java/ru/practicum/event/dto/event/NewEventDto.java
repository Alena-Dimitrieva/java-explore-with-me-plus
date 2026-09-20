package ru.practicum.event.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import ru.practicum.event.model.Location;

import java.time.LocalDateTime;

@Data
@Builder()
@Accessors(fluent = true)
@FieldDefaults(makeFinal = true)
public class NewEventDto {

    @NotBlank
    @Size(min = 20, max = 2000)
    String annotation;

    @NotNull
    Long category;

    @NotBlank
    @Size(min = 20, max = 7000)
    String description;

    @NotNull
    LocalDateTime eventDate;

    @NotNull
    Location location;

    @Builder.Default
    Boolean paid = false;

    @Builder.Default
    Integer participantLimit = 0;

    @Builder.Default
    Boolean requestModeration = true;

    @NotBlank
    @Size(min = 3, max = 120)
    String title;
}
