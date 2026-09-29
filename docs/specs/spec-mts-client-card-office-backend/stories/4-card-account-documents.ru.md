---
title: 'История 4 — CAP-4: Документы по счёту'
type: 'feature'
created: '2026-09-29'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<!-- Русская версия — перевод 4-card-account-documents.md для удобства чтения; при расхождении английский оригинал остаётся источником истины и единственным файлом, который читает воркфлоу bmad-build. -->

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Суть (Intent)

**Проблема:** `card-account-service` (Истории 0/3) обслуживает карты, но не документы. CAP-4 (расширен во время планирования этой истории — см. memlog спеки) нужно обслужить вызовы фронтенда `getDocuments`, `sendDocument`, `printDocument`: список документов клиента по счёту и возможность отправить или распечатать документ.

**Подход:** Добавить таблицу `documents` через новый Liquibase-чейнджсет (`003-...`, никогда не редактируя `001`/`002` — урок про неизменяемость чейнджсетов из Истории 1), засеянную тремя статичными документами из `mockData.documents`, плюс три эндпоинта, точно зеркалящих `client.js`: `GET /clients/{clientId}/cards/{cardId}/documents`, `POST /clients/{clientId}/documents/{documentId}/send`, `POST /clients/{clientId}/documents/{documentId}/print`.

## Границы и ограничения (Boundaries & Constraints)

**Всегда:**
- Ответ `GET` — JSON-массив, зеркалящий `mockData.documents` (`id`, `title`, `subtitle`) — AD-8. Мок безусловен (одни и те же 3 документа независимо от клиента/карты, как offer в Истории 2), поэтому seed глобальный, не по клиенту/карте — то же упрощение, что уже сделала История 2 по той же причине.
- `send`/`print` возвращают `{"ok": true}` при успехе, точно соответствуя существующему stub-контракту `client.js`.
- `send`/`print` на неизвестный `documentId` возвращают `404` с `{"error":{"code":"NOT_FOUND",...}}` — реальная проверка корректности, которой у самого мока никогда не было (он stateless), по аналогии с прецедентом Истории 1 — добавлять реальный путь «не найдено» сверх того, что упражняет мок.
- Только новый Liquibase-чейнджсет (`003-create-documents-and-seed.xml` или дробнее) — `001`/`002` из Истории 3 никогда не редактируются.
- У этого сервиса уже есть исправленный `CardExceptionHandler extends ResponseEntityExceptionHandler` (История 3, после фикса) — переиспользовать его для нового маппинга `NOT_FOUND`, не создавать второй advice-класс.

**Никогда:**
- Никакой валидации/ветвления по `channel` в `send` — `client.js` всегда вызывает с дефолтным `"email"`, и ни один UI-элемент его не варьирует; принять опциональное поле `channel` в теле запроса без проверки значения.
- Никакой перекрёстной проверки, что `documentId` принадлежит данному `clientId`/`cardId` из пути — документы глобальны в этом demo (см. выше), поэтому `clientId`/`cardId` из пути принимаются как есть (AD-9 тоже исключает межсервисную проверку в любом случае).
- Никакой реальной генерации документов/PDF-контента, никакой реальной отправки email/печати — `send`/`print` остаются stub-действиями, возвращающими `{ok: true}`, соответствуя моку; отложено вместе со всеми остальными stub-адаптерами внешних систем в спайне.

## Матрица ввода-вывода и краевых случаев (I/O & Edge-Case Matrix)

| Сценарий | Вход / состояние | Ожидаемый результат | Обработка ошибок |
|----------|--------------|---------------------------|----------------|
| Список документов | `GET /clients/client-1/cards/card-4019/documents` | `200`, массив из 3 документов, соответствующий `mockData.documents` | N/A |
| Отправка известного документа | `POST /clients/client-1/documents/doc-statement/send` | `200`, `{"ok": true}` | N/A |
| Печать известного документа | `POST /clients/client-1/documents/doc-statement/print` | `200`, `{"ok": true}` | N/A |
| Отправка/печать неизвестного документа | `POST /clients/client-1/documents/nope/send` | — | `404` с конвертом ошибки |

</frozen-after-approval>

## Карта кода (Code Map)

- `ARCHITECTURE-SPINE.md` AD-1 — `card-account` владеет документами по счёту (уже названо в спайне)
- `ARCHITECTURE-SPINE.md` AD-8 — REST-форма ресурса зеркалит `client.js`
- `ARCHITECTURE-SPINE.md` AD-12 — Liquibase; чейнджсеты неизменяемы после отгрузки (урок Истории 1 — см. Implementation Notes той истории про точный сценарий отказа при правке на месте)
- `docs/specs/spec-mts-client-card-office-backend/SPEC.md` CAP-4 (расширен в этой истории) и запись в её `.memlog.md`, фиксирующая пробел/решение
- Экспорт `documents` из `src/api/mockData.js` — точная форма и значения фикстуры
- `getDocuments(clientId, cardId)`, `sendDocument(clientId, documentId, channel)`, `printDocument(clientId, documentId)` из `src/api/client.js` — три вызова, которые обслуживает эта история
- `src/pages/BalancePage.jsx` — подтверждает, что `sendDocument`/`printDocument` — живые, подключённые UI-действия (не мёртвый код), и что фронтенд никогда не варьирует `channel` от дефолта
- `src/components/shared/DocRow.jsx` — подтверждает, что `onSend`/`onPrint` — простые fire-and-forget вызовы, без UI для тела запроса
- `backend/card-account-service/` (История 3) — существующие `Card`/`CardController`/`CardExceptionHandler` (исправленная форма) и чейнджсеты `001`/`002`, которые нужно расширить, не редактировать
- `backend/gateway/src/main/resources/application.yml` — существующий маршрут `/card-account/**` уже проксирует сюда; изменений в gateway не требуется

## Задачи и приёмка (Tasks & Acceptance)

**Выполнение:**
- [x] `.../domain/Document.java` -- framework-free сущность (`id`, `title`, `subtitle`), порт `DocumentRepository` (`List<Document> findAll()`, `boolean existsById(String id)`)
- [x] `.../application/GetDocumentsUseCase.java`, `SendDocumentUseCase.java`, `PrintDocumentUseCase.java` -- последние два кидают доменное `DocumentNotFoundException` на неизвестном id
- [x] `.../adapters/in/web/DocumentController.java` + DTO ответов (`DocumentResponse[]`, `{"ok":true}`) -- `GET /clients/{clientId}/cards/{cardId}/documents`, `POST /clients/{clientId}/documents/{documentId}/send`, `POST /clients/{clientId}/documents/{documentId}/print`
- [x] `CardExceptionHandler` -- добавить `@ExceptionHandler(DocumentNotFoundException.class)`, маппящий в `404`/`NOT_FOUND`, рядом с существующей обработкой фреймворковых исключений
- [x] `.../adapters/out/persistence/` -- JPA-сущность, Spring Data репозиторий, адаптер, реализующий доменный порт
- [x] `backend/card-account-service/src/main/resources/db/changelog/changes/003-create-documents-and-seed.xml` -- новый чейнджсет (никогда не трогая `001`/`002`), подключённый из `db.changelog-master.xml`, создающий `documents` + засевающий три из `mockData.documents`
- [x] `backend/card-account-service/src/test/java/.../DocumentControllerIntegrationTest.java` -- `@SpringBootTest(webEnvironment = RANDOM_PORT)` против Testcontainers Postgres, покрывает все четыре строки I/O-матрицы

**Критерии приёмки:**
- [x] Даны засеянные документы, когда вызывается `GET /clients/client-1/cards/card-4019/documents`, тогда ответ `200` со всеми 3 документами, точно соответствующими `mockData.documents`.
- [x] Дан известный `documentId`, когда вызывается `POST .../documents/doc-statement/send` или `.../print`, тогда ответ `200` с `{"ok": true}`.
- [x] Дан неизвестный `documentId`, когда вызывается `POST .../documents/nope/send`, тогда ответ `404` с конвертом ошибки и `code: "NOT_FOUND"`.
- [x] Дан поднятый через `docker compose up -d --wait` стек (compose-файл без изменений), когда те же запросы делаются через Gateway, тогда приходят те же ответы.
- [x] Дан `docker compose up -d --wait` с чистого тома, когда `card-account-service` стартует, тогда и схема `cards` из Истории 3, и схема `documents` этой истории применяются чисто за один запуск (доказывает, что новый чейнджсет не конфликтует с существующими и не инвалидирует их).

## Заметки по реализации (Implementation Notes)

- `sort_order` добавлен в `DocumentEntity`/таблицу `documents` (не упомянут в списке задач истории, где для доменного `Document` названы только `id`/`title`/`subtitle`), потому что порядок массива `mockData.documents` — выписка, справка об остатке, справка о движении — не алфавитный по `id` и не выводится из полей самой строки, в отличие от `Card`, который смог переиспользовать свой осмысленный флаг `is_main` для воспроизведения порядка «основная карта первой» (собственное решение по упорядочиванию Истории 3). Без явной колонки упорядочивания неупорядоченный `SELECT` в Postgres не даёт гарантии совпадения с порядком мока, а формулировка I/O-матрицы «массив из 3 документов, соответствующий mockData.documents» читается как включающая порядок (то же чтение, что История 3 применила к своему двухстрочному случаю). `sort_order` остаётся только в персистентности — доменный `Document` и `DocumentResponse` сохраняют ровно три поля, названных в истории.
- `CardExceptionHandler` расширен, а не заменён: `DocumentNotFoundException` получает собственный метод `@ExceptionHandler` (зеркалящий маппинг `ClientNotFoundException` из `client-identity-service`) рядом с существующими `handleExceptionInternal` (405/415/и т.д.) и catch-all обработкой `Exception`, добавленными в Истории 3 уже в исправленной, post-fix форме (исходный баг Историй 1/2 с голым `@ExceptionHandler(Exception.class)` здесь не воспроизведён).
- `channel` в теле запроса `send` принимается через опциональную запись `SendDocumentRequest` (`@RequestBody(required = false)`), которая десериализуется, но никогда не читается use case'ом, соответствуя явной инструкции истории «принять без проверки»; запрос вовсе без тела и запрос с нераспознанным значением `channel` оба завершаются успешно одинаково.
- Поймано до того, как вообще выполнилось: черновик заголовочного комментария `003-create-documents-and-seed.xml` использовал `--` внутри XML-комментария `<!-- -->` (стиль документации, уже используемый в Javadoc этого репозитория). XML запрещает `--` внутри комментариев; парсер Liquibase отверг чейнджлог целиком (`"databaseChangeLog" expected as root element`). Это ровно та ловушка, которую уже называют Заметки по реализации Истории 3 как воспроизведённую там, несмотря на прочтение заметки Истории 1 заранее — воспроизведена здесь в третий раз, исправлена тем же способом: переформулирован комментарий (запятые/точки с запятой вместо `--`), а не обходом парсера.

## Журнал изменений спеки (Spec Change Log)

## Журнал триажа ревью (Review Triage Log)

- **Импорты `DocumentController` не в алфавитном порядке** — `low` — Проверено: `PathVariable`/`PostMapping`/`RequestBody` идут перед `GetMapping`, нарушая строго-алфавитную конвенцию `CardController`. → **patch**: переупорядочить.
- **`clientId`/`cardId` связаны, но нигде не используются, и ничто в точке вызова не отличает «сознательно проигнорировано» от «забыли подключить»** — `low` — Проверено: ни один метод на них не ссылается; собственная секция Boundaries истории документирует это как намеренное (документы глобальны), но этот контекст не виден в самом коде. → **patch**: добавить короткий комментарий/Javadoc о том, что они намеренно неиспользуемы по дизайну глобальных документов.
- **Нет теста, демонстрирующего, что GET с несвязанным/несуществующим `clientId`/`cardId` всё равно возвращает все 3 документа** — `low` — Проверено: все существующие тесты используют `client-1`/`card-4019`; задокументированный дизайн «документы глобальны, без перекрёстной проверки» ничем не закреплён тестом, так что будущая регрессия могла бы молча сузить набор результатов без падения ни одного теста. → **patch**: добавить один тестовый вызов с другим/несуществующим `clientId`/`cardId`, проверяющий тот же ответ с 3 документами.
- **У `DocumentEntity` нет явного `@Column(length=...)`, соответствующего границам схемы Liquibase (`varchar(64)`/`varchar(255)`)** — `low` — Проверено: сущность — естественное место, где будущий читатель искал бы предполагаемый размер поля, и она об этом не сообщает. → **patch**: добавить атрибуты length.
- **`SendDocumentUseCase`/`PrintDocumentUseCase` почти дублируют друг друга (один и тот же однострочный guard существования)** — `low`, отклонено — Собственные задачи истории просили три отдельных use case, зеркалящих три REST-действия (по прецеденту «один use case на действие» из Истории 1); дублируемая логика — единственный guard-clause, и заводить под него общую абстракцию — больше механики, чем оправдывает дублирование.
- **`DocumentController` не выносит общий префикс `@RequestMapping`, как это делает `CardController`** — `low`, отклонено — У `CardController` один эндпоинт под одним префиксом; три эндпоинта `DocumentController` охватывают два разных логических префикса (`.../cards/{cardId}/documents` для списка, `.../documents/{documentId}/...` для send/print), которые не сводятся к одному чистому class-level mapping — явные пути здесь понятнее, а не несогласованность.
- **Нет отдельных unit-тестов на use case'ы** — `low`, отклонено — Тот же вывод, что и в Истории 3: ветвление (единственная проверка существования) уже упражняется end-to-end через выделенный тест «не найдено» в интеграционном тесте.
- **`ErrorResponse.details` задокументирован, но никогда не заполняется** — `low`, отклонено — Ни один вызывающий код ни в одной истории пока не нуждается в этом; добавлять payload, который никто не потребляет, преждевременно.
- **Нет unique-ограничения на `documents.sort_order`** — `low`, отклонено — Таблица полностью засеяна одним Liquibase-чейнджсетом, никакого пути вставки нигде в системе пока не существует; защищаться от правки руками, которая сейчас невозможна, преждевременно.
- **В I/O-матрице нет строки для несвязанного/несуществующего `clientId`/`cardId` на GET** — отклонено — Её фикс означал бы правку замороженного блока спеки, что запрещено собственным правилом воркфлоу маршрутизировать в patch; само поведение покрыто новым тестом выше.

## Заметки по дизайну (Design Notes)

Документы смоделированы как глобальный, неcкоуп­ленный список (без внешнего ключа `clientId`/`cardId`), потому что сам `mockData.documents` безусловен — одни и те же три документа независимо от того, какой клиент или карта их запросили. Это зеркалит решение Истории 2 считать данные offer-фикстуры клиенто-независимыми по той же причине: мок не варьируется, так что колонка скоупинга моделировала бы различие, которого данные пока реально не делают.

## Проверка (Verification)

**Команды:**
- `docker compose -f backend/docker-compose.yml build card-account-service` -- ожидается: образ собирается
  - **Результат: PASS.** Образ собран без ошибок.
- `docker compose -f backend/docker-compose.yml down -v && docker compose -f backend/docker-compose.yml up -d --wait` -- ожидается: чистый старт с тома, применяются чейнджсеты и Истории 3, и Истории 4, `card-account-service` healthy
  - **Результат: PASS.** Весь стек из 20 контейнеров сообщил healthy. Логи подтверждают, что все четыре чейнджсета выполнились за один проход: `001-create-cards-table`, `002-seed-cards-client-1`, `003-create-documents-table`, `003-seed-documents` — «UPDATE SUMMARY ... Run: 4 ... Total change sets: 4», затронуто строк: 5.
- `curl -s http://localhost:8080/card-account/clients/client-1/cards/card-4019/documents` -- ожидается: `200`, 3 документа, соответствующих `mockData.documents`
  - **Результат: PASS.** `[{"id":"doc-statement","title":"Выписка по счёту •••• 4019","subtitle":"За последние 30 дней · PDF"},{"id":"doc-balance-cert","title":"Справка об остатке денежных средств","subtitle":"На сегодняшнюю дату"},{"id":"doc-flow-cert","title":"Справка о движении средств","subtitle":"За произвольный период, для налоговой/визы"}]` — HTTP 200, все 3 строки в собственном порядке `mockData.documents`.
- `curl -s -X POST http://localhost:8080/card-account/clients/client-1/documents/doc-statement/send` -- ожидается: `200`, `{"ok":true}`
  - **Результат: PASS.** `{"ok":true}`.
- `curl -s -X POST http://localhost:8080/card-account/clients/client-1/documents/doc-statement/print` -- ожидается: `200`, `{"ok":true}`
  - **Результат: PASS.** `{"ok":true}`.
- `curl -s -w '%{http_code}' -X POST http://localhost:8080/card-account/clients/client-1/documents/nope/send` -- ожидается: `404`
  - **Результат: PASS.** `404` с телом `{"error":{"code":"NOT_FOUND","message":"Document not found: nope"}}`. Дополнительно проверено то же для `.../nope/print` (не было в исходном списке команд): идентичный `404`/`NOT_FOUND`.
- Набор тестов модуля -- ожидается: все проходят
  - **Результат: PASS.** `mvn -pl card-account-service -am test`: `CardControllerIntegrationTest` (4 теста, уже существовали, не затронуты) + `DocumentControllerIntegrationTest` (6 тестов: список соответствует моку, send/print известного, send/print неизвестного 404, опциональное тело с channel принимается) — всего `Tests run: 10, Failures: 0, Errors: 0`, против реального Testcontainers Postgres с фактически применённой через Liquibase схемой/seed. Также повторно проверен `GET /clients/client-1/products/debit-mts-dengi/cards` (эндпоинт Истории 3) через Gateway в рамках того же запуска стека — не затронут. Стек остановлен после проверки командой `docker compose down -v`.
- Постпатчевая независимая перепроверка (не только по отчёту реализации): пересборка, снова `down -v` + `up -d --wait` с чистого тома (все 4 чейнджсета применились чисто), повторно проверены те же 4 curl-сценария плюс новый случай `GET /clients/nope/cards/nope/documents` из добавленного теста.
  - **Результат: PASS.** Идентичное тело из 3 документов и для `client-1`/`card-4019`, и для `nope`/`nope`, подтверждая гарантию «документы глобальны» независимо от параметров пути; поведение `send`/`404` и эндпоинт карт Истории 3 не затронуты.
