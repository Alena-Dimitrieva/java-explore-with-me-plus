package  ru.practicum.ewm.stats.collector.controller;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.collector.service.UserActionKafkaProducer;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;

import java.time.Instant;

@GrpcService
@RequiredArgsConstructor
public class UserActionGrpcController
        extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionKafkaProducer producer;

    @Override
    public void collectUserAction(
            UserActionProto request,
            StreamObserver<Empty> responseObserver) {

        try {
            UserActionAvro action = UserActionAvro.newBuilder()
                    .setUserId(request.getUserId())
                    .setEventId(request.getEventId())
                    .setActionType(mapType(request.getActionType()))
                    .setTimestamp(toInstant(request.getTimestamp()))
                    .build();

            producer.send(action);

            responseObserver.onNext(
                    Empty.getDefaultInstance()
            );
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos()
        );
    }

    private ActionTypeAvro mapType(ActionTypeProto type) {
        return switch (type) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED ->
                    throw new IllegalArgumentException(
                            "Неизвестный тип действия: " + type
                    );
        };
    }
}