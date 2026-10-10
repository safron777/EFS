---
title: 'История 6 — CAP-6: История операций'
type: 'feature'
created: '2026-10-10'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: 'aaeaf72e5046a018f1b5d4f4499611eb1896094a'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<!-- Русская версия — перевод 6-operations.md для удобства чтения; при расхождении английский оригинал остаётся источником истины и единственным файлом, который читает воркфлоу bmad-build. -->

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Суть (Intent)

**Проблема:** `operations-service` (История 0) имеет только health-эндпоинт. CAP-6 нужно обслужить вызов `getOperations(cardId)` фронтенда историей операций по карте, включая алерт о неуспешном переводе, если он есть.

**Подход:** Реализовать схему БД операций через Liquibase (AD-12) и hexagonal-срез `GET /cards/{cardId}/operations`, засеянный из `mockData.operations` — ограниченный карточкой `card-4019` (активная карта client-1 по сиду Истории 3) — с уже установленными правками формата: копейки по AD-3 и ISO-8601 для дат, но с сознательным сохранением `status` как литеральной русской строки мока, в отличие от всех остальных нормализованных полей.

## Границы и ограничения (Boundaries & Constraints)

**Всегда:**
- Форма ответа `{failedAlert: {title, text} | null, rows: [{id, date, op, sum, status}]}` на `GET /cards/{cardId}/operations`, зеркалящая `mockData.operations` — AD-8.
- `sum` — знаковое целое число в копейках (AD-3) — мок `"−5 000 ₽"` / `"+450 ₽"` становится `-500000` / `45000`, никогда отформатированной строкой.
- `date` — ISO-8601. У мока `"12.09"` нет года; встроенная в `failedAlert.text` дата `"12.09.2026"` закрепляет год для всех пяти строк (все они попадают в один сентябрь) — `"2026-09-12"` и т. д.
- `status` остаётся точной литеральной русской строкой мока (`"Отклонён"` / `"Проведена"` / `"В обработке"`) без изменений — карта `STATUS_TONE` в `src/components/ui/Pill.jsx` ищет тон по этой точной строке, поэтому нормализация в код-перечисление молча сломала бы подбор тона на фронтенде.
- Строки упорядочены от новых к старым (`date` по убыванию), как в моке.
- Операции ссылаются на карту только по `cardId` (AD-2) — никакого межсервисного вызова к `card-account-service` для проверки (AD-9).
- `@RestControllerAdvice` этого сервиса пишется в уже финальной форме с самого начала (наследует `ResponseEntityExceptionHandler`, переопределяет `handleExceptionInternal` с вызовом `.headers(headers)`, использует `statusCode.is4xxClientError()`/`is5xxServerError()` напрямую, формирует чистое сообщение для `MethodArgumentNotValidException`) — форма, до которой каждому другому сервису пришлось доходить тремя отдельными ретроактивными фиксами. Копировать именно эту финальную форму, а не промежуточную.

**Никогда:**
- Никаких write/мутационных эндпоинтов — критерий успеха CAP-6 чисто read-only.
- Никакого `NOT_FOUND` для неизвестного `cardId` — это эндпоинт в форме коллекции; любой `cardId` без засеянной истории возвращает `200` с `{failedAlert: null, rows: []}` — тот же прецедент 200-пустого-ответа, что История 3 установила для несовпавшего `clientId`/`productId`.
- Только `card-4019` получает фикстуру строк + алерт; `card-7274` (вторая засеянная карта Истории 3) и любой другой `cardId` получают пустую форму — история операций по сути своей привязана к конкретной карте, в отличие от предложения Истории 2, одинакового для всех клиентов. Это сознательное решение по объёму сидирования, а не пробел.

## Матрица ввода-вывода и краевых случаев (I/O & Edge-Case Matrix)

| Сценарий | Вход / состояние | Ожидаемый результат | Обработка ошибок |
|----------|--------------|---------------------------|----------------|
| Известная карта с историей | `GET /cards/card-4019/operations` | `200`, `failedAlert` + 5 строк, совпадающих с `mockData.operations` с поправками на копейки/ISO | N/A |
| Известная карта, без истории | `GET /cards/card-7274/operations` | `200`, `{failedAlert: null, rows: []}` | N/A |
| Неизвестная карта | `GET /cards/nope/operations` | `200`, `{failedAlert: null, rows: []}` | N/A |
| Не-GET метод | `POST /cards/card-4019/operations` | — | `405` с конвертом ошибки спайна, присутствует заголовок `Allow` |

</frozen-after-approval>

## Карта кода (Code Map)

- `ARCHITECTURE-SPINE.md` AD-1 — `operations` владеет историей операций по карте; AD-2 — ссылка только по `cardId`; AD-3 — деньги как целое число копеек; AD-8 — REST-форма зеркалит `client.js`; AD-9 — никаких синхронных межсервисных вызовов; AD-12 — миграции Liquibase
- Экспорт `operations` из `src/api/mockData.js` — точная фикстура; `"12.09.2026"` внутри `failedAlert.text` — то, что закрепляет год для всех 5 дат строк
- `getOperations(cardId)` из `src/api/client.js` → `GET /cards/{cardId}/operations`
- `STATUS_TONE` в `src/components/ui/Pill.jsx` — доказательство, что `status` должен оставаться литеральной русской строкой
- `backend/card-account-service/` (История 3) — соседний прецедент по слою сущность/JPA-репозиторий, разбивке чейнджсетов `001-create-schema` + `002-seed`, и уже исправленной форме `CardExceptionHandler` (копировать буквально, поменяв пакет)
- `backend/operations-service/` (История 0) — существующий пустой скелет `pom.xml`/`application.yml`/package-info для застройки
- `backend/gateway/src/main/resources/application.yml` — существующий маршрут `/operations/**` (`StripPrefix=1`) уже проксирует сюда; изменений в gateway не требуется

## Задачи и приёмка (Tasks & Acceptance)

**Выполнение:**
- [x] `db/changelog/changes/001-create-operations-schema.xml` -- создать таблицы `operations` (id, card_id, op_date, description, amount_minor_units, status) и `operation_alerts` (card_id PK, title, text) -- AD-12; две таблицы, поскольку алерт — суб-агрегат 0-или-1-на-карту, а не поле строки
- [x] `db/changelog/changes/002-seed-operations-card-4019.xml` -- засеять 5 строк `mockData.operations.rows` для `card-4019` + одну строку `operation_alerts` -- для `card-7274` ничего не сидировать
- [x] `domain/Operation.java`, `domain/FailedTransferAlert.java`, `domain/OperationsHistory.java` -- framework-free value objects; `OperationsHistory` агрегирует необязательный алерт + упорядоченные строки
- [x] `domain/OperationsRepository.java` -- порт, `OperationsHistory getHistory(String cardId)`
- [x] `application/GetOperationsUseCase.java` -- оркестрирует порт
- [x] `adapters/out/persistence/OperationEntity.java`, `OperationAlertEntity.java`, их Spring Data интерфейсы, `JpaOperationsRepository.java` -- маппит обе таблицы, собирает `OperationsHistory`, упорядочивает строки по `op_date DESC`
- [x] `adapters/in/web/OperationsController.java` + DTO ответа -- `GET /cards/{cardId}/operations`
- [x] `adapters/in/web/OperationsExceptionHandler.java` + `ErrorResponse.java` -- финальная форма с самого начала (см. Границы)
- [x] `src/test/.../OperationsControllerIntegrationTest.java` -- покрывает все четыре строки матрицы I/O на реальном Testcontainers Postgres

**Критерии приёмки:**
- Дан сид, когда вызывается `GET /cards/card-4019/operations` напрямую на сервисе, тогда `200` с `failedAlert` + 5 строками, `sum` знаковыми копейками, `date` ISO-8601, `status` без изменений, от новых к старым.
- Дана `card-7274`, когда вызывается `GET /cards/card-7274/operations`, тогда `200` с `{failedAlert: null, rows: []}`.
- Дан поднятый через `docker compose up -d --wait` стек, когда вызывается `GET http://localhost:8080/operations/cards/card-4019/operations` через Gateway, тогда приходит тот же `200`.
- Дан не-`GET` метод, когда отправляется `POST`, тогда `405` с конвертом ошибки спайна и заголовком `Allow`.

## Заметки по реализации (Implementation Notes)

- Разбивка на две таблицы (`operations`/`operation_alerts`), форма агрегата `OperationsHistory` и финальная форма `OperationsExceptionHandler` (скопирована буквально из `IssuanceExceptionHandler` card-issuance-service) легли точно по плану из Карты кода/Задач — без отклонений.
- Комментарии Liquibase-чейнджлога на этот раз написаны аккуратно, без буквального `--` внутри блоков `<!-- -->` (баг, который кусал Истории 1, 3, 4 и 5) — не повторился.
- **Патч после ревью:** у `OperationJpaRepository.findByCardIdOrderByOpDateDesc` не было вторичного ключа сортировки, поэтому две операции с одной датой `op_date` имели бы неопределённый относительный порядок, несмотря на заявление в Javadoc о «детерминированности». Переименован в `findByCardIdOrderByOpDateDescIdAsc` (добавлен `id` по возрастанию как тайбрейкер) с обновлением единственной точки вызова в `JpaOperationsRepository`. У сегодняшней фикстуры все 5 дат различны, поэтому проблема никак не проявлялась визуально, но фикс убирает скрытую недетерминированность, а не оставляет её поджидать будущий сид.
- Независимая сквозная перепроверка (чистый `docker compose up -d --wait`, все 20 контейнеров) выполнена как до, так и после патча: все 4 строки матрицы I/O подтверждены через `curl` через Gateway, плюс схема таблиц `operations`/`operation_alerts` проверена напрямую в Postgres (`\d`) — PK/индекс/колонки совпадают со спекой точно. Набор тестов модуля перепроверен независимо в одноразовом контейнере `eclipse-temurin:25` (хостовый JDK — 17, по Docker-only ограничению проекта): `Tests run: 4, Failures: 0, Errors: 0`.

## Журнал изменений спеки (Spec Change Log)

## Журнал триажа ревью (Review Triage Log)

- **Находка (Blind Hunter):** в диффе нет файла-близнеца `6-operations.ru.md`. **Вердикт: false.** Русский дубликат существует на диске (создан вместе с одобрением спеки); он просто оказался за пределами области путей, с которой был сгенерирован дифф ревьюера (`backend/operations-service` + только английский `.md`), а не реальный пробел.
- **Находка (Blind Hunter):** ветка `else if (statusCode.is4xxClientError())` в `OperationsExceptionHandler.handleExceptionInternal` передаёт клиенту необработанный `exception.getMessage()`, в отличие от ветки `MethodArgumentNotValidException` выше неё. **Вердикт: medium, если реально, но defer.** Побайтово идентично уже отгруженным `ClientExceptionHandler`/`OfferExceptionHandler`/`CardExceptionHandler`/`IssuanceExceptionHandler` во всех четырёх предыдущих сервисах — замороженные Границы и ограничения этой истории явно требуют копировать именно эту финальную форму, а не изобретать более строгую. Предсуществующее во всём кодобазе, не вызвано этой историей.
- **Находка (Blind Hunter):** ни один тест не прогоняет общую ветку `INTERNAL_ERROR`/500 или `handleUnexpected`. **Вердикт: low, defer.** Тот же пробел идентично существует у всех соседних сервисов (ни у одного из их read-only срезов нет естественного триггера для необработанного исключения); не привнесено здесь.
- **Находка (Blind Hunter):** значения `op_date` в сиде `002-seed-operations-card-4019.xml` вручную выведены из года, встроенного в `failedAlert.text`, без автоматической проверки синхронизации, если один из них отредактируют позже. **Вердикт: low, отклонена.** Маловероятно, что разработчик отредактирует одно без другого в статическом, уже отгруженном файле сида; защита синхронизации — больше, чем прямое исправление.
- **Находка (Blind Hunter):** `OperationJpaRepository.findByCardIdOrderByOpDateDesc` не имел вторичного ключа сортировки, поэтому заявление Javadoc о «детерминированном новые-сначала» порядке держалось только потому, что у сегодняшних 5 строк фикстуры разные даты. **Вердикт: low, patch.** Тривиальный прямой фикс: добавить `id` как вторичный ключ.
- **Находка (Blind Hunter):** ни одна засеянная фикстура/тест не покрывает «у карты есть строки, но нет алерта» — самую распространённую реальную форму по собственному Javadoc `OperationsHistory`. **Вердикт: отклонена.** Поиск алерта (`findById` в `operation_alerts`) и запрос строк — полностью независимые, безветвящиеся пути кода, уже проверенные раздельно (строки+алерт вместе в тесте card-4019, ни то ни другое в пустых тестах); третья засеянная карта добавила бы полноту покрытия, а не отловила бы правдоподобный баг, а замороженные Границы и ограничения явно ограничивают фикстуру только `card-4019` — добавление новой засеянной карты противоречит этому сознательному решению по объёму.
- **Находка (Blind Hunter):** ни один тест не подтверждает, что полностью немаппированный путь (без совпадающего маршрута вообще, в отличие от совпавшего маршрута с неизвестным `cardId`) всё же возвращает JSON-конверт ошибки спайна, а не страницу ошибки Spring Boot по умолчанию. **Вердикт: medium, если реально, но defer.** Это вопрос обработки ошибок Spring Boot по умолчанию, идентичный во всех девяти скелетах сервисов; не привнесён и не затронут диффом этой истории.
- **Находка (Blind Hunter):** ни один тест не покрывает краевой `cardId` (очень длинный, специальные/зарезервированные-в-URL символы, не-ASCII), хотя история обещает `200` для «любого `cardId`». **Вердикт: false.** `cardId` течёт исключительно в параметризованный параметр JPA-запроса (`findByCardIdOrderByOpDateDescIdAsc(String)` / `findById(String)`) — никогда не конкатенируется и не парсится, — поэтому нет пути кода, который мог бы вести себя иначе для необычных символов; уже существующий тест «неизвестная карта» уже доказывает общее поведение пустого результата для любого значения, не присутствующего в таблице.
- **Находка (Edge Case Hunter):** нет находок (пустой список).
- **Находка (Verification Gap):** нет находок — подтверждено, что все 4 строки матрицы I/O покрыты `OperationsControllerIntegrationTest`, прогоняются на реальном Testcontainers Postgres и проходят.

## Заметки по дизайну (Design Notes)

Разбивка на две таблицы (строки `operations` vs. `operation_alerts`) избегает nullable-колонок алерта на каждой строке и зеркалит прецедент Истории 3 — отдельные таблицы для отдельных понятий на одном `cardId` (`cards` vs. `documents`). Через Gateway путь читается как `.../operations/cards/{cardId}/operations` — «operations» встречается дважды (один раз как маршрутизирующий префикс Gateway, обрезаемый перед пересылкой; один раз как сам ресурсный сегмент, зеркалящий `client.js`) — та же случайная избыточность, которую История 2 уже приняла для `offers`, не дефект для исправления здесь.

## Проверка (Verification)

**Команды:**
- `docker compose -f backend/docker-compose.yml build operations-service` -- ожидается: образ собирается
  - **Результат: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- ожидается: `operations-service` и его Postgres отчитываются healthy
  - **Результат: PASS.** Весь стек из 20 контейнеров healthy, как до, так и после патча после ревью. Схема `operation_alerts`/`operations` проверена напрямую через `psql \d`: PK на `operations.id`, индекс `ix_operations_card_id`, PK на `operation_alerts.card_id` — точно совпадает со спекой.
- `curl -s http://localhost:8080/operations/cards/card-4019/operations` -- ожидается: `200`, совпадает с `mockData.operations` с поправками на копейки/ISO
  - **Результат: PASS.** `failedAlert` + 5 строк, от новых к старым, копейки со знаком верно (`-500000`…`+45000`), даты `2026-09-08`…`2026-09-12`, `status` — неизменённые русские строки.
- `curl -s http://localhost:8080/operations/cards/card-7274/operations` -- ожидается: `200`, `{"failedAlert":null,"rows":[]}`
  - **Результат: PASS.**
- `curl -s http://localhost:8080/operations/cards/nope/operations` -- ожидается: та же пустая форма `200` для полностью неизвестной карты (не было в исходном списке команд, добавлено самостоятельно)
  - **Результат: PASS.**
- `curl -i -X POST http://localhost:8080/operations/cards/card-4019/operations` -- ожидается: `405`, присутствует заголовок `Allow`
  - **Результат: PASS.** `Allow: GET`, тело `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'POST' is not supported"}}`.
- Набор тестов модуля -- ожидается: все проходят
  - **Результат: PASS.** `Tests run: 4, Failures: 0, Errors: 0` — перепроверено самостоятельно в одноразовом контейнере `eclipse-temurin:25` на реальном Testcontainers Postgres, как до, так и после патча с тайбрейкером сортировки после ревью. Стек остановлен после проверки.
