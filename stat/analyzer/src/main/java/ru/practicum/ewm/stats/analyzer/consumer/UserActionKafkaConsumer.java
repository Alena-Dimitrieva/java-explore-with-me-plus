package ru.practicum.ewm.stats.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.service.AnalyzerService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
@RequiredArgsConstructor
public class UserActionKafkaConsumer {

    private final AnalyzerService analyzerService;

    @KafkaListener(
            topics = "stats.user-actions.v1",
            groupId = "analyzer-user-actions"
    )
    public void consume(UserActionAvro action) {
        analyzerService.processUserAction(action);
    }
}

