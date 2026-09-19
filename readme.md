# Explore With Me Plus

## О проекте

Explore With Me Plus - бэкенд для сервиса поиска мероприятий, реализованный как набор взаимодействующих Spring Boot
сервисов.

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

## Архитектура

Приложение состоит из инфраструктурных сервисов, бизнес-сервисов и общих модулей для межсервисного взаимодействия.

### Инфраструктурные сервисы

`discovery-server`

Eureka Server. Отвечает за регистрацию сервисов и их поиск по имени.

Конфигурация:

`infra/discovery-server/src/main/resources/application.yml`

Порт:

`8761`

`config-server`

Spring Cloud Config Server. Отдает централизованную конфигурацию сервисов из каталога `config`.

Конфигурация самого сервера:

`infra/config-server/src/main/resources/application.yml`

Центральные конфигурации сервисов:

`infra/config-server/src/main/resources/config/event-service.yml`

`infra/config-server/src/main/resources/config/main-service.yml`

`infra/config-server/src/main/resources/config/rating-service.yml`

`infra/config-server/src/main/resources/config/request-service.yml`

`infra/config-server/src/main/resources/config/stats-server.yml`

`infra/config-server/src/main/resources/config/user-service.yml`

`gateway-server`

Spring Cloud Gateway. Принимает внешние HTTP-запросы на порту `8080` и направляет их в сервисы через Eureka и Spring
Cloud LoadBalancer.

Локальная конфигурация:

`infra/gateway-server/src/main/resources/application.yml`

Центральная конфигурация маршрутов:

`infra/config-server/src/main/resources/config/gateway-server.yml`

Основные маршруты:

* `/events/**`, `/categories/**`, `/compilations/**`, `/admin/events/**`, `/admin/categories/**`,
  `/admin/compilations/**`, `/users/*/events/**` -> `event-service`
* `/users/*/events/*/likes/**` -> `rating-service`
* `/users/*/events/*/requests/**` -> `request-service`
* `/users/*/requests/**` -> `request-service`
* `/admin/users/**` -> `user-service`
* остальные запросы -> `main-service`

### Бизнес-сервисы

`event-service`

Отвечает за работу с событиями, категориями и подборками событий. Сервис также предоставляет внутренний API для
получения данных о событии и изменения его рейтинга.

Класс запуска:

`core/event-service/src/main/java/ru/practicum/event/EventServiceApp.java`

Локальная конфигурация:

`core/event-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/event-service.yml`

Для межсервисного взаимодействия `event-service` использует `user-client`, `request-client` и `stat-client`.

`request-service`

Отвечает за заявки пользователей на участие в событиях.

Класс запуска:

`core/request-service/src/main/java/ru/practicum/request/RequestServiceApp.java`

Локальная конфигурация:

`core/request-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/request-service.yml`

Для межсервисного взаимодействия использует `user-client` и `event-client`.

`user-service`

Отвечает за пользователей и предоставляет внутренний API для получения краткой информации о пользователе и проверки его
существования.

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

Отдельный Spring Boot сервис, зарегистрированный в Eureka. В gateway для него предусмотрен маршрут по умолчанию `/**`,
поэтому сервис обрабатывает запросы, которые не были переданы в специализированные сервисы.

Конфигурация:

`core/main-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/main-service.yml`

`stat-server`

Отвечает за запись информации о просмотрах и выдачу статистики по URI.

Класс запуска:

`stat/stat-server/src/main/java/ru/practicum/stat/server/StatServerApp.java`

Локальная конфигурация:

`stat/stat-server/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/stats-server.yml`

## Общие модули

Для уменьшения дублирования DTO и клиентского кода используются отдельные Maven модули.

### Модуль event

`event/event-dto`

Содержит DTO для обмена данными о событиях.

`event/event-client`

Содержит Feign клиент `EventClient` для обращения к `event-service`.

### Модуль user

`user/user-dto`

Содержит DTO пользователей.

`user/user-client`

Содержит Feign клиент `UserClient` и fallback для взаимодействия с `user-service`.

### Модуль request

`request/request-dto`

Содержит DTO заявок.

`request/request-client`

Содержит Feign клиент `RequestClient` для получения количества подтвержденных заявок.

### Модуль stat

`stat/stat-dto`

Содержит DTO статистики: `EndpointHitDto`, `StatsRequest` и `ViewStatsDto`.

`stat/stat-client`

Содержит `StatClient`, который находит `stats-server` через Eureka и взаимодействует с ним через `RestTemplate`.

## Взаимодействие сервисов

Основные межсервисные взаимодействия:

* `event-service` использует `user-service`, `request-service` и `stat-server`.
* `request-service` использует `user-service` и `event-service`.
* `rating-service` использует `user-service` и `event-service`.
* `main-service` использует `stat-server` через модуль `stat-client`.

Для сервисов `event-service`, `request-service` и `rating-service` взаимодействие с бизнес-сервисами реализовано через
Feign клиенты.

Для статистики используется отдельный `stat-client`, который получает экземпляр `stats-server` из Eureka и отправляет
HTTP-запросы через `RestTemplate`.

## Внутренний API

### event-service

#### Получить данные события

```text
GET /internal/events/{eventId}
```

Используется `request-service` и `rating-service`.

Возвращает внутреннее представление события `EventInternalDto`.

#### Обновить рейтинг события

```text
PATCH /internal/events/{eventId}/rate?rate={rate}
```

Используется `rating-service` для передачи нового значения рейтинга.

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

Основные потребители внутреннего API `user-service` - `event-service`, `request-service` и `rating-service`.

### request-service

#### Получить количество подтвержденных заявок для события

```text
GET /internal/requests/{eventId}/confirmed-count
```

Возвращает количество подтвержденных заявок.

#### Получить количество подтвержденных заявок для списка событий

```text
GET /internal/requests/confirmed-count?eventIds={id1}&eventIds={id2}
```

Возвращает список `EventRequestCountDto`.

Батч-запрос используется для уменьшения количества межсервисных вызовов при обработке списков событий.

Основные потребители - `event-service` и `main-service`.

### stat-server

Для взаимодействия со статистикой используется модуль `stat-client`.

#### Записать просмотр

```text
POST /hit
```

В тело передается `EndpointHitDto`.

Основные поля:

```text
app
uri
ip
timestamp
```

#### Получить статистику

```text
GET /stats
```

Параметры запроса:

```text
start
end
uris
unique
```

Возвращается список `ViewStatsDto`.

## Отказоустойчивость

Для Feign-вызовов `event-service` используются Resilience4j Circuit Breaker, Retry и TimeLimiter.

Основные настройки находятся в:

`infra/config-server/src/main/resources/config/event-service.yml`

В конфигурации заданы:

* максимальное количество попыток Retry - 3;
* пауза между попытками - 500 миллисекунд;
* размер окна Circuit Breaker - 10 вызовов;
* порог ошибок - 50 процентов;
* время открытого состояния Circuit Breaker - 10 секунд;
* количество пробных вызовов в half-open состоянии - 3;
* timeout TimeLimiter - 3 секунды.

Для `user-client` используется fallback фабрика, поэтому ошибка удаленного `user-service` может быть обработана в
клиентском слое.

`stat-client` также выполняет до 3 попыток поиска экземпляра `stats-server` в Eureka с фиксированной паузой 1 секунда.
Ошибки обращения к статистике обрабатываются внутри клиента.

## Конфигурация

Конфигурация приложения разделена на два уровня.

### Локальная конфигурация

Каждый сервис содержит собственный файл `application.yaml` или `application.yml`. В нем находятся базовые настройки
запуска сервиса и подключение к инфраструктурным сервисам.

Основные файлы:

```text
core/event-service/src/main/resources/application.yaml
core/main-service/src/main/resources/application.yaml
core/rating-service/src/main/resources/application.yaml
core/request-service/src/main/resources/application.yaml
core/user-service/src/main/resources/application.yaml
stat/stat-server/src/main/resources/application.yaml
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
infra/config-server/src/main/resources/config/stats-server.yml
infra/config-server/src/main/resources/config/user-service.yml
infra/config-server/src/main/resources/config/gateway-server.yml
```

В центральной конфигурации находятся настройки PostgreSQL, JPA, Eureka, OpenFeign, Gateway и параметры
отказоустойчивости, если они используются конкретным сервисом.

## Базы данных

Для бизнес-сервисов используются отдельные PostgreSQL базы данных.

В текущей конфигурации:

* `event-service` использует базу `ewm-event`;
* `user-service` использует базу `ewm-user`;
* `request-service` использует базу `ewm-request`;
* `rating-service` использует базу `ewm-rating`;
* `stat-server` использует базу `ewm-stats`;
* `main-service` не содержит подключения к PostgreSQL в своей центральной конфигурации.

SQL схемы находятся в `src/main/resources` соответствующих сервисов.

## Внешний API

Коллекция запросов для основного внешнего API:

[ewm-main-service.json](https://github.com/Alena-Dimitrieva/java-explore-with-me-plus-main/blob/main/postman/ewm-main-service.json)

Спецификация основного API также хранится в корне проекта в файле `ewm-main-service-spec.json`.

Спецификация сервиса статистики хранится в корне проекта в файле `ewm-stats-service-spec.json`.

## Структура проекта

### Корневые модули

* `core` - бизнес-сервисы.
* `infra` - инфраструктурные сервисы.
* `event` - DTO и клиент для `event-service`.
* `user` - DTO и клиент для `user-service`.
* `request` - DTO и клиент для `request-service`.
* `stat` - DTO, клиент и сервер статистики.

### Организация кода бизнес-сервисов

В сервисах используется разделение на:

* `controller` - HTTP контроллеры;
* `service` - бизнес-логика;
* `dao` - работа с данными;
* `model` - сущности;
* `dto` - объекты передачи данных;
* `mapper` - преобразование объектов;
* `util` - вспомогательные классы.

В `event-client`, `user-client`, `request-client` и `stat-client` находятся компоненты, предназначенные для
межсервисного взаимодействия.
