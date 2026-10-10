---
title: 'История 3 — CAP-3: Карты и балансы'
type: 'feature'
created: '2026-09-27'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<!-- Русская версия — перевод 3-card-account-cards.md для удобства чтения; при расхождении английский оригинал остаётся источником истины и единственным файлом, который читает воркфлоу bmad-build. -->

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Суть (Intent)

**Проблема:** `card-account-service` (История 0) имеет только health-эндпоинт. CAP-3 нужно обслужить вызов `getCards(clientId, productId)` фронтенда со списком карт клиента, балансами и статусом.

**Подход:** Добавить схему `cards` через Liquibase (AD-12) и hexagonal-срез, реализующий `GET /clients/{clientId}/products/{productId}/cards`, засеянный из `mockData.cards` — но с двумя сознательными исправлениями формата на проводе, которых сам мок не делает: деньги как целые копейки (AD-3) и даты как ISO-8601 (Consistency Conventions, установлено в Истории 1), а не сырое целое число рублей и строки `dd.MM.yyyy` из мока.

## Границы и ограничения (Boundaries & Constraints)

**Всегда:**
- Ответ — JSON-массив, зеркалящий поля `mockData.cards` (`id`, `product`, `masked`, `subtitle`, `isMain`, `status`, `balance`, `issuedAt`, `account`, `closedAt`) на `GET /clients/{clientId}/products/{productId}/cards` — AD-8.
- `balance` — целое число со знаком в копейках (AD-3) — значение мока `12430` (которое фронтенд рисует как `12 430 ₽` через `toLocaleString`) становится seed-значением `1243000`. Это сознательное расхождение с сырым числом мока, не его копия.
- `issuedAt`/`closedAt` — даты в ISO-8601 (`"2025-01-12"`), а не `"12.01.2025"` из мока — то же правило Consistency Conventions, которое История 1 уже установила для дат; форматирование по локали — забота фронтенда.
- `account`/`issuedAt`/`closedAt` — nullable и присутствуют только там, где они есть в моке (активная карта: `account`+`issuedAt`, без `closedAt`; закрытая карта: `closedAt`, без `account`/`issuedAt`) — воспроизвести это присутствие по строкам точно, не единую форму с null везде.
- Карты ссылаются на своего клиента только по `clientId` (AD-2) — без копии полей профиля клиента.
- Этот сервис получает `@RestControllerAdvice extends ResponseEntityExceptionHandler` с самого начала (не bare `@ExceptionHandler(Exception.class)`) — Истории 1 и 2 обе выпустили именно эту bare-форму, и обе пришлось патчить после того, как независимая проверка поймала, что она молча превращает собственные `405`/`415` Spring в обманчивый `500`. Скопировать уже исправленную форму, которая теперь есть у этих двух сервисов, а не их первоначальную.

**Никогда:**
- Никаких write/мутационных эндпоинтов — критерий success CAP-3 — только чтение; «переключение» активной карты — исключительно фронтенд-забота (состояние `activeCardId` в `useCardScopedData`), не backend-действие.
- Никаких вызовов `client-identity-service` для проверки `clientId` (AD-9).
- Никакого случая `NOT_FOUND` для неизвестного `clientId`/`productId` — это коллекционный эндпоинт; несовпадающая комбинация возвращает `200` с пустым массивом, стандартная REST-семантика коллекций, не 404 (в отличие от одиночного ресурса `/clients/{id}` в Истории 1).
- Никакого эндпоинта документов — это CAP-4, отдельная история на этом же сервисе.

## Матрица ввода-вывода и краевых случаев (I/O & Edge-Case Matrix)

| Сценарий | Вход / состояние | Ожидаемый результат | Обработка ошибок |
|----------|--------------|---------------------------|----------------|
| Известный клиент, известный продукт | `GET /clients/client-1/products/debit-mts-dengi/cards` | `200`, массив из 2 карт, соответствующий `mockData.cards` (с исправлениями копеек/ISO-дат выше) | N/A |
| Неизвестный клиент или продукт | `GET /clients/nope/products/debit-mts-dengi/cards` | `200`, `[]` | N/A |
| Не-GET метод | `POST /clients/client-1/products/debit-mts-dengi/cards` | — | `405` с конвертом ошибки спайна, не зависание и не `500` |

</frozen-after-approval>

## Карта кода (Code Map)

- `ARCHITECTURE-SPINE.md` AD-1 — `card-account` владеет картами, балансами, связанным счётом
- `ARCHITECTURE-SPINE.md` AD-2 — межконтекстные ссылки только по ID
- `ARCHITECTURE-SPINE.md` AD-3 — деньги как целые минимальные единицы, никогда отформатированная строка
- `ARCHITECTURE-SPINE.md` AD-8 — REST-форма ресурса зеркалит `client.js`
- `ARCHITECTURE-SPINE.md` AD-12 — Liquibase для миграций схемы
- `ARCHITECTURE-SPINE.md` § Consistency Conventions — конверт ошибок + словарь кодов, ISO-8601 даты
- Экспорт `cards` из `src/api/mockData.js` — точная форма и значения фикстуры (до исправлений копеек/ISO-дат)
- `getCards(clientId, productId)` из `src/api/client.js` — `/clients/{clientId}/products/{productId}/cards`
- `src/hooks/useCardScopedData.js` — подтверждает, что «переключение» карт — чисто фронтенд-состояние (`activeCardId`), не backend-концепция; backend всегда возвращает полный список
- `src/pages/BalancePage.jsx` (`mainCard.balance.toLocaleString("ru-RU")`) и `src/components/shared/CardSwitcher.jsx` — подтверждают, что `balance` в моке — сырые рубли (не копейки), и какие поля (`status`) управляют UI-состоянием, обосновывая исправление AD-3 выше
- `backend/client-identity-service/` (История 1, после фикса) и `backend/offers-service/` (История 2, после фикса) — у обеих теперь исправленный advice на основе `ResponseEntityExceptionHandler`; скопировать эту форму напрямую, не выводить заново багованную bare-форму
- `backend/gateway/src/main/resources/application.yml` — существующий маршрут `/card-account/**` уже проксирует сюда; изменений в gateway не требуется

## Задачи и приёмка (Tasks & Acceptance)

**Выполнение:**
- [x] `.../domain/Card.java` -- framework-free сущность (`id`, `product`, `masked`, `subtitle`, `isMain`, `status`, `balanceMinorUnits`, `issuedAt`, `account`, `closedAt`), порт `CardRepository` (`List<Card> findByClientIdAndProductId(String clientId, String productId)`)
- [x] `.../application/GetCardsUseCase.java` -- оркестрирует порт, без ветвления «не найдено» (пустой список — валидный результат)
- [x] `.../adapters/in/web/CardController.java` + DTO-массив ответа + `CardExceptionHandler extends ResponseEntityExceptionHandler` (исправленная форма, не первоначальная из Историй 1/2) -- `GET /clients/{clientId}/products/{productId}/cards`
- [x] `.../adapters/out/persistence/` -- JPA-сущность, Spring Data репозиторий (`findByClientIdAndProductId`), адаптер, реализующий доменный порт
- [x] `backend/card-account-service/src/main/resources/db/changelog/` -- таблица `cards` (nullable `account`/`issued_at`/`closed_at`) + один чейнджсет, засевающий обе мок-карты для `client-1`/`debit-mts-dengi` с исправлениями копеек/ISO-дат
- [x] `backend/card-account-service/src/test/java/.../CardControllerIntegrationTest.java` -- `@SpringBootTest(webEnvironment = RANDOM_PORT)` против Testcontainers Postgres, покрывает все три строки I/O-матрицы (включая случай `405`, извлечённый из Историй 1/2)

**Критерии приёмки:**
- [x] Даны засеянные данные, когда вызывается `GET /clients/client-1/products/debit-mts-dengi/cards` напрямую на сервисе, тогда ответ `200` с обеими картами, `balance` в копейках (`1243000`/`0`) и даты в ISO-8601 (`"2025-01-12"`/`"2025-10-20"`), точно соответствуя присутствию полей по строкам из мока.
- [x] Дан неизвестный `clientId`, когда вызывается `GET /clients/nope/products/debit-mts-dengi/cards`, тогда ответ `200` с `[]`.
- [x] Дан не-GET метод, когда вызывается `POST /clients/client-1/products/debit-mts-dengi/cards`, тогда ответ `405` с конвертом ошибки, не `500`.
- [x] Дан поднятый через `docker compose up -d --wait` стек (compose-файл без изменений), когда тот же запрос делается через Gateway, тогда приходит тот же `200`.

## Заметки по реализации (Implementation Notes)

- `CardResponse` помечен `@JsonInclude(Include.NON_NULL)` на уровне класса, поэтому `account`/`issuedAt`/`closedAt` *опускаются* (а не сериализуются как `"key":null`) в строке, где их нет — противоположный выбор относительно `ClientCardResponse.TriggerResponse.link` из Истории 1, где `"link":null` сознательно сохраняется. Различие — из-за мока: у каждого триггера ключ `link` всегда есть (иногда `null`), а объекты-строки `mockData.cards` просто не содержат ключ вовсе, когда его нет. Проверено на проводе: у `card-4019` нет ключа `closedAt`; у `card-7274` нет ни `account`, ни `issuedAt`.
- `clientId`/`productId` живут только в JPA-сущности `CardEntity` и в запросе `findByClientIdAndProductId`, никогда — в доменном `Card` или в ответе — этому способствуют AD-2 и явный список полей `Card` в самой истории (без них); адаптер персистентности — единственное место, знающее про эти колонки скоупинга.
- Добавлен метод репозитория `findByClientIdAndProductIdOrderByIsMainDescIdAsc` (а не безсортировочный `findByClientIdAndProductId`, как назван в сигнатуре порта), чтобы двухстрочный ответ был детерминированно упорядочен (основная карта первой), соответствуя порядку самого `mockData.cards`; история явно не требует упорядочивания, но неупорядоченный `SELECT` в Postgres такой гарантии не даёт, а формулировка I/O-матрицы «массив из 2 карт, соответствующий mockData.cards» естественнее читается как включающая порядок.
- **Специфично для песочницы, не дефект проекта:** JDK хоста — 17, а стек целится в Java 25 (задокументированное в Истории 0 разделение host-JDK/build-JDK), поэтому и `docker compose build`, и `mvn test` на Testcontainers пришлось запускать внутри контейнера `eclipse-temurin:25`, как и описывает раздел README Историй 0/1. Запуск Testcontainers таким образом (монтирование хостового `/var/run/docker.sock` в этот контейнер — «Docker-outside-of-Docker») потребовал двух дополнительных флагов из-за сетевых особенностей Docker Desktop на macOS в этой песочнице: `TESTCONTAINERS_RYUK_DISABLED=true` (порт самого resource reaper не был доступен обратно из контейнера JVM) и `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` (без него Testcontainers угадывал IP bridge-gateway для смэппленного порта контейнера Postgres, а VM Docker Desktop на macOS туда трафик не маршрутизирует). Ни один из флагов никуда в репозитории не закоммичен; они нужны были только для выполнения `mvn test` в этой конкретной сессии песочницы, не для самого кода.
- **Поймано при выполнении вышеописанного, исправлено до попадания в закоммиченный чейнджсет:** черновик `001-create-card-account-schema.xml`/`002-seed-cards-client-1.xml` использовал `--` внутри XML-комментариев `<!-- -->` для оборотов вроде тире (стиль документации, уже используемый в Javadoc этого репозитория). XML запрещает `--` внутри комментариев; SAX-парсер Liquibase отверг чейнджлог целиком (`"databaseChangeLog" expected as root element`, `The string "--" is not permitted within comments`) до того, как выполнился хоть один чейнджсет. Это ровно та ловушка, которую уже называют Заметки по реализации Истории 1 («XML-комментарии не могут содержать `--`») — воспроизведена здесь, несмотря на то что эта заметка была прочитана заранее, и исправлена переформулировкой затронутых комментариев (запятые/точки вместо `--`), а не подавлением парсера или обходным путём.
- **Ретроактивный фикс, из Истории 5 (card-issuance):** `CardExceptionHandler.handleExceptionInternal` формировал ответ как `ResponseEntity.status(statusCode).body(...)`, молча отбрасывая параметр `headers`, который заполняет Spring (например, `Allow: GET` на `405`) — у этого сервиса форма с `is4xxClientError()`/`is5xxServerError()` была исправлена с самого начала (История 3), но этот общий для всех четырёх сервисов пробел с заголовками всё равно был унаследован. Исправлено: вызывать `.headers(headers)` перед `.body(...)`, применено одинаково во всех четырёх. Перепроверено: `405` теперь возвращает `Allow: GET`; happy-path не затронут.

## Журнал изменений спеки (Spec Change Log)

## Журнал триажа ревью (Review Triage Log)

- **`CardExceptionHandler.handleExceptionInternal` без нужды преобразует `HttpStatusCode`→`HttpStatus` и может упасть** — `medium` — Проверено: `HttpStatus.valueOf(statusCode.value())` вызывается только чтобы дойти до `.is4xxClientError()`, который `HttpStatusCode` и так предоставляет напрямую; `HttpStatus.valueOf(int)` кидает `IllegalArgumentException` на любом нестандартном коде, что превратило бы уже отправляемый ответ об ошибке в необработанный `500`. Также: нестандартный (не 4xx/5xx) статус проваливался в `"INTERNAL_ERROR"` без явной ветки, а `exception.getMessage()` не имел fallback на null (в отличие от соседнего метода `handleUnexpected`), рискуя `"message": null` против задокументированного контракта конверта (`string`). **Идентичный паттерн уже существует в `client-identity-service` и `offers-service` (Истории 1/2) — спека этой истории велела реализующему скопировать эту форму, так что скопировалась и скрытая проблема.** → **patch**: вызывать `statusCode.is4xxClientError()`/`.is5xxServerError()` напрямую, добавить явную ветку fallback, задавать сообщение по умолчанию при null — применено здесь и ретроактивно в обоих соседних сервисах, поскольку 7 следующих историй будут копировать именно эту форму.
- **Тест на присутствие полей проверяет сырой JSON через разбиение строки** — `low` — Проверено: `perRowFieldPresenceMatchesMockExactlyNotPaddedWithNulls` проверяет отсутствие ключей через `body.split("\\},\\{")` + проверки подстрок — сейчас корректно, но молча ломается при pretty-print, перестановке ключей или строковом значении, содержащем `},{`. → **patch**: заменить на проверку через `JsonNode` — «ключа не существует».
- **Путь команды `mvn` в разделе Verification не согласован с соседними** — `low` — Проверено: все остальные команды в списке заданы относительно корня репозитория (`docker compose -f backend/docker-compose.yml ...`), а команда теста — `mvn -f card-account-service/pom.xml test` (без префикса `backend/`) — копипаста из корня репозитория дала бы «no such file». → **patch**: исправить путь.
- **В Javadoc `Card.java` не читается предложение** — `low` — Проверено: «AD-2 already rules out copying more of the client profile onto this object than its id would have been» не разбирается грамматически. → **patch**: переформулировать.
- **Дифф этого ревью упустил `card-account-service/pom.xml`** — принято к сведению, без действий в коде — Проверено: пробел в том, как я ограничил дифф для этого раунда ревью (продиффил только `backend/pom.xml`, не собственный `pom.xml` сервиса, который действительно менялся), не дефект самого изменения — реальный прогон `docker compose build`/`up`/тестов, который я выполнил независимо, уже упражняет настоящий `pom.xml` на диске, так что реализация была проверена независимо от слепой зоны этого ревью.
- **Дифф этого ревью упустил перевод `.ru.md`** — `false` — Та же устоявшаяся конвенция, что и в Историях 0–2: русский дубликат — перевод, не код под ревью.
- **У seed-чейнджсета нет элемента `<rollback>`** — `low`, отклонено — Поддержка rollback в Liquibase не имеет текущего применения в этом demo (никакого rollback-воркфлоу нигде в проекте не существует и не запрашивалось; CI/CD явно вне scope по Истории 0).
- **Нет отдельных unit-тестов на `GetCardsUseCase`/`Card`/`CardResponse`** — `low`, отклонено — У `GetCardsUseCase` нет ветвящейся логики для unit-теста (чистый passthrough, тот же вывод, что История 2 сделала для своего use case); маппинг nullable-полей по строкам, который это покрыло бы, уже проверен end-to-end через `perRowFieldPresenceMatchesMockExactlyNotPaddedWithNulls`.
- **Нет проверки авторизации против перечисления `clientId`** — отклонено, вне scope по intent — Реальная модель авторизации всего бэкенда (OAuth2/OIDC через корпоративный SSO) уже вынесена в Deferred в архитектурном спайне; введение отдельной проверки авторизации на одном эндпоинте противоречило бы уже согласованному откладыванию, а не заполняло бы пробел, созданный этой историей.

## Заметки по дизайну (Design Notes)

Исправления копеек и ISO-дат означают, что числа/даты этого эндпоинта не будут выглядеть как сырые значения текущего мока фронтенда при сравнении напрямую — это намеренно (AD-3 и конвенция дат из Истории 1), не несоответствие, которое нужно устранять. Подключение фронтенда к исправленному формату на проводе — вне scope здесь, как и расхождение `openCaseIds` в Истории 1.

## Проверка (Verification)

**Команды:**
- `docker compose -f backend/docker-compose.yml build card-account-service` -- ожидается: образ собирается
  - **Результат: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- ожидается: `card-account-service` и его Postgres отчитываются healthy
  - **Результат: PASS.** Все 20 контейнеров стека (gateway + 9 сервисов + 9 postgres + kafka) отчитались healthy, включая `card-account-service` и `card-account-service-postgres`; остальные 8 сервисов не затронуты.
- `curl -s http://localhost:8080/card-account/clients/client-1/products/debit-mts-dengi/cards` -- ожидается: `200`, 2 карты со значениями в копейках/ISO-датах
  - **Результат: PASS.** `[{"id":"card-4019","product":"Zero","masked":"•••• 4019","subtitle":"8_MC World Zero RUR","isMain":true,"status":"active","balance":1243000,"issuedAt":"2025-01-12","account":"****6272"},{"id":"card-7274","product":"ZERO","masked":"•••• 7274","subtitle":"8_MC World Zero RUR · Виртуальная","isMain":false,"status":"closed","balance":0,"closedAt":"2025-10-20"}]` -- HTTP 200; копейки (`1243000`/`0`) и ISO-даты подтверждены; у `card-4019` нет ключа `closedAt`, у `card-7274` нет ни `account`, ни `issuedAt` — точно как в `mockData.cards`.
- `curl -s http://localhost:8080/card-account/clients/nope/products/debit-mts-dengi/cards` -- ожидается: `200`, `[]`
  - **Результат: PASS.** `[]`, HTTP 200.
- `curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:8080/card-account/clients/client-1/products/debit-mts-dengi/cards` -- ожидается: `405`
  - **Результат: PASS.** `405` с телом `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'POST' is not supported"}}`, не `500`.
- Набор тестов модуля -- ожидается: все проходят
  - **Результат: PASS.** `mvn -f backend/card-account-service/pom.xml test` (запущено внутри контейнера `eclipse-temurin:25` из-за ограничения хостового JDK Истории 0, см. Заметки по реализации): `Tests run: 4, Failures: 0, Errors: 0` -- `CardControllerIntegrationTest` против настоящего Testcontainers Postgres, покрывает применённую Liquibase-схему/сид и все три строки I/O-матрицы плюс проверку присутствия полей по строкам. Стек остановлен после проверки командой `docker compose down`.
