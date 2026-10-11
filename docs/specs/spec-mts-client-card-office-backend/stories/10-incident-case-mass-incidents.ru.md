---
title: 'История 10 — CAP-10: Массовые инциденты incident-case (Kafka)'
type: 'feature'
created: '2026-10-11'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '665b96e9ae9553b1faa337274e4be86d8d618a12'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<!-- Русская версия — перевод 10-incident-case-mass-incidents.md для удобства чтения; при расхождении английский оригинал остаётся источником истины и единственным файлом, который читает воркфлоу bmad-build. -->

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Суть (Intent)

**Проблема:** `incident-case-service` (История 0) имеет только health-эндпоинт. CAP-10 нужно, чтобы массовые инциденты доходили до `GET /incidents/mass` фронтенда (`getMassIncidents()`) исключительно через события Kafka — никогда через REST-опрос системы мониторинга (AD-5) — плюс подключить `attachClientToIncident(clientId, incidentId)` к настоящей мутации.

**Подход:** Три `@KafkaListener` (один на топик: `incident-case.mass-incident.{opened,updated,closed}`, AD-11) пишут в собственную таблицу read-модели этого сервиса, идемпотентно делая upsert по `id` инцидента; событие, чей `version` консьюмер не поддерживает, republish-ится в dead-letter топик, а не применяется и не отбрасывается молча. `GET /incidents/mass` и `POST /incidents/mass/{incidentId}/attach` читают/пишут ту же таблицу — никакого синхронного вызова другого сервиса (AD-9) и никакого REST-вызова системы мониторинга (AD-5).

## Границы и ограничения (Boundaries & Constraints)

**Всегда:**
- `GET /incidents/mass` возвращает JSON-массив `[{id, theme, product, start, status, affected, matchesClient}]`, зеркаля `getMassIncidents()` — AD-8. `start` — ISO-8601 (Consistency Conventions), а не `dd.MM.yyyy` мока.
- Три Kafka-топика, один на глагол события (AD-11): `incident-case.mass-incident.opened`, `.updated`, `.closed`. Каждый конверт — `{eventId, eventType, occurredAt, version, data}` (AD-11). Поля `data`, разрешённые во время планирования этой истории (закрывает прежний открытый вопрос 2 SPEC.md):
  - `opened`: `{id, theme, product, start, status, affected}` — всё зафиксировано при создании.
  - `updated`: `{id, status, affected}` — только два поля, которые законно могут меняться после открытия.
  - `closed`: `{id, status}`.
- Потребление `opened` для `id`, уже существующего на диске, или `updated`/`closed` для неизвестного `id`, обрабатывается без краха консьюмера: `opened` делает upsert (идемпотентная повторная доставка), `updated`/`closed` для неизвестного `id` логируется и пропускается (доставка Kafka at-least-once и порядок топиков делают оба случая достижимыми в нормальной работе, а не только теоретическим краем).
- Событие, чей `version` не является единственным поддерживаемым значением, никогда не применяется — его полный исходный конверт republish-ится буквально в `incident-case.mass-incident.dead-letter` (AD-11: «направить в dead-letter топик, а не тихо провалиться»), и консьюмер его подтверждает (без цикла повторов, без краха).
- Неизвестные поля внутри `data` игнорируются (прямая совместимость AD-11) — десериализовать `data` нежёстко, никогда не падать на лишнем поле.
- `matchesClient` всегда `false` в каждом ответе. Это явно не часть `data` никакого Kafka-события (система мониторинга, продюсер AD-11, не имеет понятия о конкретном клиенте банковского офиса), и ничто в этой истории это не вычисляет (настоящая корреляция потребовала бы синхронного вызова в `operations-service`, запрещённого AD-9) — решение, принятое явно во время планирования этой истории, а не недосмотр. Баннер-подсказка фронтенда «Похоже на совпадение» (`matched = incidents.find(i => i.matchesClient...)`) просто никогда не срабатывает, пока будущая история не добавит настоящую корреляцию (Отложено).
- `POST /incidents/mass/{incidentId}/attach` с телом `{clientId}` записывает этого клиента за этим инцидентом и возвращает `{ok: true}` (базовый ответ мутации по Consistency Conventions), зеркаля `attachClientToIncident(clientId, incidentId)`. Привязка одного и того же `clientId` к одному и тому же инциденту дважды идемпотентна (без дублирующей строки, без ошибки) — UI позволяет сотруднику нажать кнопку больше одного раза.
- Привязка к неизвестному `incidentId` возвращает `404 NOT_FOUND` — тот же прецедент однo-ресурсного `NotFoundException`, который уже установили Истории 1/4/9 (зеркалить форму `NotificationChannelNotFoundException` точно).
- Kafka-топики декларируются явно через бины `NewTopic` (`KafkaAdmin`), а не оставляются на auto-create брокера — самодокументируется и корректно, даже если auto-create когда-нибудь отключат.
- Exception handler этого сервиса поставляется в уже финальной форме с самого начала, расширенной одним маппингом `NotFoundException` выше.

**Никогда:**
- Никакого синхронного HTTP-вызова из этого сервиса в любой другой сервис, никогда (AD-9) — ни для проверки `clientId`, ни для вычисления `matchesClient`.
- Никакого REST-опроса системы мониторинга — read-модель наполняется исключительно тремя Kafka-консьюмерами (AD-5).
- Никакого бизнес-логического повтора/requeue события с плохой версией обратно в его же топик — оно идёт в dead-letter топик один раз и подтверждается, и всё.
- `affected` остаётся свободнотекстовой приблизительной строкой мока (например, `"~3 200 клиентов"`) без изменений — это описание, а не число, поэтому AD-3 к нему не применяется.

## Матрица ввода-вывода и краевых случаев (I/O & Edge-Case Matrix)

| Сценарий | Вход / состояние | Ожидаемый результат | Обработка ошибок |
|----------|--------------|---------------------------|----------------|
| Инциденты ещё не потреблены | `GET /incidents/mass` | `200`, `[]` | N/A |
| Потреблено событие `opened` | Опубликовать валидный конверт `opened` | Последующий `GET /incidents/mass` включает этот инцидент, `matchesClient: false` | N/A |
| Потреблено событие `updated` для известного инцидента | Опубликовать `updated`, меняющий `status`/`affected` | `GET` отражает новые `status`/`affected`; `theme`/`product`/`start` без изменений | N/A |
| Потреблено событие `closed` | Опубликовать `closed` | `GET` отражает новый `status` | N/A |
| Событие с неподдерживаемой версией | Опубликовать конверт с `version: 99` | Не применяется к read-модели; полный конверт появляется в `incident-case.mass-incident.dead-letter`; консьюмер продолжает работать | N/A |
| Привязка известного инцидента | `POST /incidents/mass/{id}/attach` тело `{"clientId": "client-1"}` | `200 {"ok": true}`, идемпотентно при повторе | N/A |
| Привязка неизвестного инцидента | `POST /incidents/mass/nope/attach` тело `{"clientId": "client-1"}` | — | `404 NOT_FOUND` |
| Не-POST метод на пути привязки | `PUT /incidents/mass/{id}/attach` | — | `405` с конвертом ошибки спайна, присутствует заголовок `Allow` |

</frozen-after-approval>

## Карта кода (Code Map)

- `ARCHITECTURE-SPINE.md` AD-1 — `incident-case` владеет состоянием массовых инцидентов; AD-2 — ссылка только по id; AD-5 — Kafka-push, никогда опрос; AD-8 — REST-форма зеркалит `client.js`; AD-9 — никаких синхронных межсервисных вызовов, никогда; AD-11 — именование топиков/конверт (теперь расширен формами `data`, разрешёнными этой историей) и обработка dead-letter; AD-12 — миграции Liquibase
- Экспорт `massIncidents` из `src/api/mockData.js` — точная форма строки (минус настоящий смысл `matchesClient`, разрешённый выше)
- `getMassIncidents()` из `src/api/client.js` → `GET /incidents/mass` (без `clientId` — глобальный, не привязанный к объёму список); `attachClientToIncident(clientId, incidentId)` → `POST /incidents/mass/{incidentId}/attach`
- `src/pages/incidents/MassIncidentsPage.jsx` — рендерит кнопку «Привязать клиента» независимо от `matchesClient` (меняется только её цвет) и условный баннер «Похоже на совпадение», завязанный на `matchesClient` — подтверждает, что привязка всегда кликабельна, а баннер косметический, и эта история не обязана заставить его правильно срабатывать
- Маппинг `DocumentNotFoundException` + `CardExceptionHandler` из `backend/card-account-service/` (История 4), повторно использованный идентично в `NotificationChannelNotFoundException` Истории 9 — точная форма для `404` привязки к неизвестному инциденту
- `backend/card-issuance-service/` (История 5) — прецедент для формы `@NotBlank`/чистого сообщения валидации на теле запроса
- `backend/incident-case-service/` (История 0) — существующий скелет; уже есть `adapters/in/messaging/package-info.java`, документирующий именно этот план Kafka-консьюмера, и `adapters/out/persistence/package-info.java`, подтверждающий, что сервис владеет данными
- `backend/docker-compose.yml` строки ~358-396 — Kafka (режим KRaft, единственный брокер) уже поднята с комментарием, предвосхищающим именно эту историю; изменений в compose не требуется
- `backend/gateway/src/main/resources/application.yml` — существующий маршрут `/incident-case/**` (`StripPrefix=1`) уже проксирует сюда; изменений в gateway не требуется
- `backend/pom.xml` — `testcontainers-bom` (2.0.5) уже импортирован раньше собственного пина Boot, поэтому тестовая зависимость `testcontainers-kafka` резолвит совпадающую версию автоматически; версия Kafka-клиента зафиксирована транзитивно той версией `spring-kafka`, которой управляет BOM Spring Boot 4.1.1 — проверить в сети, что это чисто сочетается с образом брокера `apache/kafka:4.3.1`, прежде чем предполагать совместимость

## Задачи и приёмка (Tasks & Acceptance)

**Выполнение:**
- [x] `db/changelog/changes/001-create-incident-case-schema.xml` -- создать таблицы `mass_incidents` (id, theme, product, start_date, status, affected) и `mass_incident_attachments` (incident_id, client_id, attached_at; unique на `(incident_id, client_id)`) -- AD-12
- [x] `domain/MassIncident.java` -- framework-free value object (без поля `matchesClient` — это константа слоя ответа `false`, а не доменное состояние)
- [x] `domain/MassIncidentNotFoundException.java` -- зеркалит форму `NotificationChannelNotFoundException` точно
- [x] `domain/MassIncidentRepository.java` -- порт: `List<MassIncident> findAll()`; `void upsertOpened(...)`; `void applyUpdate(id, status, affected)` (no-op + лог, если id неизвестен); `void applyClose(id, status)` (аналогично); `void attachClient(incidentId, clientId)`, выбрасывающий `MassIncidentNotFoundException` для неизвестного id, идемпотентный при повторной привязке
- [x] `application/GetMassIncidentsUseCase.java`, `AttachClientUseCase.java` -- оркестрируют порт для REST-стороны
- [x] `application/ApplyMassIncidentEventUseCase.java` -- единственный use case, который вызывают все три листенера, диспетчеризуя к нужному методу репозитория по типу события
- [x] `adapters/out/persistence/MassIncidentEntity.java`, `MassIncidentAttachmentEntity.java`, их Spring Data интерфейсы, `JpaMassIncidentRepository.java` -- маппят обе таблицы, upsert через `save` над найденной-или-новой сущностью
- [x] `adapters/in/messaging/MassIncidentEventEnvelope.java` -- generic-record конверта `{eventId, eventType, occurredAt, version, data: JsonNode}`, общий для всех трёх листенеров
- [x] `adapters/in/messaging/MassIncidentOpenedData.java`, `MassIncidentUpdatedData.java`, `MassIncidentClosedData.java` -- типизированные формы `data` по Границам и ограничениям, нежёсткие к неизвестным полям
- [x] `adapters/in/messaging/MassIncidentEventListener.java` -- три метода `@KafkaListener` (один на топик); каждый проверяет `version`, republish-ит в dead-letter топик при несовпадении, иначе конвертирует `data` и вызывает `ApplyMassIncidentEventUseCase`
- [x] `adapters/in/messaging/KafkaTopicsConfig.java` -- бины `NewTopic` для трёх топиков событий + dead-letter топика
- [x] `adapters/in/web/MassIncidentController.java` + `AttachClientRequest.java` (`@NotBlank String clientId`) + DTO ответа -- `GET /incidents/mass`, `POST /incidents/mass/{incidentId}/attach`
- [x] `adapters/in/web/IncidentCaseExceptionHandler.java` + `ErrorResponse.java` + `OkResponse.java` -- финальная форма с самого начала, плюс маппинг `MassIncidentNotFoundException` → `404`
- [x] `src/test/.../MassIncidentEventListenerIntegrationTest.java` -- настоящий embedded/Testcontainers Kafka-брокер + Testcontainers Postgres: публикует конверты `opened`/`updated`/`closed`/с плохой версией, проверяет read-модель (через репозиторий или `GET`) и содержимое dead-letter топика
- [x] `src/test/.../MassIncidentControllerIntegrationTest.java` -- покрывает чисто REST-строки матрицы I/O (пустой список, привязка известного/неизвестного, неверный метод)

**Критерии приёмки:**
- Дано, что события не потреблены, когда вызывается `GET /incidents/mass`, тогда `200 []`.
- Дано событие `opened`, опубликованное в `incident-case.mass-incident.opened`, когда затем вызывается `GET /incidents/mass`, тогда инцидент появляется с `matchesClient: false` и полями события, `start` как ISO-8601.
- Дан этот инцидент, когда событие `updated` меняет его `status`/`affected`, тогда последующий `GET` отражает ровно эти два изменённых поля и ничего больше.
- Дано событие с неподдерживаемым `version`, когда оно публикуется, тогда оно никогда не появляется в read-модели, а его полный конверт появляется в `incident-case.mass-incident.dead-letter`.
- Дан известный инцидент, когда `POST /incidents/mass/{id}/attach` вызывается дважды с одним и тем же `clientId`, тогда оба вызова возвращают `200 {"ok": true}` и существует только одна строка привязки.
- Дан неизвестный инцидент, когда вызывается тот же `POST`, тогда `404` с конвертом ошибки спайна.

## Заметки по реализации (Implementation Notes)

- Spring Boot 4 разбивает поддержку Kafka так же, как уже разбивает Liquibase: зависимость для декларации — `spring-boot-starter-kafka` (который тянет `spring-kafka` плюс новый модуль автоконфигурации `spring-boot-kafka`), а не голая пара `spring-kafka`/`spring-boot-autoconfigure`. Подтверждено прямым осмотром резолвленного jar-файла (`spring-boot-kafka-4.1.1.jar` содержит `KafkaAutoConfiguration` и, под `org/springframework/boot/kafka/testcontainers/`, `ApacheKafkaContainerConnectionDetailsFactory` — класс, благодаря которому `@ServiceConnection` на поле `org.testcontainers.kafka.KafkaContainer` работает из коробки). На стороне тестов `spring-boot-starter-kafka-test` — соответствующий стартер.
- `kafka-clients` 4.2.1 (подтянут транзитивно `spring-kafka` 4.1.1, оба зафиксированы BOM Spring Boot 4.1.1) против образа брокера `apache/kafka:4.3.1`: подтверждена совместимость на практике, а не просто предположена — весь набор `MassIncidentEventListenerIntegrationTest` (настоящий Testcontainers-брокер `apache/kafka:4.3.1`) и ручная верификация полного стека ниже оба продюсируют, консьюмят и админ-создают топики против именно этого образа без единой ошибки протокола/версии.
- Эта кодобаза использует Jackson 3, а не Jackson 2: аннотации остаются под `com.fasterxml.jackson.annotation.*` (без изменений), но `ObjectMapper`/`JsonNode` переехали в `tools.jackson.databind.*` (подтверждено реальной ошибкой компиляции до исправления импорта, затем перепроверено по существующему `CardControllerIntegrationTest` в `card-account-service`, который уже импортирует `tools.jackson.databind.JsonNode`/`ObjectMapper`). `MassIncidentEventEnvelope`, `MassIncidentOpenedData`/`UpdatedData`/`ClosedData` и `MassIncidentEventListener` все используют пакет `tools.jackson.databind` соответственно.
- Настоящий класс модуля Testcontainers Kafka — `org.testcontainers.kafka.KafkaContainer` (артефакт `testcontainers-kafka`, управляемый `testcontainers-bom` 2.0.5, уже импортированным в родительском POM) — подтверждено извлечением резолвленного jar-файла и запуском `javap` на нём, а не угадыванием: он предоставляет конструктор `KafkaContainer(String dockerImageName)`, который оба тестовых класса используют с `"apache/kafka:4.3.1"`, точно совпадая с образом брокера, зафиксированным в `docker-compose.yml`.
- Маршрутизация в dead-letter пересылает необработанный `String`-пейлоад листенера Kafka без изменений, никогда повторно сериализованный Java-объект — методы `@KafkaListener` здесь объявлены как `(String payload)`, а не `(MassIncidentEventEnvelope envelope)`, специально чтобы «republish-ится буквально» (Границы и ограничения) было побайтовой гарантией, а не приближением, которое могло бы переставить поля. Подтверждено прямо: интеграционный тест dead-letter проверяет, что значение потреблённой записи `isEqualTo` ровно той буквальной строке, что была опубликована.
- Ни один метод листенера никогда не даёт исключению просочиться наружу (приватный хелпер `withValidEnvelope` глушит и логирует и ошибки парсинга JSON, и ошибки применения), поэтому «консьюмер подтверждает его, без цикла повторов, без краха» (Границы и ограничения) получается бесплатно из поведения контейнера Spring Kafka по умолчанию — никакой кастомный `CommonErrorHandler` или ручной `Acknowledgment` не потребовался.
- `mass_incident_attachments` использует составной первичный ключ (`incident_id`, `client_id`) через `@IdClass` JPA, тот же прецедент, что у связывающих таблиц `client_badges`/`client_open_case_ids` в `client-identity-service`, а не суррогатный id. Это одновременно удовлетворяет буквальное «unique на `(incident_id, client_id)`» из задачи и делает повторную привязку обычным `save`-как-merge той же строки (проверка на дублирующую строку не нужна для идемпотентности) — подтверждено прямым осмотром таблицы после двух вызовов `POST .../attach`: ровно одна строка.
- И `MassIncidentEventListenerIntegrationTest`, и `MassIncidentControllerIntegrationTest` запускают Testcontainers Kafka-брокер через `@ServiceConnection`, хотя тест контроллера сам ничего не публикует — без этого создание топиков `KafkaAdmin` при старте иначе потратило бы реальное время, повторяя попытки против недостижимого хоста `kafka:9092` (не фатально, поскольку `spring.kafka.admin.fail-fast` по умолчанию `false`, но неоправданно медленно); тест контроллера публикует одно установочное событие `opened` через настоящий путь Kafka, чтобы получить свой единственный известный инцидент, а не засеивает базу данных напрямую, чтобы оставаться честным насчёт реальной границы CAP-10 (AD-5).
- Три новых XML-комментария Liquibase изначально содержали буквальный двойной дефис (`--`) внутри `<!-- -->`, что XML запрещает внутри комментария — пойманo реальной ошибкой парсинга Liquibase (`"databaseChangeLog" expected as root element"`) во время верификации, а не догадкой линтера, и переформулированы, чтобы избежать этого. Та же самая ловушка повторилась при редактировании собственных XML-комментариев `pom.xml` (на этот раз обычная ошибка парсинга Maven POM) до того, как любой из файлов собрался. Это та же повторяющаяся ловушка, которую уже отметили Заметки по реализации Истории 9 (которые сами цитируют Истории 1/3/4/5) — теперь повторилась в двух дополнительных файлах в одной этой истории.
- `version` в `MassIncidentEventEnvelope` — примитивный `int`, а не `Integer`: отсутствующее/нечисловое поле `version` десериализуется в `0`, а не выбрасывает исключение, что проверка листенера `!= SUPPORTED_VERSION` уже трактует как неподдерживаемое и направляет в dead-letter — одной проверкой на null меньше, тот же корректный результат.
- Javadoc package-info для всех пяти пакетов `incident-case-service` (`domain`, `application`, `adapters/in/{web,messaging}`, `adapters/out/persistence`) обновлены с формулировки «пустой скелет» Истории 0 до того, что эта история наполнила — по точному прецеденту, который уже установили пять package-info `notifications-service` в Истории 9.
- **Патч после ревью:** все три независимых слоя ревью сошлись на одном и том же пробеле — событие с валидной версией, чья `data` не смогла примениться, логировалось и тихо отбрасывалось, никогда не направляясь в dead-letter, в отличие от двух других путей отказа. Catch-блок отказа применения в `withValidEnvelope` теперь также вызывает `sendToDeadLetter`, поэтому все три режима отказа направляются в dead-letter единообразно; сам `sendToDeadLetter` теперь логирует, если публикация в dead-letter не удалась; ветка непарсящегося payload теперь логирует объект исключения, а не только его сообщение. Новый тест (`dataConversionFailureIsRoutedToDeadLetterAndConsumerKeepsRunning`) подтверждает фикс и то, что консьюмер выживает и продолжает потреблять после. Перепроверено самостоятельно: образ пересобран, сценарий воспроизведён вживую через `kafka-console-producer`/`-consumer` на чистом стеке, полный набор тестов модуля перепрогнан (15/15 проходят).

## Журнал изменений спеки (Spec Change Log)

## Журнал триажа ревью (Review Triage Log)

- **Находка (Blind Hunter + Edge Case Hunter + Verification Gap, сходящаяся):** событие с валидной `version`, чья `data` не смогла сконвертироваться/примениться, попадало только в общий catch в `withValidEnvelope` и тихо логировалось-и-отбрасывалось — в отличие от двух других путей отказа (непарсящийся JSON, неподдерживаемая версия), оно никогда не направлялось в dead-letter, и ни один тест его не проверял. **Вердикт: patch (применён).** Все три независимых ревьюера сошлись ровно на этом пробеле. Исправлено: catch-блок отказа применения теперь также вызывает `sendToDeadLetter(payload)`, так что все три режима отказа направляются в dead-letter единообразно. Добавлен новый тест (`dataConversionFailureIsRoutedToDeadLetterAndConsumerKeepsRunning`), публикующий конверт `opened` с `version: 1` и непарсящейся датой `start`, подтверждающий, что он никогда не применяется, появляется буквально в dead-letter топике, и что консьюмер продолжает работать после (опубликованное сразу после валидное событие успешно подхватывается). Перепроверено самостоятельно (9/9 тестов слушателя проходят; воспроизведено вручную через `kafka-console-producer`/`-consumer` на чистом стеке).
- **Находка (Edge Case Hunter + Blind Hunter, сходящаяся):** `sendToDeadLetter`'s `kafkaTemplate.send(...)` отбрасывал возвращаемый future, поэтому отказ публикации в сам dead-letter топик был тихим. **Вердикт: patch (применён).** Объединено с тем же фиксом: `.whenComplete(...)` теперь логирует, если сама публикация в dead-letter не удалась.
- **Находка (Blind Hunter):** catch-блок непарсящегося payload логировал только `exception.getMessage()`, без объекта исключения (без стектрейса), в отличие от двух других веток. **Вердикт: patch (применён).** Объединено с тем же фиксом: теперь передаёт `exception` как последний аргумент лога, согласованно с другими ветками.
- **Находка (Edge Case Hunter):** событие, чей `eventType` не совпадает с топиком, в который оно опубликовано (например, payload в форме `MassIncidentClosed`, отправленный в топик `opened`), явно не проверяется. **Вердикт: поглощена фиксом выше, отдельного исправления не требуется.** Трассировка реального режима отказа показывает: несовпадающая форма провалит привязку `objectMapper.treeToValue` к собственной типизированной записи `data` слушателя (отсутствующие/неверные поля) — это именно путь отказа применения, только что исправленный; он не «тихо применяется как неверный тип», как было изначально сформулировано; теперь он направляется в dead-letter, как и любой другой отказ конвертации.
- **Находка (Blind Hunter):** матрица I/O и краевых случаев упускает строки «привязка с пустым `clientId` → 400», «повторно доставленный `opened` для уже известного id» и «`updated`/`closed` для неизвестного id», хотя все три реализованы, описаны в прозе Границ и ограничений, и покрыты отдельными тестами. **Вердикт: отклонена.** Фикс означал бы редактирование замороженной матрицы после одобрения ради пробела в полноте документации, а не функционального пробела — само поведение реализовано и протестировано корректно; конвенция матрицы как репрезентативных строк (установленная с Истории 2) никогда не задумывалась как исчерпывающее перечисление каждого случая, уже описанного в прозе Границ и ограничений.
- **Находка (Blind Hunter):** дифф добавляет `10-incident-case-mass-incidents.md` без видимого в области путей диффа файла-близнеца `.ru.md`. **Вердикт: false.** Русский дубликат существует на диске, создан вместе с одобрением спеки; он был за пределами области путей, с которой был сгенерирован дифф ревьюера, а не реальный пробел.
- **Находка (Blind Hunter):** не-4xx ветка `handleExceptionInternal` передаёт `exception.getMessage()`, в то время как отдельный catch-all `handleUnexpected` всегда возвращает общее сообщение — два пути 5xx расходятся в том, утекать ли внутренний текст. **Вердикт: defer.** Побайтово идентично обработчику ошибок каждого соседнего сервиса начиная с Истории 6; предсуществующее, не привнесено здесь.
- **Находка (Blind Hunter):** `MethodArgumentNotValidException` дал бы пустое сообщение, если у `BindingResult` есть только глобальные ошибки и ноль ошибок полей. **Вердикт: defer.** Предсуществует в унаследованной форме обработчика ошибок, идентична в каждом соседнем сервисе, у которого есть эта ветка; у `AttachClientRequest` нет ограничения уровня класса, поэтому недостижимо именно в этом сервисе — тот же вердикт, что у аналогичной находки Историй 7/9.
- **Находка (Blind Hunter):** маппинг каждого `statusCode.is4xxClientError()` в `VALIDATION_ERROR` означает, что несвязанный `404` (например, опечатка в маршруте) возвращается с этим кодом вместо `NOT_FOUND`. **Вердикт: defer.** Тот же предсуществующий, идентичный повсюду паттерн, уже отложенный в каждой предыдущей истории.
- **Находка (Blind Hunter):** повторная привязка перезаписывает `attached_at` последней отметкой времени, а не сохраняет исходную. **Вердикт: отклонена.** Спека требует только идемпотентности (без дублирующей строки, без ошибки), а не сохранения времени; «привязан в последний раз» — защищаемая, возможно более операционно полезная семантика, и никакое требование не нарушено.
- **Находка (Blind Hunter):** id группы консьюмера `"incident-case-service"` — литерал, дублированный в трёх аннотациях `@KafkaListener` и в `application.yml`, без общей константы. **Вердикт: отклонена.** Косметическая проблема сопровождения; несовпадение было бы немедленно обнаружено любым функциональным тестом (консьюмеры просто разделились бы на группы, а не упали бы или повредили данные) — не скрытый тихий риск, достойный раунда патча.
- **Находка (Edge Case Hunter):** конкурентные слушатели `updated`/`closed` (разные топики, разные потоки консьюмера), гонящиеся за одним id инцидента, могли бы потерять обновление; конкурентные дублирующие запросы привязки могли бы обогнать проверку существования в нарушение ограничения. **Вердикт: отклонены обе.** Та же диспозиция, что у аналогичной находки Истории 9 про конкурентность: реально в принципе, но несоразмерно для этого явно однооператорского, демо/прототип-масштабного бэкенда (уже зафиксировано на уровне архитектуры как «не производственного уровня»); фиксы (оптимистичная блокировка + повтор, или `SELECT ... FOR UPDATE`) добавляют значительную сложность для сценария, который объём этой истории не предвидит.

## Заметки по дизайну (Design Notes)

Это первый Kafka-потребляющий сервис в бэкенде, поэтому он несёт больше новой машинерии, чем любая предыдущая история: generic-конверт, оборачивающий типизированный `data` для каждого топика, явные декларации `NewTopic`, идемпотентная семантика upsert для доставки at-least-once, и маршрутизация в dead-letter для вперёд-несовместимого `version`. Dead-letter топик — единый общий `incident-case.mass-incident.dead-letter` (не один на каждый исходный топик), потому что AD-11 говорит «dead-letter топик» (в единственном числе), и человеку, расследующему плохие события, выгодно одно место для поиска независимо от того, какой из трёх топиков их произвёл. `matchesClient`, читающийся безусловно как `false`, — единственное пользовательски заметное изменение поведения этой истории относительно исходного статического мока (разрешено явно с пользователем во время планирования, а не выведено) — баннер-подсказка, зависевший от него, просто замолкает, пока не появится будущая, явно вне-объёмная история корреляции.

## Проверка (Verification)

**Команды:**
- `docker compose -f backend/docker-compose.yml build incident-case-service` -- ожидается: образ собирается
  - **Результат: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- ожидается: `incident-case-service`, его Postgres и `kafka` отчитываются healthy
  - **Результат: PASS.** Весь стек (gateway + все девять сервисов + Kafka + экземпляры Postgres) healthy.
- `curl -s http://localhost:8080/incident-case/incidents/mass` -- ожидается: `200 []` на чистом стеке
  - **Результат: PASS.**
- Опубликовать конверт `opened` в `incident-case.mass-incident.opened` (например, через `kafka-console-producer` в контейнере `kafka`) -- ожидается: последующий `curl` на том же эндпоинте показывает новый инцидент
  - **Результат: PASS.** Опубликовано через `kafka-console-producer.sh` для `MI-MANUAL-1`; последующий `GET` вернул его с `theme`/`product`/`start`/`status`/`affected`, точно совпадающими с опубликованной `data`, `start` как ISO-8601 (`"2026-09-12"`), и `matchesClient:false`.
- Опубликовать `updated`, затем `closed` конверт для того же `id` -- ожидается: каждый `curl` после этого отражает изменение
  - **Результат: PASS.** `updated` изменил `status`→`"Устраняется"` и `affected`→`"~4 000 клиентов"`, при этом `theme`/`product`/`start` остались без изменений; `closed` затем изменил `status`→`"Устранён"`, оставив `affected` ровно таким, каким его установил `updated`.
- Опубликовать конверт с неподдерживаемым `version` -- ожидается: он никогда не появляется через `curl`; `kafka-console-consumer` против `incident-case.mass-incident.dead-letter` показывает его
  - **Результат: PASS.** Опубликован конверт с `version:99` для `MI-MANUAL-BAD`; он никогда не появился в последующих `GET`, а `kafka-console-consumer.sh --from-beginning` против `incident-case.mass-incident.dead-letter` вернул этот самый конверт побайтово.
- `curl -s -X POST -H 'Content-Type: application/json' -d '{"clientId":"client-1"}' http://localhost:8080/incident-case/incidents/mass/{id}/attach` (дважды) -- ожидается: оба `200 {"ok":true}`
  - **Результат: PASS.** Оба вызова вернули `{"ok":true}`; прямой запрос `psql` к `mass_incident_attachments` после этого показал ровно одну строку для `(MI-MANUAL-1, client-1)`.
- `curl -i -X POST ... /incidents/mass/nope/attach` -- ожидается: `404`
  - **Результат: PASS.** `404`, тело `{"error":{"code":"NOT_FOUND","message":"Mass incident not found: nope"}}`.
- `curl -i -X PUT .../incidents/mass/{id}/attach` -- ожидается: `405`, присутствует заголовок `Allow`
  - **Результат: PASS.** `405`, `Allow: POST`, тело `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'PUT' is not supported"}}`.
- После патча, перепроверено самостоятельно: опубликовать событие с валидной версией и некорректной `data` (`"start":"not-a-date"`) -- ожидается: никогда не применяется, появляется буквально в dead-letter топике, консьюмер продолжает работать
  - **Результат: PASS.** `GET` никогда не показал некорректный инцидент; `kafka-console-consumer` против dead-letter топика показал его побайтово; валидное событие, опубликованное сразу после, было подхвачено нормально.
- Набор тестов модуля -- ожидается: все проходят
  - **Результат: PASS.** `MassIncidentEventListenerIntegrationTest`: `Tests run: 9, Failures: 0, Errors: 0` (opened/повторно-доставленный-opened/updated/updated-неизвестный/closed/closed-неизвестный/плохая-версия-никогда-не-применяется/плохая-версия-в-dead-letter-буквально/отказ-конвертации-данных-в-dead-letter, против настоящего Testcontainers-брокера `apache/kafka:4.3.1` + Postgres — 9-й тест добавлен патчем после ревью). `MassIncidentControllerIntegrationTest`: `Tests run: 6, Failures: 0, Errors: 0` (пустой список, форма известного инцидента, привязка-дважды-идемпотентна, привязка-неизвестного-404, пустой-clientId-400, неверный-метод-405). 15/15 всего, перепроверено самостоятельно после патча. Стек остановлен после ручной верификации.
