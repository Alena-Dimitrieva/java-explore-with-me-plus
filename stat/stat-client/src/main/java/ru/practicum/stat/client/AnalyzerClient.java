package ru.practicum.stat.client;



import com.google.protobuf.BoolValue;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.dashboard.*;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.Spliterator;
import java.util.Spliterators;

@Component
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Stream<RecommendedEventProto> getRecommendationsForUser(
            long userId,
            int maxResults) {

        UserPredictionsRequestProto request =
                UserPredictionsRequestProto.newBuilder()
                        .setUserId(userId)
                        .setMaxResults(maxResults)
                        .build();

        return asStream(client.getRecommendationsForUser(request));
    }

    public Stream<RecommendedEventProto> getSimilarEvents(
            long eventId,
            long userId,
            int maxResults) {

        SimilarEventsRequestProto request =
                SimilarEventsRequestProto.newBuilder()
                        .setEventId(eventId)
                        .setUserId(userId)
                        .setMaxResults(maxResults)
                        .build();

        return asStream(client.getSimilarEvents(request));
    }

    public Stream<RecommendedEventProto> getInteractionsCount(
            List<Long> eventIds) {

        InteractionsCountRequestProto.Builder builder =
                InteractionsCountRequestProto.newBuilder();

        eventIds.forEach(builder::addEventId);
        return asStream(client.getInteractionsCount(builder.build()));
    }

    public boolean hasUserInteracted(long userId, long eventId) {
        UserInteractionRequestProto request =
                UserInteractionRequestProto.newBuilder()
                        .setUserId(userId)
                        .setEventId(eventId)
                        .build();

        BoolValue result = client.hasUserInteracted(request);
        return result.getValue();
    }

    private Stream<RecommendedEventProto> asStream(
            Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        iterator,
                        Spliterator.ORDERED
                ),
                false
        );
    }
}
