package ru.practicum.ewm.stats.analyzer.controller;

import com.google.protobuf.BoolValue;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.analyzer.service.AnalyzerService;
import ru.practicum.ewm.stats.proto.dashboard.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.dashboard.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.dashboard.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.UserInteractionRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.UserPredictionsRequestProto;

@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcController
        extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final AnalyzerService analyzerService;

    @Override
    public void getRecommendationsForUser(
            UserPredictionsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            analyzerService.getRecommendationsForUser(
                            request.getUserId(),
                            request.getMaxResults())
                    .forEach(event -> responseObserver.onNext(toProto(event)));

            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription(ex.getMessage())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void getSimilarEvents(
            SimilarEventsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            analyzerService.getSimilarEvents(
                            request.getEventId(),
                            request.getUserId(),
                            request.getMaxResults())
                    .forEach(event -> responseObserver.onNext(toProto(event)));

            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription(ex.getMessage())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void getInteractionsCount(
            InteractionsCountRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            analyzerService.getInteractionsCount(request.getEventIdList())
                    .forEach(event -> responseObserver.onNext(toProto(event)));

            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription(ex.getMessage())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void hasUserInteracted(
            UserInteractionRequestProto request,
            StreamObserver<BoolValue> responseObserver) {
        try {
            boolean result = analyzerService.hasUserInteracted(
                    request.getUserId(),
                    request.getEventId()
            );

            responseObserver.onNext(BoolValue.of(result));
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription(ex.getMessage())
                            .asRuntimeException()
            );
        }
    }

    private RecommendedEventProto toProto(AnalyzerService.ScoredEvent event) {
        return RecommendedEventProto.newBuilder()
                .setEventId(event.eventId())
                .setScore(event.score())
                .build();
    }
}