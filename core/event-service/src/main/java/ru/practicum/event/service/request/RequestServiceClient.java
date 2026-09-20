package ru.practicum.event.service.request;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.request.client.RequestClient;
import ru.practicum.request.dto.EventRequestCountDto;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestServiceClient {

    private final RequestClient requestClient;

    @Retry(name = "requestService", fallbackMethod = "getConfirmedCountsFallback")
    public List<EventRequestCountDto> getConfirmedCounts(List<Long> eventIds) {
        return requestClient.getConfirmedCounts(eventIds);
    }

    private List<EventRequestCountDto> getConfirmedCountsFallback(
            List<Long> eventIds,
            Exception ex) {

        return Collections.emptyList();
    }

    @Retry(name = "requestService", fallbackMethod = "getConfirmedCountFallback")
    public long getConfirmedCount(Long eventId) {
        return requestClient.getConfirmedCount(eventId);
    }

    private long getConfirmedCountFallback(
            Long eventId,
            Exception ex) {

        return 0L;
    }
}
