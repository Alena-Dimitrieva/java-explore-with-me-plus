package ru.practicum.stat.server.mapper;

import org.springframework.lang.NonNull;
import ru.practicum.stat.dto.EndpointHitDto;
import ru.practicum.stat.server.model.EndpointHit;

public class EndpointHitMapper {

    public static EndpointHit toEntity(@NonNull EndpointHitDto dto) {
        return EndpointHit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
    }
}
