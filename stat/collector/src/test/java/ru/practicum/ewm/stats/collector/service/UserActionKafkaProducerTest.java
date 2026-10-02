package ru.practicum.ewm.stats.collector.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserActionKafkaProducerTest {

    @Mock
    private KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    @InjectMocks
    private UserActionKafkaProducer producer;

    @Test
    void send_shouldSendToUserActionsTopicWithUserIdAsKey() {
        UserActionAvro action = UserActionAvro.newBuilder()
                .setUserId(42L)
                .setEventId(7L)
                .setActionType(ActionTypeAvro.VIEW)
                .setTimestamp(Instant.now())
                .build();

        producer.send(action);

        verify(kafkaTemplate).send("stats.user-actions.v1", 42L, action);
    }
}
