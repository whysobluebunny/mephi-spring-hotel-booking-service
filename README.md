# MEPhI Spring Hotel Booking Service

Микросервисный учебный проект на Spring Boot для системы бронирования отелей.

## Состав

- `eureka-server` — service registry.
- `api-gateway` — входной шлюз и маршрутизация.
- `booking-service` — пользователи, JWT, бронирования, двухшаговая согласованность.
- `hotel-service` — отели, номера, рекомендации по `timesBooked`, подтверждение доступности.

## Стек

- Java 17
- Spring Boot 3.5.0
- Spring Cloud 2025.0.0
- Spring Security + JWT
- Spring Data JPA + H2
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka
- springdoc OpenAPI
- Lombok, MapStruct
- JUnit 5 + MockMvc

## Запуск

Сначала запустите registry:

```bash
./mvnw -pl eureka-server spring-boot:run
```

Затем `hotel-service`:

```bash
./mvnw -pl hotel-service spring-boot:run
```

Потом `booking-service`:

```bash
./mvnw -pl booking-service spring-boot:run
```

И в конце `api-gateway`:

```bash
./mvnw -pl api-gateway spring-boot:run
```

## Docker Compose

Иначе собрать и поднять все сервисы можно одной командой:

```bash
docker compose up --build
```

После старта будут доступны:

- Gateway: `http://localhost:8080`
- Eureka: `http://localhost:8761`
- Booking Swagger: `http://localhost:8081/swagger-ui.html`
- Hotel Swagger: `http://localhost:8082/swagger-ui.html`

Остановить сервисы:

```bash
docker compose down
```

## Порты

- Gateway: `8080`
- Booking Service: `8081`
- Hotel Service: `8082`
- Eureka Server: `8761`

## Swagger

- Booking Service: `http://localhost:8081/swagger-ui.html`
- Hotel Service: `http://localhost:8082/swagger-ui.html`

## Demo Accounts

- `admin` / `admin` — роль `ADMIN`
- `user` / `user` — роль `USER`

## Environment Variables

- `APP_SECURITY_JWT_SECRET` — общий секрет для подписи и проверки JWT во всех сервисах
- `EUREKA_DEFAULT_ZONE` — адрес Eureka для клиентов
- `HOTEL_SERVICE_URL` — прямой URL `hotel-service` для внутренних вызовов из `booking-service`
- `SERVER_PORT` — локальный порт сервиса

При первом `docker compose up --build` запуск может занять несколько минут, потому что Docker и Maven прогревают кэш и скачивают зависимости.

## Тесты

```bash
./mvnw test
```

## Базовые сценарии

1. Зарегистрировать пользователя через `POST /api/users/register` в `booking-service`.
2. Получить JWT и передавать его в `Authorization: Bearer <token>`.
3. Администратором создать отель и комнаты.
4. Пользователем запросить свободные или рекомендованные комнаты.
5. Создать бронирование через `POST /api/bookings` с `X-Request-Id`.

## Предзаполнение данных

- В `booking-service` создаются пользователи `admin/admin` и `user/user`.
- В `hotel-service` создаются два отеля: `Aurora` и `Neva Palace`.
- Для комнат заранее формируется разная история подтверждений, чтобы рекомендации по `timesBooked` и аналитика по загрузке были видны сразу после старта.
- Одна из комнат (`Aurora 103`) помечена как операционно недоступная, чтобы показать отличие между `available` и занятостью по датам.

## Схема БД

### Booking Service

- `users`: `id`, `username`, `password`, `role`
- `bookings`: `id`, `user_id`, `room_id`, `request_id`, `start_date`, `end_date`, `status`, `created_at`

Особенности:

- `username` уникален
- `request_id` уникален и используется для идемпотентности повторной доставки запроса

### Hotel Service

- `hotels`: `id`, `name`, `address`
- `rooms`: `id`, `hotel_id`, `number`, `available`, `times_booked`
- `room_locks`: `id`, `room_id`, `booking_id`, `request_id`, `start_date`, `end_date`, `status`, `created_at`, `released_at`

Особенности:

- `rooms.hotel_id -> hotels.id`
- `room_locks.request_id` уникален для идемпотентности внутренних confirm/release запросов
- `room_locks` хранит временные блокировки слотов по датам и используется в саге бронирования

## Ключевые Endpoint'ы

- `POST /api/users/register` — регистрация пользователя и получение JWT
- `POST /api/users/auth` — вход и получение JWT
- `POST /api/bookings` — создание бронирования, поддерживает `X-Request-Id`
- `GET /api/bookings?page=0&size=10` — история своих бронирований с пагинацией
- `GET /api/rooms/recommend?startDate=...&endDate=...` — рекомендованные комнаты по `timesBooked`
- `GET /api/rooms/stats` — статистика по комнатам для `ADMIN`
- `GET /api/hotels/analytics` — агрегированная аналитика по отелям для `ADMIN`
- `POST /internal/rooms/{id}/confirm-availability` и `POST /internal/rooms/{id}/release` — внутренние маршруты межсервисной координации, не для внешнего клиента

## Correlation Headers

- `X-Request-Id` — ключ идемпотентности запроса на бронирование
- `X-Trace-Id` — сквозной идентификатор трассировки между gateway, booking-service и hotel-service

## Архитектура
![schema.png](docs/diagram/schema.png)

## Основные сценарии

### Успешное бронирование
![booking_success.png](docs/diagram/booking_success.png)

### Ошибка подтверждения и компенсация
![false_confirm_release.png](docs/diagram/false_confirm_release.png)

### Автоподбор комнаты
![find_room.png](docs/diagram/find_room.png)

## Пояснения к некоторым ключевым решениям (ADR)

### Согласованность через локальные транзакции и компенсацию

`booking-service` сначала фиксирует локальное бронирование в статусе `PENDING`, затем вызывает `hotel-service` для подтверждения доступности номера.
При ошибке или тайм-ауте выполняется компенсация через `release`, а бронь переводится в `CANCELLED`.

### Планирование занятости по простому счётчику

Для автоподбора используется простая и объяснимая стратегия: выбрать только свободные комнаты на указанный период и отсортировать их по `timesBooked`, затем по `id`.
