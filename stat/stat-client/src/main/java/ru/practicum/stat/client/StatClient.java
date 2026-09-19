package ru.practicum.stat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stat.dto.EndpointHitDto;
import ru.practicum.stat.dto.StatsRequest;
import ru.practicum.stat.dto.ViewStatsDto;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
public class StatClient {

    private final String statsServiceId;
    private final DiscoveryClient discoveryClient;
    private final RestTemplate rest;
    private final RetryTemplate retryTemplate;

    public StatClient(@Value("${stats-server.id:stats-server}") String statsServiceId,
                      DiscoveryClient discoveryClient,
                      RestTemplateBuilder builder) {
        this.statsServiceId = statsServiceId;
        this.discoveryClient = discoveryClient;
        this.rest = builder.build();
        this.retryTemplate = buildRetryTemplate();
    }

    private RetryTemplate buildRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000L);
        template.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        template.setRetryPolicy(retryPolicy);

        return template;
    }

    private ServiceInstance getInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
        if (instances == null || instances.isEmpty()) {
            throw new IllegalStateException(
                    "Сервис статистики с id=" + statsServiceId + " не найден в реестре Eureka");
        }
        return instances.getFirst();
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    public void hit(EndpointHitDto endpointHitDto) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<EndpointHitDto> requestEntity = new HttpEntity<>(endpointHitDto, headers);

            rest.exchange(
                    makeUri("/hit"),
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e) {
            log.error("Ошибка записи: {}", endpointHitDto, e);
        }
    }

    public List<ViewStatsDto> getStat(StatsRequest statsRequest) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUri(makeUri("/stats"))
                    .queryParam("start", statsRequest.getStart())
                    .queryParam("end", statsRequest.getEnd())
                    .queryParam("unique", statsRequest.getUnique());

            List<String> uris = statsRequest.getUris();

            if (uris != null && !uris.isEmpty()) {
                builder.queryParam("uris", uris);
            }

            URI uri = builder.encode().build().toUri();

            return rest.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStatsDto>>() {
                    }
            ).getBody();

        } catch (Exception e) {
            log.error("Ошибка записи: {}", statsRequest, e);
            return null;
        }
    }
}
