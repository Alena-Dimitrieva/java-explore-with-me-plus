package ru.practicum.event.util.statistic;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.event.util.error.exception.HitRequestException;
import ru.practicum.event.util.error.exception.StatResponseException;
import ru.practicum.stat.client.StatClient;
import ru.practicum.stat.dto.EndpointHitDto;
import ru.practicum.stat.dto.StatsRequest;
import ru.practicum.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StatRepository {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final LocalDateTime startUnixEpoch = LocalDateTime.parse("1970-01-01T00:00:00");

    @Value("${app.name:ewm-main-service}")
    private String appName;

    private final StatClient statClient;

    public void sendHitRequest(HttpServletRequest request) {
        try {
            statClient.hit(EndpointHitDto.builder()
                    .ip(request.getRemoteAddr())
                    .uri(request.getRequestURI())
                    .app(appName)
                    .timestamp(LocalDateTime.now())
                    .build()
            );
        } catch (Exception ex) {
            throw new HitRequestException(ex);
        }
    }

    public List<ViewStatsDto> getStat(List<String> statUris,
                                      LocalDateTime rangeStart,
                                      LocalDateTime rangeEnd,
                                      boolean uniqe) {
        if (statUris == null || statUris.isEmpty()) {
            throw new StatResponseException();
        }

        if (rangeStart == null) {
            rangeStart = startUnixEpoch;
        }

        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now();
        }

        StatsRequest request = StatsRequest.builder()
                .uris(statUris)
                .start(rangeStart.truncatedTo(ChronoUnit.MILLIS).format(formatter))
                .end(rangeEnd.truncatedTo(ChronoUnit.MILLIS).format(formatter))
                .unique(uniqe)
                .build();

        List<ViewStatsDto> stats;
        try {
            stats = statClient.getStat(request);
        } catch (Exception ex) {
            throw new StatResponseException(ex);
        }

        if (stats == null) {
            throw new StatResponseException();
        }

        return stats;
    }

    public List<ViewStatsDto> getStat(List<String> statUris, boolean uniqe) {
        return getStat(statUris, null, null, uniqe);
    }
}

