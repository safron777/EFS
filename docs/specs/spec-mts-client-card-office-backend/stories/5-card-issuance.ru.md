---
title: 'История 5 — CAP-5: Оформление карты'
type: 'feature'
created: '2026-10-04'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<!-- Русская версия — перевод 5-card-issuance.md для удобства чтения; при расхождении английский оригинал остаётся источником истины и единственным файлом, который читает воркфлоу bmad-build. -->

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Суть (Intent)

**Проблема:** `card-issuance-service` (История 0) имеет только health-эндпоинт. CAP-5 нужно обслужить `getIssuance(clientId, productId)`, плюс критерий success, который SPEC.md уже обещает — «действие перевыпуска/новой карты создаёт новую запись заявки» — но, в отличие от `sendDocument`/`printDocument` из CAP-4, **для этого действия вообще не существует функции в `client.js`**: кнопки «Активировать карту»/«Начать оформление» в `IssuancePage.jsx` и «Перевыпустить карту» в `BalancePage.jsx` не имеют `onClick` вообще. Нет формы фронтенд-вызова, которую можно было бы зеркалить (AD-8 не на что опереться).

**Подход:** Обслужить `GET /clients/{clientId}/products/{productId}/issuance`, точно зеркаля `mockData.issuance` (с уже установленной ISO-8601 поправкой для дат в `history`), и **спроектировать новый эндпоинт с нуля** — `POST /clients/{clientId}/products/{productId}/issuance/applications` — покрывающий ровно то одно действие, которое SPEC.md уже обещает в CAP-5 (создание новой записи заявки), а не две другие мёртвые кнопки, которые остаются вне scope.

## Границы и ограничения (Boundaries & Constraints)

**Всегда:**
- Ответ `GET` зеркалит три верхнеуровневых поля `mockData.issuance` (`currentApplication`, `newCard`, `history`) по форме точно — AD-8 (это всё ещё зеркалирование одного реального вызова `client.js`, `getIssuance`).
- `currentApplication` и `newCard` статичны (не подкреплены таблицей) — то же упрощение, что История 2 сделала для `offers`, поскольку ни мок, ни какая-либо кнопка их никогда не меняют.
- `history` — настоящий, персистентный, упорядоченный список (Liquibase + JPA), засеянный тремя мок-строками в их точном заданном порядке (не по дате — собственный порядок мока не хронологичен: `app1` самый новый, `app2`/`app3` старше, но тоже не в порядке убывания). Колонка `sort_order` сохраняет этот порядок — тот же паттерн, что и у упорядоченных списков Историй 1/3.
- `history[].date` — в ISO-8601. Значение мока для `app1` (`"01.09"`) без года; читается как день.месяц текущего для этой демо года (2026) по датировке этой сессии — становится `"2026-09-01"`. `app2`/`app3` уже несут год и конвертируются напрямую (`"2025-01-12"`, `"2025-10-20"`).
- `POST .../issuance/applications` — **новый эндпоинт, придуманный для этой истории, нет существующего вызова client.js для зеркалирования** — принимает `{"card": string, "method": string}`, оба обязательны; создаёт одну новую строку `history` с сгенерированным id (`UUID`, заметно отличным от засеянных id вида `appN` — это два разных происхождения, не призванных выглядеть одинаково), сегодняшней серверной датой, `status: "В обработке"` (переиспользуя ровно ту строку статуса, которую уже использует `mockData.operations` для состояния ожидания, не изобретая новую лексику), и возвращает `201` с созданной строкой. Это **не** трогает `currentApplication`.
- Отсутствующий/пустой `card` или `method` в теле `POST` возвращает `400` с `{"error":{"code":"VALIDATION_ERROR",...}}` — первое реальное применение этого кода (установлен в Истории 1, до сих пор не использовался).

**Никогда:**
- Никакого эндпоинта для «Активировать карту» или «Изменить способ получения» — критерий success CAP-5 в SPEC.md называет только действие перевыпуска/новой карты; две другие мёртвые кнопки остаются нереализованными и вне scope.
- Никакого изменения `currentApplication`/`newCard` через `POST` — они остаются статичными в этой истории.
- Никакой реальной переписки с системой изготовления/доставки карт — новая запись заявки — просто персистентная строка, согласно тому же принципу, что и у любого другого stub-подкреплённого сюжета в спайне; отложено.

## Матрица ввода-вывода и краевых случаев (I/O & Edge-Case Matrix)

| Сценарий | Вход / состояние | Ожидаемый результат | Обработка ошибок |
|----------|--------------|---------------------------|----------------|
| Получить состояние оформления | `GET /clients/client-1/products/debit-mts-dengi/issuance` | `200`, совпадает с `mockData.issuance` (ISO-даты в `history`, тот же порядок строк) | N/A |
| Создать заявку | `POST .../issuance/applications` `{"card":"Zero","method":"Курьер"}` | `201`, новая строка (сгенерированный id, сегодняшняя дата, `status:"В обработке"`), и последующий `GET` показывает её добавленной в `history` | N/A |
| Создать заявку, отсутствует поле | `POST .../issuance/applications` `{"card":"Zero"}` | — | `400` с `{"error":{"code":"VALIDATION_ERROR",...}}` |

</frozen-after-approval>

## Карта кода (Code Map)

- `ARCHITECTURE-SPINE.md` AD-1 — `card-issuance` владеет заявками на оформление и историей
- `ARCHITECTURE-SPINE.md` AD-8 — REST-форма зеркалит `client.js`, где вызов существует; у `POST` этой истории такового нет, что явно оговорено выше, а не придумано молча
- `ARCHITECTURE-SPINE.md` AD-12 — Liquibase; новый чейнджсет, та же дисциплина неизменяемости, что и у каждой предыдущей истории
- `ARCHITECTURE-SPINE.md` § Consistency Conventions — код `VALIDATION_ERROR` (определён в Истории 1, впервые реально применён здесь), ISO-8601 даты
- Экспорт `issuance` из `src/api/mockData.js` — точная форма/значения фикстуры для `GET`
- `getIssuance(clientId, productId)` из `src/api/client.js` — единственный реальный вызов, который обслуживает эта история
- `src/pages/IssuancePage.jsx` — подтверждает, что у всех трёх кнопок действий нет `onClick` (мёртвый UI); колонки таблицы «История заявок» (`date`, `card`, `method`, `status`) обосновывают форму строки `history`
- `src/pages/BalancePage.jsx` — подтверждает, что «Перевыпустить карту» — тоже мёртвая кнопка (`canReissue`), не повод для второго mutation-эндпоинта
- `src/pages/OperationsPage.jsx` / `mockData.operations` — источник строки статуса `"В обработке"`, переиспользуемой для начального состояния новой заявки
- `backend/client-identity-service/`, `backend/card-account-service/` — устоявшаяся форма hexagonal/Liquibase/`ResponseEntityExceptionHandler` для копирования; у последнего `handleExceptionInternal` уже обобщённо обрабатывает `MethodArgumentNotValidException` (Bean Validation) → эта история первая, кто реально запускает этот путь
- `backend/gateway/src/main/resources/application.yml` — существующий маршрут `/card-issuance/**` уже проксирует сюда; изменений в gateway не требуется

## Задачи и приёмка (Tasks & Acceptance)

**Выполнение:**
- [x] `.../domain/IssuanceApplication.java` -- framework-free сущность (`id`, `date`, `card`, `method`, `status`), статичные value objects `CurrentApplication`/`NewCardInfo` (хардкод, по статичному тексту мока), порт `IssuanceRepository` (`List<IssuanceApplication> findAllOrdered()`, `IssuanceApplication save(...)`)
- [x] `.../application/GetIssuanceUseCase.java`, `CreateApplicationUseCase.java` -- последний валидирует `card`/`method` (Jakarta Bean Validation на DTO запроса, рассчитывая на существующий обобщённый `handleExceptionInternal` в `CardExceptionHandler`-подобном advice для `400`)
- [x] `.../adapters/in/web/IssuanceController.java` + DTO (`IssuanceResponse`, `CreateApplicationRequest` с `@NotBlank`-полями, `ApplicationResponse`) + `IssuanceExceptionHandler extends ResponseEntityExceptionHandler` (исправленная форма с самого начала, по урокам Историй 1-3) -- `GET /clients/{clientId}/products/{productId}/issuance`, `POST /clients/{clientId}/products/{productId}/issuance/applications`
- [x] `.../adapters/out/persistence/` -- JPA-сущность только для строк `history` (не для статичных `currentApplication`/`newCard`), Spring Data репозиторий, адаптер, реализующий доменный порт
- [x] `backend/card-issuance-service/src/main/resources/db/changelog/` -- таблица `issuance_applications` (`sort_order` для сохранения засеянного порядка) + один чейнджсет, засевающий три мок-строки с ISO-датами
- [x] `backend/card-issuance-service/src/test/java/.../IssuanceControllerIntegrationTest.java` -- `@SpringBootTest(webEnvironment = RANDOM_PORT)` против Testcontainers Postgres, покрывает все три строки I/O-матрицы

**Критерии приёмки:**
- [x] Даны засеянные данные, когда вызывается `GET /clients/client-1/products/debit-mts-dengi/issuance`, тогда ответ `200` совпадает с `mockData.issuance` по форме и порядку строк, с `history[].date` в ISO-8601.
- [x] Дано валидное тело, когда вызывается `POST .../issuance/applications`, тогда ответ `201` с новой строкой, и последующий `GET` показывает её добавленной в `history` (без изменения `currentApplication`/`newCard`).
- [x] Дано тело без `card` или `method`, когда вызывается `POST .../issuance/applications`, тогда ответ `400` с конвертом ошибки и `code: "VALIDATION_ERROR"`.
- [x] Дан поднятый через `docker compose up -d --wait` стек (compose-файл без изменений), когда те же запросы делаются через Gateway, тогда приходят те же ответы.

## Заметки по реализации (Implementation Notes)

- `CurrentApplication`/`NewCardInfo` несут свои хардкод-значения через статичный фабричный метод `fixture()` на каждом классе (не через пару порт+stub-адаптер, как `OfferEnginePort`/`StubOfferEngineAdapter` в `offers-service`): абстракция порта из AD-10 зарезервирована для реальных фасадов внешних систем (`risk-blocks`, `offers`), а для этих двух значений нигде в спайне не названа такая будущая точка подмены, поэтому выбрана более простая форма статической фабрики.
- Колонка даты в `IssuanceApplicationEntity` называется `application_date`, не `date`: никакое функциональное требование этого не требует, но так имя колонки остаётся однозначным в SQL-инструментах, не полагаясь на то, что `date` не зарезервированное слово именно в Postgres.
- `JpaIssuanceRepository.save` назначает следующий `sort_order` как `(select max(sort_order) from issuance_applications) + 1` через небольшой `@Query`, не через count строк, так что новая заявка всегда добавляется после всех существующих строк, даже если какие-то были когда-либо удалены.
- Поймано до того, как сломало прогон: черновик заголовочного комментария `001-create-issuance-applications-and-seed.xml` использовал `--` внутри XML-комментария `<!-- -->` (та же ловушка, которую уже называют собственные Implementation Notes Историй 3/4 как воспроизведённую в их чейнджлогах). XML-парсер Liquibase отверг чейнджлог целиком (`"databaseChangeLog" expected as root element`). Исправлено так же, как в тех историях: переформулирован комментарий вместо обхода парсера.
- Проверено end-to-end через `docker compose build/up --wait` плюс точные curl-команды из раздела Verification этой истории, через Gateway (`localhost:8080/card-issuance/...`), в дополнение к интеграционному тесту на Testcontainers. Полный `mvn test` реактора (все девять сервисов + gateway) также был перезапущен и остаётся зелёным, подтверждая отсутствие регрессии в других модулях.
- Хостовый Java — 17, но стек запинен на Java 25 (ARCHITECTURE-SPINE.md Stack); проверка использовала локально загруженный Temurin 25 JDK против собственного Docker-демона хоста (Testcontainers внутри контейнера столкнулись с проблемами Docker-in-Docker сети в этой сессии-песочнице, не связанными с реализацией), а для самой сборки/запуска Docker-образа — собственный `Dockerfile` проекта.
- **Патчи после ревью:** `MethodArgumentNotValidException` теперь формирует сообщение из `BindingResult.getFieldErrors()` вместо сырой строки исключения; `CreateApplicationRequest` получил `@Size(max=255/64)`, соответствующий ширине колонок сущности; новый чейнджсет `002-add-sort-order-unique-constraint.xml` добавляет unique-ограничение на `sort_order` (чейнджсет `001` не тронут); интеграционный тест получил пины `@TestMethodOrder`/`@Order` плюс новый тест на последовательное создание двух заявок. Та же ошибка с `--` в XML-комментарии была повторена и снова исправлена при написании заголовочного комментария нового чейнджсета.
- **Кросс-сервисный ретроактивный фикс (та же причина найдена здесь, применена везде):** `handleExceptionInternal` в этом сервисе, `card-account-service`, `client-identity-service` и `offers-service` молча отбрасывали заполняемый Spring параметр `headers` (например, `Allow: GET` на `405`) — внесено фиксом обработчика ошибок из Историй 2/3, не поймано до ревью этой истории. Все четыре теперь вызывают `.headers(headers)` на ответе.

## Журнал изменений спеки (Spec Change Log)

## Журнал триажа ревью (Review Triage Log)

- **Сообщение `400` для отсутствующего/пустого поля утекает полную внутреннюю строку исключения Spring** — `medium` — Проверено независимо (не только ревьюером): `curl` с `{"card":"Zero"}` вернул `message`, содержащий весь toString `MethodArgumentNotValidException` — сигнатуру контроллера/метода, индекс параметра, внутренности binding-result. → **patch**: извлекать чистое сообщение из field errors `BindingResult` (например, `"method: must not be blank"`) вместо `exception.getMessage()` для этого типа исключения.
- **Отсутствуют границы `@Size` на `CreateApplicationRequest`, из-за чего слишком длинное значение даёт 500 вместо 400** — `medium` — Проверено: `card`/`method` несут только `@NotBlank`; колонки в БД — `varchar(255)`/`varchar(64)`. Слишком длинное значение проходит Bean Validation, затем падает на flush JPA необработанным исключением, которое проваливается в catch-all `Exception.class` обработчик → `500`, что противоречит собственной цели истории «показывать ошибки ввода клиента как чистый `400`». → **patch**: добавить `@Size(max = 255)`/`@Size(max = 64)`, соответствующий длинам колонок сущности.
- **Незащищённая гонка при назначении `sort_order`; нет ограничения схемы, чтобы хотя бы громко упасть** — `low` (слой упрочнения схемы пропатчен; полный concurrency-safe редизайн отложен) — Проверено: `JpaIssuanceRepository.save` вычисляет `findMaxSortOrder()+1` и вставляет за два неатомарных шага, без `SELECT...FOR UPDATE`/последовательности/unique-ограничения; два конкурентных `POST` могли бы приземлиться с одинаковым `sort_order` с недетерминированным итоговым порядком, и ни один тест не упражняет два создания. Полный фикс (последовательность БД или блокировка на уровне строки) — больше машинерии, чем взяла на себя эта demo-масштабная, однооператорская история. → **patch**: добавить unique-ограничение на `sort_order` (превращает тихую гонку в громкий, диагностируемый отказ, а не тихую порчу) и добавить тест, создающий две заявки последовательно, проверяющий различный, правильно упорядоченный `sort_order`. → **defer**: сам редизайн атомарного выделения.
- **`Allow`/другие заголовки, вычисленные фреймворком, в `handleExceptionInternal` молча отбрасываются из каждого замапленного ответа об ошибке** — `medium` — Проверено: сигнатура метода получает `HttpHeaders headers` (Spring заполняет, например, `Allow: GET` для `405`), но ответ строится как `ResponseEntity.status(statusCode).body(...)`, никогда не вызывая `.headers(headers)`. **Это уже существует в `client-identity-service`, `offers-service` и `card-account-service`** — внесено кросс-сервисным фиксом Историй 2/3, не поймано до ревью этой истории. → **patch**: пробросить `headers` в построитель ответа во всех четырёх сервисах.
- **Общий Testcontainers/Spring-контекст на все 4 метода теста без пина порядка или очистки, риск ложных падений, зависящих от порядка** — `low` — Проверено: `IssuanceControllerIntegrationTest` не имеет `@TestMethodOrder`, `@Transactional`-откат или `@DirtiesContext`; проверка «ровно 3 строки» в GET-тесте упала бы, если JUnit случайно запустил бы тест создания первым — ложное падение, не упущенная регрессия, но реальный риск нестабильности для паттерна, который будущие истории продолжат копировать. → **patch**: закрепить порядок выполнения методов (`@TestMethodOrder`/`@Order`).
- **4xx-статусы, не являющиеся реальными ошибками валидации (например, `405`), помечаются как `"VALIDATION_ERROR"`** — `low`, отклонено — Уже существующий, сознательный дизайн из минимального 4-кодового словаря Истории 1 (`NOT_FOUND`/`VALIDATION_ERROR`/`INTERNAL_ERROR`/`UPSTREAM_UNAVAILABLE`), уже выпущенный идентично в 3 предыдущих сервисах; не дефект, внесённый этой историей, а добавление 5-го кода для этого различия — изменение словаря на уровне спайна без текущего потребителя, которому это нужно.
- **Нет защиты от идемпотентности при повторной отправке `POST`** — `low`, отклонено — Спекулятивно; требования к дедупликации нигде в этом проекте не существует, а терпимость офисного инструмента к повторной отправке (исправимо вручную) согласуется с объёмом demo.
- **`clientId`/`productId` принимаются без проверки, что они соответствуют чему-то реальному** — отклонено, вне scope по дизайну — Явно задокументировано в собственных Boundaries этой истории (`history` глобальна в этом demo), совпадает с тем же упрощением, которое уже сделали Истории 2 и 4 по той же причине.
- **Неограниченный рост `history` без пагинации** — `low`, отклонено — Спекулятивное упрочнение под масштаб, которого не существует в demo с одним засеянным клиентом.
- **Чеклист задач/приёмки отмечен `[x]`, хотя собственный журнал истории говорил, что ревью не проходило** — принято к сведению, без действий в коде — Несогласованность процесса из прохода реализации (он выставил `status: done` преждевременно); исправлено координирующей сессией, вернувшей `in-review` перед запуском этого ревью — тот же паттерн, что и у каждой предыдущей истории.
- **Нет теста на некорректное/несовпадающее по типу тело JSON** — `low`, отклонено — Общий путь 4xx, который это упражняло бы (`HttpMessageNotReadableException`), — тот же путь кода, уже покрытый тестами на отсутствующее поле и 405; третий подтип фреймворкового исключения добавляет убывающую отдачу.
- **Утверждение Code Map, что валидация «опирается на `handleExceptionInternal` `CardExceptionHandler`», буквально неверно** — `false` — `IssuanceExceptionHandler` — собственный класс (правильно — сервисы не могут делить класс между независимо развёртываемыми модулями); в сочетании с точной формулировкой списка задач («`IssuanceExceptionHandler extends ResponseEntityExceptionHandler`, исправленная форма... по урокам Историй 1-3») формулировка Code Map означает тот же исправленный *паттерн*, не буквальный межсервисный импорт — не вводит в заблуждение на самом деле.

## Заметки по дизайну (Design Notes)

Это первая история без фронтенд-вызова для зеркалирования своей mutation-части — форма эндпоинта (`POST .../issuance/applications`, поля тела запроса, схема генерации id через UUID) — придуманный дизайн, закрывающий уже данное обещание SPEC.md, а не обратная разработка существующего поведения фронтенда. Если фронтенд позже подключит «Начать оформление»/«Перевыпустить карту» к реальному вызову, эта интеграционная работа (и любое несовпадение формы, которое она обнаружит) принадлежит той истории, которая этим займётся, не этой.

## Проверка (Verification)

**Команды:**
- `docker compose -f backend/docker-compose.yml build card-issuance-service` -- ожидается: образ собирается
  - **Результат: PASS.**
- `docker compose -f backend/docker-compose.yml down -v && up -d --wait` -- ожидается: чистый старт с тома, `card-issuance-service` и его Postgres отчитываются healthy
  - **Результат: PASS.** Все 20 контейнеров healthy, оба чейнджсета (`001`, `002-add-sort-order-unique-constraint`) применены; `\d issuance_applications` подтверждает наличие `uq_issuance_applications_sort_order`.
- `curl -s http://localhost:8080/card-issuance/clients/client-1/products/debit-mts-dengi/issuance` -- ожидается: `200`, совпадает с `mockData.issuance` с ISO-датами
  - **Результат: PASS.** 3 засеянные строки history в точном порядке мока; `app1` датирован `2026-09-01`.
- `curl -s -X POST -H 'Content-Type: application/json' -d '{"card":"Zero","method":"Курьер"}' http://localhost:8080/card-issuance/clients/client-1/products/debit-mts-dengi/issuance/applications` -- ожидается: `201`, возвращена новая строка
  - **Результат: PASS.** Проверено также второе последовательное создание: обе приземляются с разными id, добавлены в порядке вызова, видны в последующем `GET`.
- `curl -s -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{"card":"Zero"}' http://localhost:8080/card-issuance/clients/client-1/products/debit-mts-dengi/issuance/applications` -- ожидается: `400`
  - **Результат: PASS после исправления.** До патча `message` утекал полную внутреннюю строку исключения; теперь `{"error":{"code":"VALIDATION_ERROR","message":"method: must not be blank"}}`. Также независимо проверено слишком длинное значение `card`: `400` с `"card: size must be between 0 and 255"`, не `500`.
- Постпатчевая кросс-сервисная проверка (не в исходном списке): `curl -i -X POST` на GET-only эндпоинт во всех четырёх патченных сервисах.
  - **Результат: PASS.** Все четыре теперь возвращают `Allow: GET` в ответе `405`; раньше молча отбрасывался.
- Набор тестов модуля -- ожидается: все проходят
  - **Результат: PASS.** `card-issuance-service` 6/6 (включая новый тест на порядок), плюс `card-account-service` 11/11, `client-identity-service` 10/10, `offers-service` 6/6 перепроверены, не затронуты кросс-сервисным фиксом заголовков. Стек остановлен после проверки.
