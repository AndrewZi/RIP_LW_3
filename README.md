# Sensor Reactive System

Результаты сравнений профилирования проектов ЛР 2 (before) и ЛР 3 (after) в папке Screens.

Производительность улучшилась.

Полнофункциональный Spring Boot проект на Java с реактивным программированием, использующий WebFlux для потоковой обработки данных датчиков.

## Технический стек

- **Spring Boot 3.4.0** с WebFlux для реактивности
- **Java 17+**
- **PostgreSQL** для основной БД
- **Docker & Docker Compose** для контейнеризации
- **Lombok** для снижения boilerplate кода
- **Maven** для управления зависимостями
- **SLF4J** для логирования
- **Flyway** для миграций БД
- **R2DBC** для асинхронного доступа к БД
- **Reactor** для реактивного программирования

## Быстрый старт

### С использованием Docker (Рекомендуется)

```bash
# Построить Docker образ и запустить контейнеры
make docker-build
make docker-up

# Просмотреть логи
make logs
# 
# Остановить контейнеры
make docker-down
```

## Доступные Makefile команды

| Команда | Описание |
|---------|---------|
| `make help` | Показать все доступные команды |
| `make build` | Собрать Maven проект (compile и package) |
| `make clean` | Очистить build артефакты и Docker образы |
| `make run` | Запустить приложение локально |
| `make docker-build` | Построить Docker образ |
| `make docker-up` | Запустить контейнеры с docker-compose |
| `make docker-down` | Остановить и удалить контейнеры |
| `make docker-logs` | Просмотреть логи контейнера |
| `make ps` | Список запущенных контейнеров |
| `make logs` | Просмотреть логи приложения |
| `make health` | Проверить health статус приложения |
| `make reset-db` | Сбросить базу данных |

## API Endpoints

### Service B (Server) - Потоковые данные датчиков

#### Получить поток одного датчика
```bash
curl -N "http://localhost:8080/api/sensors/stream?sensorId=1&limit=5"
```

**Параметры:**
- `sensorId` (Long, опционально) - ID датчика для потока
- `limit` (Integer, опционально) - Максимальное количество элементов (по умолчанию 10)

**Ответ (NDJSON - Newline Delimited JSON):**
```json
{"sensor_id":1,"timestamp":1734447600000,"temperature":22.5,"humidity":55.0,"pressure":1013.2,"value":45.3,"anomaly":false}
{"sensor_id":1,"timestamp":1734447601000,"temperature":22.6,"humidity":55.1,"pressure":1013.3,"value":45.4,"anomaly":false}
{"sensor_id":1,"timestamp":1734447602000,"temperature":22.7,"humidity":55.2,"pressure":1013.4,"value":45.5,"anomaly":true}
```

#### Получить поток нескольких датчиков
```bash
curl -N "http://localhost:8080/api/sensors/stream/multi?sensorCount=3&limit=10"
```

**Параметры:**
- `sensorCount` (Integer, опционально) - Количество датчиков для потока (по умолчанию 5)
- `limit` (Integer, опционально) - Максимальное количество элементов на датчик (по умолчанию 20)

### Service A (Client) - Клиентский доступ к потокам

#### Получить поток через клиента (single sensor)
```bash
curl -N "http://localhost:8080/api/client/sensors?sensorId=1&limit=5"
```

#### Получить поток через клиента (multiple sensors)
```bash
curl -N "http://localhost:8080/api/client/sensors/multi?sensorCount=3&limit=10"
```

## Примеры логов

### Успешная инициализация приложения
```
2024-12-17 20:15:30.123 [main] INFO  com.sensordata.SensorReactiveApplication - Starting SensorReactiveApplication
2024-12-17 20:15:32.456 [main] INFO  com.sensordata.config.WebClientConfig - Creating WebClient with baseUrl=http://localhost:8080
2024-12-17 20:15:33.789 [main] INFO  o.s.b.w.e.netty.NettyWebServer - Netty started on port(s): 8080
```

### HTTP запрос через LoggingFilter
```
2024-12-17 20:15:35.123 [reactor-http-nio-2] INFO  com.sensordata.filter.LoggingFilter - >>> [REQUEST] method=GET, path=/api/sensors/stream, params={sensorId=[1], limit=[5]}
2024-12-17 20:15:40.456 [reactor-http-nio-2] INFO  com.sensordata.filter.LoggingFilter - <<< [RESPONSE] method=GET, path=/api/sensors/stream, status=200, duration=5123ms, signal=ON_COMPLETE
```

### Начало потока датчика
```
2024-12-17 20:15:35.234 [parallel-1] INFO  com.sensordata.service.SensorStreamService - Starting sensor stream for sensorId=1, limit=5
2024-12-17 20:15:35.345 [parallel-1] DEBUG com.sensordata.service.SensorStreamService - Emitting sensor data at tick=0
2024-12-17 20:15:35.456 [parallel-1] DEBUG com.sensordata.service.SensorClientService - Received sensor data: sensorId=1, value=45.3
```

### Завершение потока
```
2024-12-17 20:15:40.567 [parallel-1] INFO  com.sensordata.service.SensorStreamService - Sensor stream completed for sensorId=1
2024-12-17 20:15:40.678 [parallel-1] INFO  com.sensordata.service.SensorClientService - Sensor stream completed for sensorId=1
```

### Ошибка и обработка
```
2024-12-17 20:15:45.123 [reactor-http-nio-3] ERROR com.sensordata.service.SensorClientService - Error receiving sensor stream for sensorId=2: Connection refused
2024-12-17 20:15:45.234 [reactor-http-nio-3] WARN  com.sensordata.service.SensorClientService - Retrying sensor stream request, attempt=1
2024-12-17 20:15:46.345 [reactor-http-nio-3] WARN  com.sensordata.service.SensorClientService - Retrying sensor stream request, attempt=2
2024-12-17 20:15:47.456 [reactor-http-nio-3] ERROR com.sensordata.service.SensorClientService - Failed to fetch sensor stream after retries: Connection refused
```

**Жизненный цикл:**
1. **Subscription** - клиент подписывается на Flux
2. **Emission** - Flux начинает испускать элементы (в данном случае каждую секунду)
3. **Processing** - каждый элемент обрабатывается через операторы
4. **Completion** или **Error** или **Cancellation** - поток заканчивается одним из способов

## Неоптимальный код (будет оптимизирован в ЛР №3)

### Расположение
Основная неоптимальная логика находится в:
- **`src/main/java/com/sensordata/service/SensorDataGenerator.java`** - Главный класс с неоптимизированным кодом

## Переменные окружения

| Переменная | По умолчанию | Описание |
|-----------|-------------|---------|
| `DB_HOST` | localhost | Хост PostgreSQL |
| `DB_PORT` | 5432 | Порт PostgreSQL |
| `DB_NAME` | sensordb | Имя базы данных |
| `DB_USER` | postgres | Пользователь БД |
| `DB_PASSWORD` | postgres | Пароль БД |
| `SERVER_PORT` | 8080 | Порт приложения |
| `SENSOR_SERVER_URL` | http://localhost:8080 | URL сервера датчиков |

## Мониторинг и Управление

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

### Info
```bash
curl http://localhost:8080/actuator/info
```

### Очистить контейнеры и данные
```bash
make clean
```

### Проверить логи контейнера
```bash
make logs
```

## Тестирование

### Тест 1: Простой поток одного датчика
```bash
curl -N -X GET "http://localhost:8080/api/sensors/stream?sensorId=1&limit=3"
```

### Тест 2: Поток нескольких датчиков
```bash
curl -N -X GET "http://localhost:8080/api/sensors/stream/multi?sensorCount=3&limit=5"
```

### Тест 3: Клиентский доступ
```bash
curl -N -X GET "http://localhost:8080/api/client/sensors?sensorId=1&limit=3"
```

### Тест 4: Health check
```bash
curl -X GET "http://localhost:8080/actuator/health"
```

## Лицензия

MIT
