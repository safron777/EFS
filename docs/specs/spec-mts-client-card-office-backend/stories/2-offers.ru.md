---
title: 'История 2 — CAP-2: Топ-предложение'
type: 'feature'
created: '2026-09-24'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<!-- Русская версия — перевод 2-offers.md для удобства чтения; при расхождении английский оригинал остаётся источником истины и единственным файлом, который читает воркфлоу bmad-build. -->

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Суть (Intent)

**Проблема:** `offers-service` (История 0) имеет только health-эндпоинт. CAP-2 нужно обслужить вызов `getOffer(clientId)` фронтенда — но, в отличие от `client-identity-service` из Истории 1, роль этого сервиса по AD-1 — чистый фасад над внешним offer/маркетинговым движком банка (AD-10), а не владелец данных.

**Подход:** Реализовать `GET /clients/{clientId}/offers/top` как тонкий hexagonal-срез — доменный value object `Offer`, порт `OfferEnginePort`, и stub-адаптер, возвращающий фикстуру из `mockData.offer` — вообще без схемы БД. `offers-service-postgres` (единообразно выделен в Истории 0) остаётся неиспользуемым; это единственный сервис, который эта история сознательно оставляет без схемы, по самой формулировке фасада в AD-10.

## Границы и ограничения (Boundaries & Constraints)

**Всегда:**
- Ответ зеркалит `mockData.offer` точно (`title`, `text`, `moreCount`) на `GET /clients/{clientId}/offers/top` — AD-8.
- Реализовано исключительно через `OfferEnginePort` (домен) + stub-адаптер (`adapters/out/external/offerengine/`) по AD-10 — никакого JPA, никакого Liquibase, никакой персистентности для этой способности.
- `clientId` принимается как есть, никогда не проверяется против `client-identity-service` (AD-9 — никаких синхронных межсервисных вызовов).

**Никогда:**
- Никаких write/мутационных эндпоинтов.
- Никакого реального протокола offer-движка — только stub-фикстура (Deferred в спайне).
- Никакой схемы БД — единственное сознательное исключение из паттерна Liquibase Истории 1, потому что этот сервис не владеет собственными данными.

## Матрица ввода-вывода и краевых случаев (I/O & Edge-Case Matrix)

| Сценарий | Вход / состояние | Ожидаемый результат | Обработка ошибок |
|----------|--------------|---------------------------|----------------|
| Любой клиент | `GET /clients/client-1/offers/top` | `200`, тело точно совпадает с `mockData.offer` | N/A |
| Другой клиент | `GET /clients/client-2/offers/top` | `200`, та же фикстура, что и выше (пока не персонализировано — совпадает с сегодняшним моком, который безусловен) | N/A |

</frozen-after-approval>

## Карта кода (Code Map)

- `ARCHITECTURE-SPINE.md` AD-1 — `offers` — фасад над stub-портом к внешнему offer-движку, не владелец данных
- `ARCHITECTURE-SPINE.md` AD-10 — паттерн stub-порта (`OfferEnginePort` в domain/application, stub-адаптер в `adapters/out/external/`)
- `ARCHITECTURE-SPINE.md` AD-8 — REST-форма ресурса зеркалит `client.js`
- `ARCHITECTURE-SPINE.md` AD-9 — никаких синхронных межсервисных вызовов
- Экспорт `offer` из `src/api/mockData.js` — точная форма и значения фикстуры
- `getOffer(clientId)` из `src/api/client.js` — `/clients/{clientId}/offers/top`
- `backend/offers-service/` (История 0) — существующие `pom.xml`, `application.yml`, hexagonal-скелет пакетов, включая уже созданный пустым `adapters/out/external/offerengine/`
- `backend/gateway/src/main/resources/application.yml` — существующий маршрут `/offers/**` уже проксирует сюда; изменений в gateway не требуется
- `backend/client-identity-service/` (История 1) — соседний прецедент по форме контроллера/exception-handler'а и словарю конверта `NOT_FOUND`/`INTERNAL_ERROR` (здесь не переиспользуется, поскольку у этой способности нет пути отказа, но по форме код должен выглядеть как сосед, а не одноразовая поделка)

## Задачи и приёмка (Tasks & Acceptance)

**Выполнение:**
- [x] `.../domain/Offer.java` -- framework-free value object (`title`, `text`, `moreCount`)
- [x] `.../domain/OfferEnginePort.java` -- интерфейс-порт, `Offer getTopOffer(String clientId)`
- [x] `.../application/GetTopOfferUseCase.java` -- оркестрирует порт, без ветвления сверх самого вызова (пути «не найдено» для этой способности не существует)
- [x] `.../adapters/out/external/offerengine/StubOfferEngineAdapter.java` -- реализует `OfferEnginePort`, возвращает фиксированную фикстуру `mockData.offer` независимо от `clientId`
- [x] `.../adapters/in/web/OfferController.java` + DTO ответа (`title`, `text`, `moreCount`) -- `GET /clients/{clientId}/offers/top`

**Критерии приёмки:**
- [x] Дан stub-адаптер, когда вызывается `GET /clients/client-1/offers/top` напрямую на сервисе, тогда ответ `200` и точно совпадает с `mockData.offer`.
- [x] Дан другой `clientId`, когда вызывается `GET /clients/client-2/offers/top`, тогда возвращается та же неизменная фикстура.
- [x] Дан поднятый через `docker compose up -d --wait` стек (compose-файл без изменений), когда вызывается `GET http://localhost:8080/offers/clients/client-1/offers/top` через Gateway, тогда приходит тот же `200`.

## Заметки по реализации (Implementation Notes)

- `offers-service/pom.xml` уже несёт `spring-boot-starter-data-jpa` + `postgresql` из единообразного выделения Историей 0; оставлено без изменений, поскольку удаление не запрашивалось, а datasource на всё ещё выделенный (но неиспользуемый) `offers-service-postgres` продолжает стартовать чисто без единой JPA-сущности.
- **Патч после ревью — добавлен `OfferExceptionHandler` + `ErrorResponse`** (`adapters/in/web/`): минимальный `@RestControllerAdvice` с единственным `@ExceptionHandler(Exception.class)` → `500` + `{"error":{"code":"INTERNAL_ERROR",...}}`, по форме и словарю совпадает с `client-identity-service`. Без маппинга `NOT_FOUND` — для этой способности такого случая не существует.
- **Патч после ревью — добавлен `OfferControllerIntegrationTest`** (`@SpringBootTest(webEnvironment = RANDOM_PORT)`): поднимает реальное DI-приложение и обращается к `GET /clients/{clientId}/offers/top` через `WebTestClient`, проверяя точную фикстуру, включая кириллицу. `spring.autoconfigure.exclude` отключает `DataSourceAutoConfiguration`/`HibernateJpaAutoConfiguration` только для этого теста, чтобы не требовался реальный Postgres. `spring-boot-starter-webflux` (test-scope) добавлен в `pom.xml` по тому же прецеденту, что и в `client-identity-service/pom.xml`.
- **Патч после ревью — `Offer` теперь отклоняет отрицательный `moreCount`** (`IllegalArgumentException`), по аналогии с существующими проверками `title`/`text`.
- **Обнаружено при независимой перепроверке после патчей ревью, не ревьюером:** новый catch-all `@ExceptionHandler(Exception.class)` не видел собственные фреймворковые исключения Spring — обычный `POST` на этот `GET`-only эндпоинт возвращал `500 INTERNAL_ERROR` вместо корректного `405`. Исправлено: `OfferExceptionHandler` теперь наследует `ResponseEntityExceptionHandler` и переопределяет `handleExceptionInternal`, переформатируя тело в конверт спайна, сохраняя статус, который уже определил Spring; общий обработчик `Exception.class` теперь ловит только то, что Spring MVC не распознаёт сам. **Идентичный баг был в `client-identity-service` (История 1, уже смержена)** — тот же класс, скопированный этой историей, установил ровно такую же форму bare-catch-all — исправлено там же в рамках цикла ревью этой истории, поскольку причина одна и та же.
- **Второй ретроактивный фикс, из Истории 3 (card-account):** `handleExceptionInternal` выше всё ещё пересчитывал `HttpStatus` через `HttpStatus.valueOf(statusCode.value())` только чтобы вызвать `.is4xxClientError()` — избыточно, и кидает `IllegalArgumentException` на нестандартном коде (что само рухнуло бы в необработанный `500`); `exception.getMessage()` также не имел fallback на null. Исправлено: `statusCode.is4xxClientError()`/`.is5xxServerError()` напрямую с явной веткой `else`, сообщение по умолчанию при null — применено одинаково здесь, в `client-identity-service` и в `card-account-service` (получил исправленную форму сразу). Перепроверено: happy-path не затронут.

## Журнал изменений спеки (Spec Change Log)

## Журнал триажа ревью (Review Triage Log)

## Заметки по дизайну (Design Notes)

Путь через Gateway — `/offers/clients/{clientId}/offers/top` — «offers» встречается дважды: один раз как маршрутизирующий префикс Gateway по имени сервиса (обрезается перед пересылкой, установлено в Истории 0), второй раз как часть самого ресурсного пути, зеркалящего `client.js` (AD-8). Стилистически избыточно, но не дефект — это тот же паттерн, что уже установила История 1 (`/client-identity/clients/{id}`), и менять его означало бы отступить либо от буквального зеркалирования пути по AD-8, либо от уже зафиксированной конвенции роутинга Gateway из Истории 0 — ни то, ни другое эта история не должна пересматривать в одиночку.

## Проверка (Verification)

**Команды:**
- `docker compose -f backend/docker-compose.yml build offers-service` -- ожидается: образ собирается
  - **Результат: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- ожидается: `offers-service` отчитывается healthy (миграций нет; его контейнер Postgres остаётся поднятым, но неиспользуемым)
  - **Результат: PASS.** Весь стек из 20 контейнеров (gateway + 9 сервисов + 9 postgres + kafka) healthy, включая `offers-service` и `offers-service-postgres`.
- `curl -s http://localhost:8080/offers/clients/client-1/offers/top` -- ожидается: `200`, тело совпадает с `mockData.offer`
  - **Результат: PASS.** `{"title":"MTS CASHBACK 111 дней","text":"Повышенный кэшбэк на 111 дней при активации подписки","moreCount":1}`, HTTP 200, точное совпадение включая кириллицу.
- `curl -s http://localhost:8080/offers/clients/client-2/offers/top` -- ожидается: `200`, идентичное тело
  - **Результат: PASS.** Тело побайтово идентично `client-1`.
- Набор тестов модуля -- ожидается: все проходят
  - **Результат: PASS.** `mvn -pl offers-service -am test`: `Tests run: 6, Failures: 0, Errors: 0` (включая новый `OfferControllerIntegrationTest`, добавленный после ревью). Стек остановлен после проверки командой `docker compose down`.
