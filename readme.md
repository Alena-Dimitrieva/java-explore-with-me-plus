# Explore With Me Plus

## О проекте

Explore With Me Plus — бэкенд для сервиса поиска мероприятий, реализованный как набор взаимодействующих Spring Boot сервисов. Сервис поддерживает публикацию мероприятий, заявки на участие, подборки, рейтинг и персонализированные рекомендации на основе косинусного сходства.

Проект использует Java 21, Spring Boot 3.5.9 и Spring Cloud 2025.0.3.

Основные технологии:

* Java 21
* Spring Boot 3.5.9
* Spring Cloud 2025.0.3
* Spring Web
* Spring Data JPA
* PostgreSQL
* Spring Cloud OpenFeign
* Eureka Service Discovery
* Spring Cloud Config
* Spring Cloud Gateway
* Resilience4j
* Apache Kafka
* Confluent Schema Registry
* Apache Avro
* gRPC (grpc-spring-boot-starter)

## Архитектура

Приложение состоит из инфраструктурных сервисов, бизнес-сервисов, рекомендательной системы и общих модулей для межсервисного взаимодействия.

### Инфраструктурные сервисы

`discovery-server`

Eureka Server. Отвечает за регистрацию сервисов и их поиск по имени.

Конфигурация:

`infra/discovery-server/src/main/resources/application.yml`

Порт:

`8761`

`config-server`

Spring Cloud Config Server. Отдаёт централизованную конфигурацию сервисов из каталога `config`.

Конфигурация самого сервера:

`infra/config-server/src/main/resources/application.yml`

Центральные конфигурации сервисов:

`infra/config-server/src/main/resources/config/event-service.yml`

`infra/config-server/src/main/resources/config/main-service.yml`

`infra/config-server/src/main/resources/config/rating-service.yml`

`infra/config-server/src/main/resources/config/request-service.yml`

`infra/config-server/src/main/resources/config/user-service.yml`

`infra/config-server/src/main/resources/config/collector.yml`

`infra/config-server/src/main/resources/config/aggregator.yml`

`infra/config-server/src/main/resources/config/analyzer.yml`

`gateway-server`

Spring Cloud Gateway. Принимает внешние HTTP-запросы на порту `8080` и направляет их в сервисы через Eureka и Spring Cloud LoadBalancer.

Локальная конфигурация:

`infra/gateway-server/src/main/resources/application.yml`

Центральная конфигурация маршрутов:

`infra/config-server/src/main/resources/config/gateway-server.yml`

Основные маршруты:

* `/events/**`, `/categories/**`, `/compilations/**`, `/admin/events/**`, `/admin/categories/**`, `/admin/compilations/**`, `/users/*/events/**` → `event-service`
* `/users/*/events/*/likes/**` → `rating-service`
* `/users/*/events/*/requests/**` → `request-service`
* `/users/*/requests/**` → `request-service`
* `/admin/users/**` → `user-service`
* остальные запросы → `main-service`

### Бизнес-сервисы

`event-service`

Отвечает за работу с событиями, категориями и подборками событий. Предоставляет внутренний API для получения данных о событии. Поле `views` заменено на `rating` — рейтинг мероприятия запрашивается у сервиса Analyzer через gRPC.

Класс запуска:

`core/event-service/src/main/java/ru/practicum/event/EventServiceApp.java`

Локальная конфигурация:

`core/event-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/event-service.yml`

Для межсервисного взаимодействия `event-service` использует `user-client`, `request-client` и `stat-client` (в нём gRPC-клиенты `AnalyzerClient` и `CollectorClient`).

`request-service`

Отвечает за заявки пользователей на участие в событиях. При создании заявки отправляет `ACTION_REGISTER` в Collector через gRPC.

Класс запуска:

`core/request-service/src/main/java/ru/practicum/request/RequestServiceApp.java`

Локальная конфигурация:

`core/request-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/request-service.yml`

Для межсервисного взаимодействия использует `user-client`, `event-client` и `stat-client` (gRPC-клиент `CollectorClient`).

`user-service`

Отвечает за пользователей и предоставляет внутренний API для получения краткой информации о пользователе и проверки его существования.

Класс запуска:

`core/user-service/src/main/java/ru/practicum/user/UserServiceApp.java`

Локальная конфигурация:

`core/user-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/user-service.yml`

`rating-service`

Отвечает за работу с оценками событий.

Класс запуска:

`core/rating-service/src/main/java/ru/practicum/rating/RatingServiceApp.java`

Локальная конфигурация:

`core/rating-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/rating-service.yml`

Для проверки пользователя и получения данных события использует `user-client` и `event-client`.

`main-service`

Отдельный Spring Boot сервис, зарегистрированный в Eureka. В gateway для него предусмотрен маршрут по умолчанию `/**`, поэтому сервис обрабатывает запросы, которые не были переданы в специализированные сервисы.

Конфигурация:

`core/main-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/main-service.yml`

### Рекомендательная система

Рекомендательная система — три микросервиса, обменивающихся сообщениями через Apache Kafka. Все сообщения — в формате Avro, схемы хранятся в Confluent Schema Registry. Взаимодействие core-сервисов с рекомендательной системой идёт по gRPC.

`collector`

Принимает действия пользователей (`VIEW`, `REGISTER`, `LIKE`) по gRPC и публикует их в Kafka-топик `stats.user-actions.v1` в формате Avro.

Класс запуска:

`stat/collector/src/main/java/ru/practicum/ewm/stats/collector/CollectorApplication.java`

gRPC-сервис: `stats.service.collector.UserActionController`
Метод: `CollectUserAction(UserActionProto) → Empty`

Порт gRPC-сервера выбирается случайным образом (`grpc.server.port: 0`).

`aggregator`

Читает топик `stats.user-actions.v1` и пересчитывает косинусное сходство между мероприятиями. Результаты публикует в топик `stats.events-similarity.v1`.

Класс запуска:

`stat/aggregator/src/main/java/ru/practicum/ewm/stats/aggregator/AggregatorApplication.java`

Формула косинусного сходства:

```
similarity(i_p, i_q) = S_min(i_p, i_q) / (sqrt(S_i_p) × sqrt(S_i_q))
```

где `S_min(i_p, i_q)` — сумма минимальных весов действий пользователей с обоими мероприятиями, `S_i_p` и `S_i_q` — общие суммы весов действий с каждым мероприятием.

Идентификаторы пары всегда упорядочены по возрастанию: `eventA < eventB`.

`analyzer`

Читает оба топика (`stats.user-actions.v1` и `stats.events-similarity.v1`), хранит историю взаимодействий пользователей с мероприятиями и коэффициенты сходства в БД. По gRPC отдаёт рекомендации core-сервисам.

Класс запуска:

`stat/analyzer/src/main/java/ru/practicum/ewm/stats/analyzer/AnalyzerApplication.java`

gRPC-сервис: `stats.service.dashboard.RecommendationsController`
Методы:

* `GetRecommendationsForUser(UserPredictionsRequestProto) → stream RecommendedEventProto` — предсказанные оценки для пользователя.
* `GetSimilarEvents(SimilarEventsRequestProto) → stream RecommendedEventProto` — похожие мероприятия, с которыми пользователь ещё не взаимодействовал.
* `GetInteractionsCount(InteractionsCountRequestProto) → stream RecommendedEventProto` — сумма весов взаимодействий по указанным мероприятиям.
* `HasUserInteracted(UserInteractionRequestProto) → BoolValue` — проверка, взаимодействовал ли пользователь с мероприятием.

Порт gRPC-сервера выбирается случайным образом (`grpc.server.port: 0`).

#### Kafka-топики

* `stats.user-actions.v1` — действия пользователей с мероприятиями (`UserActionAvro`).
* `stats.events-similarity.v1` — коэффициенты сходства между мероприятиями (`EventSimilarityAvro`).

#### Avro-схемы

Все Avro-сообщения находятся в неймспейсе `ru.practicum.ewm.stats.avro`.

`UserActionAvro`:

* `userId` (long)
* `eventId` (long)
* `actionType` (enum `ActionTypeAvro`: `VIEW`, `REGISTER`, `LIKE`)
* `timestamp` (long, `timestamp-millis`)

`EventSimilarityAvro`:

* `eventA` (long) — id меньшего мероприятия
* `eventB` (long) — id большего мероприятия
* `score` (double)
* `timestamp` (long, `timestamp-millis`)

#### Веса действий

При расчёте сходства используются веса действий:

* `VIEW` — 1
* `REGISTER` — 3
* `LIKE` — 5

При нескольких действиях пользователя с одним мероприятием учитывается только действие с максимальным весом.

## Общие модули

Для уменьшения дублирования DTO и клиентского кода используются отдельные Maven модули.

### Модуль event

`event/event-dto` — DTO для обмена данными о событиях.

`event/event-client` — Feign-клиент `EventClient` для обращения к `event-service`.

### Модуль user

`user/user-dto` — DTO пользователей.

`user/user-client` — Feign-клиент `UserClient` и fallback для взаимодействия с `user-service`.

### Модуль request

`request/request-dto` — DTO заявок.

`request/request-client` — Feign-клиент `RequestClient` для получения количества подтверждённых заявок.

### Модуль stat

`stat/stat-client` — gRPC-клиенты `CollectorClient` и `AnalyzerClient`, которые находят соответствующие сервисы через Eureka (`discovery:///collector`, `discovery:///analyzer`) и обращаются по gRPC.

`stat/serialization/proto-schemas` — Protobuf-схемы для gRPC-контрактов.

`stat/serialization/avro-schemas` — Avro-схемы для Kafka-сообщений.

### Модуль infra

`infra/discovery-server`, `infra/config-server`, `infra/gateway-server` — инфраструктурные сервисы.

### Модуль serialization

`serialization` — родительский pom для модулей `proto-schemas` и `avro-schemas`.

## Взаимодействие сервисов

Основные межсервисные взаимодействия:

* `event-service` использует `user-service` и `request-service` (Feign), а также `AnalyzerClient` и `CollectorClient` (gRPC через `stat-client`).
* `request-service` использует `user-service`, `event-service` (Feign) и `CollectorClient` (gRPC).
* `rating-service` использует `user-service` и `event-service` (Feign).
* `main-service` работает через остальные сервисы.
* `collector` → Kafka → `aggregator` → Kafka → `analyzer`.
* Core-сервисы не обращаются к `collector`/`analyzer` напрямую по HTTP — только по gRPC.

Для бизнес-сервисов взаимодействие реализовано через Feign-клиенты. Для рекомендательной системы — через gRPC и Kafka.

## Внутренний API

### event-service

#### Получить данные события

```text
GET /internal/events/{eventId}
```

Используется `request-service` и `rating-service`.

Возвращает внутреннее представление события `EventInternalDto`.

### user-service

#### Получить пользователя

```text
GET /internal/users/{userId}
```

Возвращает `UserShortDto`.

#### Получить пользователей списком

```text
GET /internal/users/batch?ids={id1}&ids={id2}
```

Возвращает список `UserShortDto`.

#### Проверить существование пользователя

```text
GET /internal/users/{userId}/exists
```

Возвращает `true` или `false`.

Основные потребители внутреннего API `user-service` — `event-service`, `request-service` и `rating-service`.

### request-service

#### Получить количество подтверждённых заявок для события

```text
GET /internal/requests/{eventId}/confirmed-count
```

Возвращает количество подтверждённых заявок.

#### Получить количество подтверждённых заявок для списка событий

```text
GET /internal/requests/confirmed-count?eventIds={id1}&eventIds={id2}
```

Возвращает список `EventRequestCountDto`.

Батч-запрос используется для уменьшения количества межсервисных вызовов при обработке списков событий.

Основные потребители — `event-service` и `main-service`.

### Collector (gRPC)

Пакет: `stats.service.collector`

Метод `CollectUserAction` принимает `UserActionProto`:

* `user_id` (int64)
* `event_id` (int64)
* `action_type` (`ActionTypeProto`: `ACTION_VIEW`, `ACTION_REGISTER`, `ACTION_LIKE`)
* `timestamp` (`google.protobuf.Timestamp`)

Возвращает `google.protobuf.Empty`.

### Analyzer (gRPC)

Пакет: `stats.service.dashboard`

Сервис `RecommendationsController`:

* `GetRecommendationsForUser(UserPredictionsRequestProto)` — предсказанные оценки.
* `GetSimilarEvents(SimilarEventsRequestProto)` — похожие мероприятия.
* `GetInteractionsCount(InteractionsCountRequestProto)` — суммы весов взаимодействий.
* `HasUserInteracted(UserInteractionRequestProto)` — проверка взаимодействия.

## Внешний API

### event-service — новые эндпоинты

#### Получить рекомендации мероприятий

```text
GET /events/recommendations
```

Идентификатор пользователя — в HTTP-заголовке `X-EWM-USER-ID`.

Возвращает список `EventShortDto` с `rating`, рекомендованных Analyzer на основе similarity и истории пользователя.

#### Поставить лайк мероприятию

```text
PUT /events/{eventId}/like
```

Идентификатор пользователя — в HTTP-заголовке `X-EWM-USER-ID`.

Пользователь может лайкать только те мероприятия, которые он просматривал. В противном случае возвращается `400 BAD REQUEST`.

При успехе отправляет `ACTION_LIKE` в Collector по gRPC.

### Замена views на rating

Поле `views` (`long`) в DTO события заменено на `rating` (`double`). Значение `rating` получается у Analyzer через gRPC (`GetInteractionsCount`) и представляет сумму максимальных весов действий пользователей с мероприятием.

Коллекция запросов для основного внешнего API:

[ewm-main-service.json](https://github.com/Alena-Dimitrieva/java-explore-with-me-plus-main/blob/main/postman/ewm-main-service.json)

Спецификация основного API также хранится в корне проекта в файле `ewm-main-service-spec.json`.

## Отказоустойчивость

Для Feign-вызовов `event-service` используются Resilience4j Circuit Breaker, Retry и TimeLimiter.

Основные настройки находятся в:

`infra/config-server/src/main/resources/config/event-service.yml`

В конфигурации заданы:

* максимальное количество попыток Retry — 3;
* пауза между попытками — 500 миллисекунд;
* размер окна Circuit Breaker — 10 вызовов;
* порог ошибок — 50 процентов;
* время открытого состояния Circuit Breaker — 10 секунд;
* количество пробных вызовов в half-open состоянии — 3;
* timeout TimeLimiter — 3 секунды.

Для `user-client` используется fallback фабрика, поэтому ошибка удалённого `user-service` может быть обработана в клиентском слое.

Для gRPC-клиентов (`CollectorClient`, `AnalyzerClient`) Resilience4j не применяется — отказоустойчивость обеспечивается на уровне транспорта: включены `keepAlive` и `keepAliveWithoutCalls`, а также используется `negotiationType: plaintext` для локальной разработки.

## Конфигурация

Конфигурация приложения разделена на два уровня.

### Локальная конфигурация

Каждый сервис содержит собственный файл `application.yaml` или `application.yml`. В нём находятся базовые настройки запуска сервиса и подключение к инфраструктурным сервисам.

Основные файлы:

```text
core/event-service/src/main/resources/application.yaml
core/main-service/src/main/resources/application.yaml
core/rating-service/src/main/resources/application.yaml
core/request-service/src/main/resources/application.yaml
core/user-service/src/main/resources/application.yaml
stat/collector/src/main/resources/application.yml
stat/aggregator/src/main/resources/application.yml
stat/analyzer/src/main/resources/application.yml
infra/gateway-server/src/main/resources/application.yml
infra/discovery-server/src/main/resources/application.yml
infra/config-server/src/main/resources/application.yml
```

### Центральная конфигурация

Основные настройки сервисов хранятся в `config-server`:

```text
infra/config-server/src/main/resources/config/event-service.yml
infra/config-server/src/main/resources/config/main-service.yml
infra/config-server/src/main/resources/config/rating-service.yml
infra/config-server/src/main/resources/config/request-service.yml
infra/config-server/src/main/resources/config/user-service.yml
infra/config-server/src/main/resources/config/gateway-server.yml
infra/config-server/src/main/resources/config/collector.yml
infra/config-server/src/main/resources/config/aggregator.yml
infra/config-server/src/main/resources/config/analyzer.yml
```

В центральной конфигурации находятся настройки PostgreSQL, JPA, Eureka, OpenFeign, Gateway, Kafka, Schema Registry и параметры отказоустойчивости.

## Базы данных

Для бизнес-сервисов и рекомендательной системы используются отдельные PostgreSQL базы данных.

В текущей конфигурации:

* `event-service` использует базу `ewm-event`;
* `user-service` использует базу `ewm-user`;
* `request-service` использует базу `ewm-request`;
* `rating-service` использует базу `ewm-rating`;
* `analyzer` использует базу `ewm-stats`;
* `main-service` не содержит подключения к PostgreSQL в своей центральной конфигурации.

В БД `ewm-stats` используются таблицы:

* `user_event_interactions` — история взаимодействий пользователей с мероприятиями: `user_id`, `event_id`, `weight`, `last_interaction_at`.
* `event_similarity` — коэффициенты сходства: `event_a`, `event_b`, `score`, `updated_at`.

SQL-схемы находятся в `src/main/resources` соответствующих сервисов.

## Apache Kafka и Schema Registry

Kafka и Schema Registry поднимаются в `docker-compose`. Топики создаются автоматически сервисом `kafka-init-topics`.

Для запуска инфраструктуры:

```bash
docker compose up -d
```

После запуска доступны:

* Kafka — `localhost:9092`
* Schema Registry — `localhost:8081`

Проверка топиков:

```bash
docker exec ewm-kafka kafka-topics --bootstrap-server kafka:29092 --list
```

Проверка Schema Registry:

```bash
curl http://localhost:8081/subjects
```

## Структура проекта

### Корневые модули

* `core` — бизнес-сервисы (event, request, user, rating, main).
* `infra` — инфраструктурные сервисы (discovery, config, gateway).
* `event` — DTO и Feign-клиент для `event-service`.
* `user` — DTO и Feign-клиент для `user-service`.
* `request` — DTO и Feign-клиент для `request-service`.
* `stat` — gRPC-клиенты, Protobuf/Avro-схемы и рекомендательная система (`collector`, `aggregator`, `analyzer`).

### Организация кода бизнес-сервисов

В сервисах используется разделение на:

* `controller` — HTTP-контроллеры и gRPC-контроллеры;
* `service` — бизнес-логика;
* `dao` — работа с данными;
* `model` — сущности;
* `dto` — объекты передачи данных;
* `mapper` — преобразование объектов;
* `util` — вспомогательные классы.

В `event-client`, `user-client`, `request-client` и `stat-client` находятся компоненты, предназначенные для межсервисного взаимодействия.

## Запуск

Рекомендуемый порядок запуска:
1. Инфраструктура: `docker compose`.
2. `discovery-server`.
3. `config-server`.
4. `gateway-server`.
5. `collector`.
6. `aggregator`.
7. `analyzer`.
8. `user-service`, `event-service`, `request-service`, `rating-service`, `main-service`.

После запуска все сервисы должны быть зарегистрированы в Eureka: `http://localhost:8761/`.