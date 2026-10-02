package ru.practicum.ewm.stats.aggregator.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.service.SimilarityCalculator;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
@RequiredArgsConstructor
public class UserActionKafkaConsumer {

    private final SimilarityCalculator similarityCalculator;

    @KafkaListener(
            topics = "stats.user-actions.v1",
            groupId = "aggregator"
    )
    public void consume(UserActionAvro action) {
        similarityCalculator.process(action);
    }
}
