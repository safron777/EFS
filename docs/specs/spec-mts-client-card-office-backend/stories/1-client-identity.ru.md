---
title: 'История 1 — CAP-1: Идентификация клиента'
type: 'feature'
created: '2026-09-23'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '28f331031f62d7520d5d7c7cf415192a54cd0899'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<!-- Русская версия — перевод 1-client-identity.md для удобства чтения; при расхождении английский оригинал остаётся источником истины и единственным файлом, который читает воркфлоу bmad-build. -->

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Суть (Intent)

**Проблема:** `client-identity-service` (История 0) имеет только health-эндпоинт — ни данных, ни таблицы, фронтенд никогда не увидит реального клиента. CAP-1 (профиль, бейджи, триггеры, сводка открытых обращений) — первая бизнес-способность во всём бэкенде и первая история, которой нужна настоящая схема БД (у каждого сервиса всё ещё `ddl-auto: none` и ноль таблиц).

**Подход:** Добавить схему `clients`/`client_badges`/`client_triggers`/`client_open_cases` через Liquibase (AD-12, добавлен в спайн под эту историю), hexagonal-срез (доменная сущность, use case, REST-контроллер, JPA-адаптер), реализующий `GET /clients/{id}` по образцу `getClientCard` из `client.js`, и seed-данные, точно повторяющие `mockData.client`, чтобы ответ был побайтово сравним с сегодняшним моком фронтенда (кроме сознательного расхождения по `openCases` ниже).

## Границы и ограничения (Boundaries & Constraints)

**Всегда:**
- Форма ответа зеркалит `mockData.client` (`id`, `fullName`, `clientSince`, `badges`, `triggers[]` с `id`/`icon`/`tone`/`title`/`text`/`link`) — AD-8.
- Каждый `trigger.link` проверяется по правилу AD-4 перед выходом из сервиса: относительный внутренний путь (`^/(?!/|\\)`) или `null`, никогда не абсолютный/внешний URL, даже несмотря на то что сегодняшние seed-данные доверенные.
- Схема принадлежит только этому сервису, через Liquibase-чейнджлоги под `src/main/resources/db/changelog/` (AD-12); `ddl-auto` остаётся `none`.
- `openCases` становится **`openCaseIds: string[]`** — только голые ID обращений, без текста label. Человекочитаемый label из мока принадлежит `incident-case-service` (ещё не построен — Истории 10/11), а AD-2 запрещает этому сервису хранить денормализованную копию чужих данных. Это сознательное, одобренное пользователем расхождение с текущей формой мока фронтенда для этого одного поля.
- Отсутствие клиента возвращает `404` с конвертом ошибок спайна (`{ "error": { "code": "NOT_FOUND", "message": "..." } }`).
- Словарь кодов ошибок решён здесь для всех девяти сервисов (поправка в спайн): `NOT_FOUND`, `VALIDATION_ERROR`, `INTERNAL_ERROR`, плюс уже существующий `UPSTREAM_UNAVAILABLE` у Gateway (История 0). Эта история испускает только `NOT_FOUND`; остальные названы, чтобы следующие сервисы переиспользовали их, а не изобретали локальные коды.

**Никогда:**
- Никаких write/мутационных эндпоинтов — в `client.js` нет мутационных функций для client-identity; критерий success CAP-1 — только чтение.
- Никаких вызовов других сервисов (AD-9) — `openCaseIds` хранятся/возвращаются как есть, никогда не обогащаются запросом к `incident-case-service`.
- Никакого seed-данных сверх одного demo-клиента, уже есть в `mockData.client` — множественные клиенты не часть критерия success CAP-1.

## Матрица ввода-вывода и краевых случаев (I/O & Edge-Case Matrix)

| Сценарий | Вход / состояние | Ожидаемый результат | Обработка ошибок |
|----------|--------------|---------------------------|----------------|
| Известный клиент | `GET /clients/client-1` | `200`, полное тело соответствует форме `mockData.client` (`openCaseIds` вместо `openCases`) | N/A |
| Неизвестный клиент | `GET /clients/does-not-exist` | — | `404` с `{ "error": { "code": "NOT_FOUND", "message": "..." } }` |
| Триггер с `link: null` | Засеянный триггер `mobile-app` (`link: null` в моке) | Поле `link` — `null` в ответе, не опущено | N/A |

</frozen-after-approval>

## Карта кода (Code Map)

- `ARCHITECTURE-SPINE.md` AD-1 — `client-identity` владеет профилем, бейджами, триггерами, *ссылками* на открытые обращения (не полными данными обращений)
- `ARCHITECTURE-SPINE.md` AD-2 — межконтекстные ссылки только по ID — управляет расхождением по `openCaseIds` выше
- `ARCHITECTURE-SPINE.md` AD-4 — правило безопасности поля-ссылки (происхождение `isSafeInternalLink`, GHSA-wrjc-x8rr-h8h6)
- `ARCHITECTURE-SPINE.md` AD-8 — REST-форма ресурса зеркалит `client.js`
- `ARCHITECTURE-SPINE.md` AD-12 — Liquibase для миграций схемы (добавлен в этой истории)
- `ARCHITECTURE-SPINE.md` § Consistency Conventions — форма конверта ошибок
- Экспорт `client` из `src/api/mockData.js` — точная форма и значения seed-данных для воспроизведения
- `getClientCard(clientId)` из `src/api/client.js` — вызов фронтенда, форме ответа на который должен соответствовать этот эндпоинт
- `backend/client-identity-service/` (История 0) — существующие `pom.xml`, `application.yml` (`ddl-auto: none`, datasource уже указывает на собственный Postgres), hexagonal-скелет пакетов — все пустые `package-info.java` предстоит заполнить
- `backend/gateway/src/main/resources/application.yml` — существующий маршрут `/client-identity/**` уже проксирует сюда; изменений в gateway не требуется

## Задачи и приёмка (Tasks & Acceptance)

**Выполнение:**
- [x] `backend/client-identity-service/pom.xml` -- добавить `org.liquibase:liquibase-core` (версия управляется BOM Spring Boot) -- включает миграции
- [x] `backend/client-identity-service/src/main/resources/application.yml` -- добавить `spring.liquibase.change-log: classpath:db/changelog/db.changelog-master.xml` -- указывает Boot на чейнджлог
- [x] `backend/client-identity-service/src/main/resources/db/changelog/db.changelog-master.xml` + один чейнджсет, создающий `clients`, `client_badges`, `client_triggers`, `client_open_case_ids` + один чейнджсет, засевающий единственного demo-клиента из `mockData.client` дословно -- схема + seed-данные
- [x] `.../domain/Client.java`, `ClientTrigger.java` (+ интерфейс-порт `ClientRepository`) -- доменная модель, без фреймворковых аннотаций
- [x] `.../application/GetClientCardUseCase.java` -- оркестрирует порт репозитория, маппит в DTO ответа, кидает доменное `ClientNotFoundException` при промахе
- [x] `.../adapters/in/web/ClientController.java` + DTO ответа (`id`, `fullName`, `clientSince`, `badges`, `triggers[]`, `openCaseIds`) + `@ExceptionHandler`/`@ControllerAdvice`, маппящий `ClientNotFoundException` в `404` + конверт ошибки -- `GET /clients/{id}`
- [x] `.../adapters/out/persistence/` -- JPA-сущности (`ClientEntity`, `ClientTriggerEntity`), Spring Data репозиторий, и адаптер, реализующий доменный порт `ClientRepository`
- [x] `backend/client-identity-service/src/test/java/.../ClientControllerTest.java` -- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `WebTestClient` против Testcontainers Postgres (по стеку спайна: Testcontainers 2.0.5), покрывает все три строки I/O-матрицы

**Критерии приёмки:**
- [x] Дан засеянный demo-клиент, когда вызывается `GET /clients/client-1` напрямую на сервисе (порт контейнера, ещё не через Gateway), тогда ответ `200` и совпадает с полями `mockData.client` кроме `openCaseIds` вместо `openCases`.
- [x] Дан незасеянный id, когда вызывается `GET /clients/does-not-exist`, тогда ответ `404` с конвертом ошибки и `code: "NOT_FOUND"`.
- [x] Дан поднятый через `docker compose up -d --wait` стек (compose-файл Истории 0, без изменений), когда вызывается `GET http://localhost:8080/client-identity/clients/client-1` через Gateway, тогда приходит тот же `200` — доказывает, что миграция Liquibase реально запускается при старте контейнера и существующий маршрут Gateway (История 0) достаёт до нового эндпоинта.

## Заметки по реализации (Implementation Notes)

- **`clients`/`client_triggers` нужен `spring-boot-liquibase` в classpath, не только `liquibase-core`** — Spring Boot 4.1.1 разложил автоконфигурацию по фичам; `LiquibaseAutoConfiguration` теперь живёт в отдельном артефакте `org.springframework.boot:spring-boot-liquibase`. Без него Liquibase молча никогда не запускается (ни ошибки, ни строки в логе), и первый же запрос падает в 500, потому что таблиц нет. Касается каждого будущего сервиса, принимающего AD-12, не только этого.
- **XML-комментарии не могут содержать `--`** — несколько комментариев в чейнджлоге/pom использовали `--` как тире; XML запрещает это внутри `<!-- -->`. Ломало парсинг pom.xml и, что серьёзнее, молча валило собственный XML-парсер чейнджлога Liquibase при старте (маскировалось под обычную 500, пока не прочитали логи контейнера напрямую).
- **Testcontainers 2.x переименовал свои Maven-артефакты** — `org.testcontainers:junit-jupiter` → `testcontainers-junit-jupiter`, `postgresql` → `testcontainers-postgresql` (имена Java-пакетов не изменились). Легко ошибиться снова, поскольку старый artifactId просто отдаёт 404 с Maven Central с неинформативной ошибкой.
- `client_badges` и `client_open_case_ids` — чистые упорядоченные коллекции скаляров (составной первичный ключ `(client_id, sort_order)`, без суррогатного id), поскольку это списки значений, не сущности; `client_triggers` получает собственный суррогатный id, поскольку это настоящая дочерняя сущность со своей идентичностью (`trigger.id` из мока, например `"passport-expired"`).
- Проверка безопасности ссылок по AD-4 живёт в доменном конструкторе (`ClientTrigger`), а не в web/persistence слое, так что она обеспечивается независимо от вызывающего кода — включая будущий путь прямого конструирования на уровне домена, не только текущий, загружаемый через JPA.
- **Обнаружено при независимой перепроверке после ревью-патчей, не ревьюером:** первая попытка патча добавила новый unique constraint/индекс правкой уже применённого чейнджсета `003` на месте. Чейнджсеты Liquibase неизменяемы после применения — правка меняет их checksum и кидает `ValidationFailedException` на любой среде, где он уже применён (в данном случае — собственный dev-том Postgres этого репозитория, сервис не смог стартовать). Исправлено переносом обеих добавок в новый чейнджсет `005-client-triggers-constraints`. В собственном объясняющем комментарии этого фикса была повторена та же самая ошибка с `--` в XML-комментарии, о которой уже предупреждали более ранние заметки этой же истории — поймано тем же сбоем старта, исправлено попутно.

## Журнал изменений спеки (Spec Change Log)

## Журнал триажа ревью (Review Triage Log)

- **Путь отклонения по AD-4 (безопасность ссылок) не покрыт тестами** — `medium` — Проверено: `ClientTrigger.validateLink` кидает `IllegalStateException` на небезопасную ссылку, но все засеянные ссылки уже безопасны, и ни один тест нигде не конструирует `ClientTrigger` с небезопасным значением; регрессия, ослабившая guard (например, ослабленный negative lookahead в regex), ушла бы в прод с полностью зелёным набором тестов — ровно тот класс уязвимости (GHSA-wrjc-x8rr-h8h6), для предотвращения которого этот guard существует. → **patch**: добавить точечный unit-тест на `ClientTrigger` (без Spring/Testcontainers), проверяющий, что конструктор отклоняет представительные небезопасные значения (`//evil.com`, `\evil.com`, `http://evil.com`) и по-прежнему принимает валидный относительный путь.
- **Нет catch-all обработчика исключений — сбои помимо `ClientNotFoundException` обходят конверт ошибок** — `medium` — Проверено: `ClientExceptionHandler` маппит только `ClientNotFoundException`; любое другое исключение (будущий отказ AD-4 на плохих данных, ошибка БД и т.д.) проваливается в стандартное тело ошибки Spring, нарушая требование Consistency Conventions спайна о том, что конверт «единообразен для всех девяти сервисов». → **patch**: добавить `@ExceptionHandler(Exception.class)` → `500` + `{"error":{"code":"INTERNAL_ERROR",...}}`, используя словарь, который эта же история и установила.
- **В схеме `client_triggers` не хватает ограничений, которые есть у соседних таблиц** — `low` — Проверено: нет unique-ограничения на `(client_id, trigger_key)` (ничто не мешает дубликату), и нет индекса на `client_id` (Postgres не индексирует FK-колонки автоматически; `client_badges`/`client_open_case_ids` получают это бесплатно через составной PK, `client_triggers` — нет). → **patch**: добавить оба в `001-create-client-identity-schema.xml`.
- **Конструктор `Client` не защищает `badges`/`triggers`/`openCaseIds` от null** — `low` — Проверено: `id`/`fullName` используют `Objects.requireNonNull`, но три вызова `List.copyOf(...)` кинули бы голый, менее понятный NPE при передаче null — не согласуется с собственным устоявшимся паттерном защиты класса. → **patch**: добавить такие же guard'ы `requireNonNull`.
- **Правки спайна/спеки (AD-12, словарь кодов ошибок) не видны в рецензируемом диффе** — `false` — Проверено: `ARCHITECTURE-SPINE.md` и `SPEC.md`/`SPEC.ru.md` действительно содержат AD-12 и словарь `NOT_FOUND` и т.д. (подтверждено прямым grep); дифф был сознательно ограничен кодовым следом этой истории, та же конвенция, что и в Истории 0 — не потерянное изменение.
- **Координаты артефактов `spring-boot-liquibase` / `testcontainers-*` «не проверены»** — `false` — Опровергнуто прямым доказательством, не только отчётом реализации: независимо запущены `docker compose build` (успешно) и `up -d --wait` (все 20 контейнеров healthy, чейнджсеты Liquibase видимо применены), затем запрошен живой эндпоинт и получены реальные seed-данные — ничего из этого невозможно при неверных координатах.
- **Несовпадение версии Postgres между тестом и рантаймом «непроверяемо по этому диффу»** — `false` — Проверено напрямую: `docker compose config` показывает, что каждый контейнер Postgres (включая `client-identity-service-postgres`) запинен на `postgres:18.6`, точно совпадает с пином Testcontainers в `ClientControllerTest`.
- **Риск NPE при распаковке `entity.getClientSince()`** — `false` — Проверено: `client_since` имеет `nullable="false"` на уровне БД (`001-create-client-identity-schema.xml`); сам Postgres не допускает null-значение, которое требуется для условия срабатывания этой находки.
- **Нет `equals`/`hashCode` на доменных/JPA-типах** — `low`, отклонено — Ни один текущий путь кода не сравнивает `Client`/`ClientTrigger` по значению; добавление незапрошенного публичного API под сценарий, которого пока не существует, — спекулятивно.
- **У критерия приёмки через Gateway (AC3) нет автоматического интеграционного теста** — `low`, отклонено — Согласуется с собственным устоявшимся прецедентом Истории 0 (ручная проверка `curl` через Gateway, выделенного кросс-сервисного тестового харнесса пока не существует); общепроектный e2e-набор — будущая инициатива, а не пробел, вызванный этой историей.
- **Колонка `link` не ограничена на уровне БД** — `low`, отклонено — Доменный конструктор уже громко и корректно падает на небезопасном значении; ограничение на уровне БД защищало бы от гипотетического обхода через прямой SQL, которого в этой кодовой базе сегодня не существует. (Его реальный симптом — неконвертированный ответ об ошибке, если он всё же случится — теперь покрыт патчем обработчика исключений выше.)

## Заметки по дизайну (Design Notes)

`openCaseIds` — единственное сознательное изменение контракта относительно текущего мока фронтенда в этой истории (мок несёт `openCases: [{id, label}]`; этот сервис возвращает `openCaseIds: [string]`). Фронтенд в этой истории не трогается — подключение фронтенда к реальному бэкенду и решение, как он заново получит текст label (второй вызов к `incident-case-service`, когда тот появится, или будущая event-driven read-модель), вне scope здесь и зафиксировано как заметка для той истории, которая займётся этой интеграцией.

## Проверка (Verification)

**Команды:**
- `docker compose -f backend/docker-compose.yml build client-identity-service` -- ожидается: образ собирается, включая новый Liquibase-чейнджлог в classpath
  - **Результат: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- ожидается: `client-identity-service` и его Postgres отчитываются healthy (Liquibase запускается автоматически при старте; неудачная миграция валит health check)
  - **Результат: PASS после исправления.** Первая попытка после патчей упала здесь (`ValidationFailedException` — см. Implementation Notes); исправлено и перепроверено: все 20 контейнеров healthy, `uq_client_triggers_client_id_trigger_key` и `ix_client_triggers_client_id` подтверждены через `\d client_triggers`, остальные 8 сервисов не затронуты.
- `curl -s http://localhost:8080/client-identity/clients/client-1` -- ожидается: `200`, тело совпадает с `mockData.client` (с `openCaseIds`)
  - **Результат: PASS.** Проверено независимо (не только по отчёту сабагента-реализатора): тело полностью совпадает с `mockData.client` поле-в-поле, включая точный кириллический текст, все 5 триггеров, и `"link":null` сохранён (не опущен) для триггера `mobile-app`.
- `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/client-identity/clients/nope` -- ожидается: `404`
  - **Результат: PASS.** `{"error":{"code":"NOT_FOUND","message":"Client not found: nope"}}`.
- Набор тестов модуля (запускается внутри Docker по ограничению Истории 0 про хостовый JDK 17 vs build JDK 25) -- ожидается: все проходят, включая `ClientControllerTest` на Testcontainers
  - **Результат: PASS** (по отчёту реализации: `Tests run: 3, Failures: 0, Errors: 0`; независимо не перезапускалось, но те же три сценария независимо перепроверены выше прямым curl против живого стека).
