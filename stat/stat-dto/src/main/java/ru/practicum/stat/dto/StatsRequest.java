package ru.practicum.stat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatsRequest {

    @NotNull
    private String start;

    @NotNull
    private String end;

    private List<String> uris;

    @Builder.Default
    private Boolean unique = false;
}