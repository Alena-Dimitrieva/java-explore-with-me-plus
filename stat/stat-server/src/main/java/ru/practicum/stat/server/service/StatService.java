package ru.practicum.stat.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stat.dto.EndpointHitDto;
import ru.practicum.stat.dto.ViewStatsDto;
import ru.practicum.stat.server.dao.StatRepository;
import ru.practicum.stat.server.mapper.EndpointHitMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatService {

    private final StatRepository statRepository;

    @Transactional
    public void saveHit(@NonNull EndpointHitDto endpointHitDto) {
        log.debug("Сохранение записи о просмотре для URI: {}", endpointHitDto.getUri());
        statRepository.save(EndpointHitMapper.toEntity(endpointHitDto));
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (unique) {
            log.debug("Получение статистики (уникальные IP) для URI: {}", uris);
            return statRepository.getStatsUnique(start, end, uris);
        }

        log.debug("Получение общей статистики для URI: {}", uris);
        return statRepository.getStats(start, end, uris);
    }
}
