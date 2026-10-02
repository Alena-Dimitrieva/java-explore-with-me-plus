package ru.practicum.ewm.stats.collector.controller;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.collector.service.UserActionKafkaProducer;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserActionGrpcControllerTest {

    @Mock
    private UserActionKafkaProducer producer;

    @Mock
    private StreamObserver<Empty> observer;

    @InjectMocks
    private UserActionGrpcController controller;

    @Test
    void collectUserAction_shouldMapProtoToAvroAndSend() {
        Timestamp ts = Timestamp.newBuilder()
                .setSeconds(1_000L)
                .setNanos(500_000_000)
                .build();

        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(1L)
                .setEventId(2L)
                .setActionType(ActionTypeProto.ACTION_LIKE)
                .setTimestamp(ts)
                .build();

        controller.collectUserAction(request, observer);

        ArgumentCaptor<UserActionAvro> captor = ArgumentCaptor.forClass(UserActionAvro.class);
        verify(producer).send(captor.capture());

        UserActionAvro avro = captor.getValue();
        assertEquals(1L, avro.getUserId());
        assertEquals(2L, avro.getEventId());
        assertEquals(ActionTypeAvro.LIKE, avro.getActionType());
        assertEquals(Instant.ofEpochSecond(1_000L, 500_000_000), avro.getTimestamp());

        verify(observer).onNext(Empty.getDefaultInstance());
        verify(observer).onCompleted();
    }

    @Test
    void collectUserAction_shouldMapActionView() {
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(10L)
                .setEventId(20L)
                .setActionType(ActionTypeProto.ACTION_VIEW)
                .setTimestamp(Timestamp.getDefaultInstance())
                .build();

        controller.collectUserAction(request, observer);

        ArgumentCaptor<UserActionAvro> captor = ArgumentCaptor.forClass(UserActionAvro.class);
        verify(producer).send(captor.capture());
        assertEquals(ActionTypeAvro.VIEW, captor.getValue().getActionType());
    }

    @Test
    void collectUserAction_shouldMapActionRegister() {
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(10L)
                .setEventId(20L)
                .setActionType(ActionTypeProto.ACTION_REGISTER)
                .setTimestamp(Timestamp.getDefaultInstance())
                .build();

        controller.collectUserAction(request, observer);

        ArgumentCaptor<UserActionAvro> captor = ArgumentCaptor.forClass(UserActionAvro.class);
        verify(producer).send(captor.capture());
        assertEquals(ActionTypeAvro.REGISTER, captor.getValue().getActionType());
    }
}
