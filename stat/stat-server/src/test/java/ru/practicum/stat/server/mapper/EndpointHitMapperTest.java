package ru.practicum.stat.server.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.stat.dto.EndpointHitDto;
import ru.practicum.stat.server.model.EndpointHit;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EndpointHitMapperTest {

    @Test
    void toEntity_shouldMapAllFieldsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        EndpointHitDto dto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(now)
                .build();
        EndpointHit entity = EndpointHitMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(dto.getApp(), entity.getApp());
        assertEquals(dto.getUri(), entity.getUri());
        assertEquals(dto.getIp(), entity.getIp());
        assertEquals(dto.getTimestamp(), entity.getTimestamp());
    }
}