package ru.practicum.ewm.stats.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.service.AnalyzerService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Component
@RequiredArgsConstructor
public class EventSimilarityKafkaConsumer {

    private final AnalyzerService analyzerService;

    @KafkaListener(
            topics = "stats.events-similarity.v1",
            groupId = "analyzer-similarity"
    )
    public void consume(EventSimilarityAvro similarity) {
        analyzerService.processSimilarity(similarity);
    }
}
