---
title: 'История 11 — CAP-11: Единичные инциденты и жалобы'
type: 'feature'
created: '2026-10-12'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '717b15cd59693520eaf91b901866cf6822ccc72f'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<!-- Русская версия — перевод 11-incident-case-single-incidents-complaints.md для удобства чтения; при расхождении английский оригинал остаётся источником истины и единственным файлом, который читает воркфлоу bmad-build. -->

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Суть (Intent)

**Проблема:** `incident-case-service` (Истории 0, 10) имеет массовые инциденты, но ничего для единичных инцидентов или жалоб. CAP-11 нужно обслужить `getSingleIncidents(clientId)`/`createSingleIncident(clientId, payload)` и `getComplaints(clientId)`/`createComplaint(clientId, payload)` — последнюю способность в спеке.

**Подход:** Два обычных привязанных к клиенту CRUD-lite ресурса (без Kafka, без stub-портов — обычный Liquibase + JPA, как `cashback`/`notifications`) в том же сервисе: `GET`/`POST /clients/{clientId}/incidents/single` и `GET`/`POST /clients/{clientId}/complaints`. Создание принимает только `theme` — собственный `TODO` фронтенда (`README.md` «Что осознанно оставлено как TODO») говорит, что обязательные поля формы создания ещё не определены продуктом; изобретать больше означало бы самостоятельно решать продуктовый вопрос, на который никто не ответил.

## Границы и ограничения (Boundaries & Constraints)

**Всегда:**
- `GET /clients/{clientId}/incidents/single` возвращает `[{id, theme, created, line, status}]`; `GET /clients/{clientId}/complaints` возвращает `[{id, theme, created, term, status}]` — зеркаля `mockData.singleIncidents`/`mockData.complaints` (AD-8). `created` — ISO-8601, а не `dd.MM.yyyy` мока (Consistency Conventions). Несидированный/неизвестный `clientId` возвращает `200 []` — тот же прецедент пустой коллекции, который установил каждый списочный эндпоинт начиная с Истории 3 — никогда `404`.
- `POST /clients/{clientId}/incidents/single` и `POST /clients/{clientId}/complaints` каждый принимает только `{theme}` (точно совпадает с тем, что сейчас отправляют `createSingleIncident`/`createComplaint` — `README.md` явно отмечает остальную форму создания как ожидающую продуктового ввода, не на изобретение этой историей) и возвращает `201 Created` с полной созданной строкой — допущение «additive-only» Consistency Conventions сверх базового `{ok: true}`, тот же прецедент, который уже установил `POST .../applications` card-issuance-service (История 5) для настоящего эндпоинта создания.
- Новые единичные инциденты получают `id` в формате `ЕИ-<n>` из выделенной последовательности; новые жалобы получают `id` в формате `complaint-<n>` — оба префикса уже названы в таблице Consistency Conventions `ARCHITECTURE-SPINE.md`. Обе последовательности начинаются заметно выше собственных номеров засеянных строк — чисто чтобы свежесозданный id визуально отличался от засеянных, а не потому что что-то их парсит или сравнивает.
- Две засеянные строки в каждой таблице буквально воспроизводят `mockData.singleIncidents`/`mockData.complaints`, включая их литеральные строки `id` (`"ЕИ-118225"`, `"Жалоба №118240"` и т. д.) — данные сида всегда совпадают с моком точно независимо от схемы генерации новых строк, та же сознательная расходимость, которую провела сидовая часть каждой предыдущей истории.
- Новые единичные инциденты по умолчанию получают `line: "2 линия"`, `status: "В работе"` — единственные значения, которые любое из полей принимает где-либо в моке. Новые жалобы по умолчанию получают `term: "—"`, `status: "На 2 линии"` — `"—"` уже допустимое значение мока (вторая засеянная жалоба имеет его), а `"На 2 линии"` — единственный не-терминальный статус, который мок показывает для этого ресурса.
- Контроллеры обоих ресурсов переиспользуют существующий `IncidentCaseExceptionHandler` сервиса `incident-case-service` (История 10) — один advice-класс на сервис, уже установлено.

**Никогда:**
- Никаких полей сверх `theme` не принимается и не предполагается при создании — ни приоритет, ни описание, ни вложения, ни различие типа жалоба/претензия (две строки жалоб мока различаются только отображаемой строкой `id`, а не отдельным полем типа).
- Никакого межсервисного вызова для проверки `clientId` против `client-identity-service` (AD-9).
- Никакой попытки синхронизировать собственные засеянные данные `openCases` сервиса `client-identity-service` (который уже содержит запись `complaint-118240` из собственного сида Истории 1) с этим сервисом — AD-2 удовлетворяется тем, что обе стороны ссылаются на одну и ту же схему id; ничто здесь не пишет в `client-identity-service` и не читает из него.
- Никакого ресурса с префиксом `"req-"` — этот префикс появляется в таблице Consistency Conventions спайна, но принадлежит способности, которую ни один CAP в `SPEC.md` никогда не определял; CAP-11 производит только id `ЕИ-`/`complaint-`.

## Матрица ввода-вывода и краевых случаев (I/O & Edge-Case Matrix)

| Сценарий | Вход / состояние | Ожидаемый результат | Обработка ошибок |
|----------|--------------|---------------------------|----------------|
| Известный клиент, единичные инциденты | `GET /clients/client-1/incidents/single` | `200`, 2 строки, совпадающие с моком, `created` как ISO-8601 | N/A |
| Неизвестный клиент, единичные инциденты | `GET /clients/nope/incidents/single` | `200`, `[]` | N/A |
| Создание единичного инцидента | `POST /clients/client-1/incidents/single` тело `{"theme": "Тест"}` | `201`, полная строка с новым id `ЕИ-<n>`, `line: "2 линия"`, `status: "В работе"`; последующий `GET` включает её | N/A |
| Создание единичного инцидента, пустая тема | `POST /clients/client-1/incidents/single` тело `{"theme": ""}` | — | `400 VALIDATION_ERROR`, чистое сообщение |
| Известный клиент, жалобы | `GET /clients/client-1/complaints` | `200`, 2 строки, совпадающие с моком | N/A |
| Создание жалобы | `POST /clients/client-1/complaints` тело `{"theme": "Тест"}` | `201`, полная строка с новым id `complaint-<n>`, `term: "—"`, `status: "На 2 линии"` | N/A |
| Создание жалобы, пустая тема | `POST /clients/client-1/complaints` тело `{"theme": ""}` | — | `400 VALIDATION_ERROR`, чистое сообщение |
| Не-POST метод на любом пути создания | `PUT /clients/client-1/complaints` | — | `405` с конвертом ошибки спайна, присутствует заголовок `Allow` |

</frozen-after-approval>

## Карта кода (Code Map)

- `ARCHITECTURE-SPINE.md` AD-1 — `incident-case` владеет единичными инцидентами/жалобами (плюс массовые инциденты, История 10); AD-2 — ссылка только по id; AD-8 — REST-форма зеркалит `client.js`; AD-9 — никаких синхронных межсервисных вызовов; AD-12 — миграции Liquibase; Consistency Conventions — таблица префиксов id `ЕИ-`/`complaint-`/`req-`, мутации — POST, возвращающий минимум `{ok: true}` additive-only
- Экспорты `singleIncidents`, `complaints` из `src/api/mockData.js` — точные формы сида; запись `complaint-118240` в `client.openCases` — независимое подтверждение, что `complaint-` — настоящий префикс id сервиса, отличный от отображаемой строки, которую использует сам `mockData.complaints`
- `getSingleIncidents(clientId)`/`createSingleIncident(clientId, payload)`, `getComplaints(clientId)`/`createComplaint(clientId, payload)` из `src/api/client.js`
- `src/pages/incidents/SingleIncidentsPage.jsx`, `ComplaintsPage.jsx` — оба вызывают создание только с `{theme: "..."}`, оба несут `TODO`, цитирующий `README.md` насчёт неопределённого вопроса обязательных полей
- `README.md` «Что осознанно оставлено как TODO» — явный продуктовый пробел по обязательным полям формы создания; эта история сознательно не заполняет этот пробел
- `backend/card-issuance-service/` (История 5) — прецедент для настоящего эндпоинта создания: `201 Created` + полная созданная строка, валидация запроса `@NotBlank`/`@Size`, выделенная схема генерации id, отличная от засеянных легаси-строк
- `backend/incident-case-service/` (История 10) — существующие `IncidentCaseExceptionHandler`/`ErrorResponse` (переиспользовать как есть, без изменений) и собственный набор чейнджлогов Liquibase сервиса (добавлять новые чейнджсеты, никогда не редактировать `001-create-incident-case-schema.xml`)
- `backend/gateway/src/main/resources/application.yml` — существующий маршрут `/incident-case/**` уже проксирует сюда; изменений в gateway не требуется

## Задачи и приёмка (Tasks & Acceptance)

**Выполнение:**
- [x] `db/changelog/changes/002-create-single-incidents-and-complaints-schema.xml` -- создать таблицы `single_incidents` (id, client_id, theme, created_date, line, status) и `complaints` (id, client_id, theme, created_date, term, status), плюс две генерирующие id последовательности -- AD-12; новый чейнджсет, никогда не редактируя `001-create-incident-case-schema.xml`
- [x] `db/changelog/changes/003-seed-single-incidents-and-complaints.xml` -- засеять 2 строки `singleIncidents` мока + 2 строки `complaints` для `client-1`, id буквально
- [x] `domain/SingleIncident.java`, `Complaint.java` -- framework-free value objects
- [x] `domain/SingleIncidentRepository.java`, `ComplaintRepository.java` -- порты: `List<X> findByClientId(String clientId)`; `X create(String clientId, String theme)`
- [x] `application/GetSingleIncidentsUseCase.java`, `CreateSingleIncidentUseCase.java`, `GetComplaintsUseCase.java`, `CreateComplaintUseCase.java`
- [x] `adapters/out/persistence/SingleIncidentEntity.java`, `ComplaintEntity.java`, их Spring Data интерфейсы, `JpaSingleIncidentRepository.java`, `JpaComplaintRepository.java` -- генерация id через собственную последовательность Postgres каждой таблицы, формат `ЕИ-<n>`/`complaint-<n>`
- [x] `adapters/in/web/SingleIncidentController.java` + `CreateSingleIncidentRequest.java` (`@NotBlank @Size(max=255) theme`) + DTO ответа -- `GET`/`POST /clients/{clientId}/incidents/single`
- [x] `adapters/in/web/ComplaintController.java` + `CreateComplaintRequest.java` (та же форма валидации) + DTO ответа -- `GET`/`POST /clients/{clientId}/complaints`
- [x] `src/test/.../SingleIncidentControllerIntegrationTest.java`, `ComplaintControllerIntegrationTest.java` -- покрывают все 8 строк матрицы I/O на реальном Testcontainers Postgres

**Критерии приёмки:**
- Дан сид, когда вызывается `GET /clients/client-1/incidents/single`, тогда `200` с 2 строками, совпадающими с моком точно, даты как ISO-8601.
- Дана тема, когда вызывается `POST /clients/client-1/incidents/single`, тогда `201` с новым id `ЕИ-<n>`, `line: "2 линия"`, `status: "В работе"`, и последующий `GET` для того же клиента включает её.
- Дан сид, когда вызывается `GET /clients/client-1/complaints`, тогда `200` с 2 строками, совпадающими с моком точно.
- Дана тема, когда вызывается `POST /clients/client-1/complaints`, тогда `201` с новым id `complaint-<n>`, `term: "—"`, `status: "На 2 линии"`.
- Дана пустая `theme` на любом эндпоинте создания, тогда `400` с чистым сообщением валидации.
- Дан неизвестный `clientId`, когда вызывается любой `GET`, тогда `200` с `[]`, никогда `404`.

## Заметки по реализации (Implementation Notes)

- Разбивка на два ресурса (`SingleIncidentController`/`ComplaintController`, каждый со своим набором domain/application/persistence, переиспользующий существующий `IncidentCaseExceptionHandler` Истории 10 без изменений), генерация id через последовательности (`ЕИ-<n>`/`complaint-<n>`), и разделение сид-вербатим-против-нового-генерируемого-id легли точно по плану из Карты кода/Задач — без отклонений. Новых Maven-зависимостей или изменений пакетов сверх того, что уже добавила История 10, не потребовалось.
- Обе последовательности (`single_incident_id_seq`, `complaint_id_seq`) начинаются с `200000`, заметно выше собственных номеров засеянных строк, чисто для визуальной отличимости — подтверждено вживую: свежесозданный единичный инцидент получил id `ЕИ-200000`, свежесозданная жалоба — `complaint-200000`.
- Чейнджсеты Liquibase добавлены как новые файлы (`002-create-single-incidents-and-complaints-schema.xml`, `003-seed-single-incidents-and-complaints.xml`); `001-create-incident-case-schema.xml` (История 10) не тронут.
- Все четыре файла package-info.java в этом сервисе (`domain`, `application`, `adapters/in/web`, `adapters/out/persistence`) после реализации всё ещё упоминали только классы Истории 10; самостоятельно найдено при независимом ревью и обновлено с добавлением классов этой истории.
- **Патч после ревью:** два ревьюера независимо сошлись на одном и том же пробеле — `knownClientReturns200WithTwoSeededRowsMatchingMockExactly` в обоих тестовых классах проверял только то, что обе засеянные строки существуют где-то в списке, но никогда — что они действительно упорядочены от новых к старым, как обещает собственный Javadoc репозитория; пропавший/обратный `OrderBy` прошёл бы незамеченным. Добавлено по одному утверждению упорядочивания в каждый тест (само поведение упорядочивания уже было корректным, подтверждено самостоятельно вживую до патча). Также добавлен по одному тесту на ресурс для непротестированной границы `@Size(max=255)` на `theme`.
- Независимая сквозная перепроверка (чистый `docker compose up -d --wait`, все контейнеры) выполнена самостоятельно, как до, так и после патча: все 8 строк матрицы I/O подтверждены через `curl` через Gateway — включая то, что созданная строка видна в последующем `GET`, что краевой случай слишком длинного clientId прослежен (предсуществующий, идентичный пробел для каждого мутационного эндпоинта начиная с Истории 9, не уникален для этой истории), и что `Allow: POST, GET` присутствует на `405`. Набор тестов модуля перепроверен независимо в одноразовом контейнере `eclipse-temurin:25` (хостовый JDK — 17, по Docker-only ограничению проекта): все 4 тестовых класса сервиса проходят, 27/27 всего (9 Kafka-слушателя + 6 REST массовых инцидентов + 6 жалоб + 6 единичных инцидентов, было 25 до патча).

## Журнал изменений спеки (Spec Change Log)

## Журнал триажа ревью (Review Triage Log)

- **Находка (Blind Hunter + Edge Case Hunter, сходящаяся):** `knownClientReturns200WithTwoSeededRowsMatchingMockExactly` в обоих тестовых классах находил каждую ожидаемую строку через `.stream().filter(...).findFirst()`, поэтому ни один тест не проверял, действительно ли строки упорядочены от новых к старым, как обещает собственный Javadoc репозитория — пропавший или обратный `OrderBy` прошёл бы незамеченным. **Вердикт: patch (применён).** В каждый тест добавлено по одному утверждению, закрепляющему элемент 0 за более новой засеянной строкой, а элемент 1 — за другой. Само поведение упорядочивания уже было корректным (подтверждено самостоятельно вживую до патча); не хватало только утверждения теста.
- **Находка (Blind Hunter):** ни один тестовый класс не проверял границу `@Size(max = 255)` на `theme` — покрыт был только пустой случай. **Вердикт: patch (применён).** Добавлен по одному тесту на класс, отправляющему `theme` длиной 256 символов и проверяющему `400 VALIDATION_ERROR`.
- **Находка (Blind Hunter):** дифф добавляет спеку истории без видимого в своей области путей файла-близнеца `.ru.md`. **Вердикт: false.** Русский дубликат существует на диске, создан вместе с одобрением спеки; он был за пределами области путей, с которой был сгенерирован дифф ревьюера, а не реальный пробел.
- **Находка (Blind Hunter):** нет unit-теста на инварианты `Objects.requireNonNull` доменных value objects. **Вердикт: отклонена.** Совпадает с уже установленной, общей для всего проекта конвенцией «только интеграционные тесты», уже идентично принятой в Историях 7-10.
- **Находка (Blind Hunter):** `clientId` принимается без проверки на пустоту/пробелы — сегмент пути вроде одного пробела персистировал бы фактически недостижимую строку. **Вердикт: отклонена.** Ни один сервис в этой кодобазе не проверяет формат path-переменных `clientId`/`cardId`/`incidentId`; фронтенд всегда предоставляет настоящее, выведенное из контекста значение, а вручную сконструированные некорректные запросы — класс угроз, от которого этот бэкенд никогда и нигде не защищался.
- **Находка (Blind Hunter):** изоляция между клиентами для `findByClientId` протестирована только с `client-1` и нулевым `nope`, никогда с двумя разными реальными клиентами с данными. **Вердикт: отклонена.** Стандартный параметризованный запрос `WHERE client_id = ?`, тривиально корректный по построению, без общего/кэшированного состояния, которое могло бы протечь между клиентами — полнота покрытия без правдоподобного режима отказа.
- **Находка (Blind Hunter):** `ComplaintEntity`/`SingleIncidentEntity` не переопределяют `equals`/`hashCode`. **Вердикт: отклонена.** Согласуется с каждой другой JPA-сущностью в этой кодобазе; ни одна их не реализует.
- **Находка (Blind Hunter):** нет теста на полностью отсутствующий ключ `theme` (`{}`), отличного от `{"theme":""}`. **Вердикт: отклонена/false.** `@NotBlank` Jakarta явно обрабатывает отсутствующее (null) поле так же, как пустое — не отдельный путь кода от уже протестированного.
- **Находка (Blind Hunter):** `created` использует серверный `LocalDate.now()` без явной зоны, а тест проверяет точное равенство со вторым вызовом `LocalDate.now()` — скрытая возможность флаки на границе полуночи. **Вердикт: отклонена.** Тот же паттерн уже идентично используется в другом месте этой кодобазы для серверных дат создания (например, История 5); крайне низкая реальная вероятность, не уникальна для этой истории.
- **Находка (Blind Hunter):** у двух новых чейнджсетов Liquibase нет элементов `<rollback>`. **Вердикт: defer.** Предсуществует во всех чейнджсетах каждого сервиса начиная с Истории 1; не привнесено и не усугублено здесь.
- **Находка (Edge Case Hunter):** `clientId` длиннее колонки `varchar(64)` при `POST` привёл бы к необработанному `DataIntegrityViolationException`, проявляющемуся как `500` вместо `400`. **Вердикт: defer.** Идентичный, предсуществующий пробел для каждого id-path-переменной (`notificationId`, `incidentId` и т. д.) во всех мутационных эндпоинтах начиная с Истории 9; не уникален и не усугублён этой историей.
- **Находка (Verification Gap):** нет находок сверх сходящегося пробела в упорядочивании выше (уже исправлен) — подтверждено, что все 8 строк матрицы I/O покрыты `SingleIncidentControllerIntegrationTest`/`ComplaintControllerIntegrationTest`, прогоняются на реальном Testcontainers Postgres и проходят.

## Заметки по дизайну (Design Notes)

Два решения по объёму стоит зафиксировать. Первое: создание принимает только `theme`: сам `README.md` говорит, что настоящие обязательные поля (приоритет, описание, вложения) ждут продуктового ввода, и изобретение их здесь означало бы, что эта история тихо решает продуктовый вопрос, на который никто не ответил — именно то, что явно вне границ. Второе: новые строки получают другой формат id, чем две засеянные легаси-строки (`ЕИ-<n>`/`complaint-<n>` против `"ЕИ-118225"`/`"Жалоба №118240"` мока): это не столько несогласованность, сколько то же разделение, которое провела каждая предыдущая история с легаси-данными сида — данные сида зеркалят историю мока точно, новые данные следуют собственной перспективной конвенции архитектуры, подтверждённой независимо здесь собственным примером `openCases` сервиса `client-identity-service`, уже использующим `complaint-118240`, а не `"Жалоба №118240"`, для той же реальной сущности.

## Проверка (Verification)

**Команды:**
- `docker compose -f backend/docker-compose.yml build incident-case-service` -- ожидается: образ собирается
  - **Результат: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- ожидается: `incident-case-service` и его Postgres отчитываются healthy
  - **Результат: PASS.** Весь стек (gateway + все девять сервисов + Kafka + инстансы Postgres) healthy.
- `curl -s http://localhost:8080/incident-case/clients/client-1/incidents/single` -- ожидается: `200`, 2 строки, совпадающие с моком
  - **Результат: PASS.** Обе засеянные строки (`ЕИ-118225`, `ЕИ-117904`) возвращены с `created` как ISO-8601 (`2026-09-02`/`2026-08-14`) и `theme`/`line`/`status`, совпадающими с моком точно.
- `curl -s -X POST -H 'Content-Type: application/json' -d '{"theme":"Тест"}' http://localhost:8080/incident-case/clients/client-1/incidents/single` -- ожидается: `201`, новая строка `ЕИ-<n>`
  - **Результат: PASS.** `201`, `{"id":"ЕИ-200000","theme":"Тестовый инцидент","created":"2026-10-12","line":"2 линия","status":"В работе"}`; последующий `GET` показал её первой (порядок от новых к старым).
- `curl -s http://localhost:8080/incident-case/clients/client-1/complaints` -- ожидается: `200`, 2 строки, совпадающие с моком
  - **Результат: PASS.** Обе засеянные строки (`Жалоба №118240`, `Претензия №117580`) возвращены, совпадая с моком точно, включая `term: "—"` для второй строки.
- `curl -s -X POST -H 'Content-Type: application/json' -d '{"theme":"Тест"}' http://localhost:8080/incident-case/clients/client-1/complaints` -- ожидается: `201`, новая строка `complaint-<n>`
  - **Результат: PASS.** `201`, `{"id":"complaint-200000","theme":"Тестовая жалоба","created":"2026-10-12","term":"—","status":"На 2 линии"}`.
- `curl -i -X POST -H 'Content-Type: application/json' -d '{"theme":""}' http://localhost:8080/incident-case/clients/client-1/complaints` -- ожидается: `400`
  - **Результат: PASS.** `400`, `{"error":{"code":"VALIDATION_ERROR","message":"theme: must not be blank"}}`.
- `curl -i -X PUT http://localhost:8080/incident-case/clients/client-1/complaints` -- ожидается: `405`, присутствует заголовок `Allow`
  - **Результат: PASS.** `405`, `Allow: POST, GET`, тело `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'PUT' is not supported"}}`.
- Набор тестов модуля -- ожидается: все проходят
  - **Результат: PASS.** 27/27 по всем 4 тестовым классам сервиса, перепроверено самостоятельно после патча. Стек остановлен после ручной верификации.
