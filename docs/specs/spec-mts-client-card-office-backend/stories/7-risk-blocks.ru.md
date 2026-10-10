---
title: 'История 7 — CAP-7: Блокировки по рискам'
type: 'feature'
created: '2026-10-10'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '84368164e22bb34cbcdc4329168913abaa107122'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<!-- Русская версия — перевод 7-risk-blocks.md для удобства чтения; при расхождении английский оригинал остаётся источником истины и единственным файлом, который читает воркфлоу bmad-build. -->

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Суть (Intent)

**Проблема:** `risk-blocks-service` (История 0) имеет только health-эндпоинт. CAP-7 нужно обслужить вызов `getBlocks(cardId)` фронтенда статусом блокировок СФМ / арест ФССП / антифрод, каждая — через собственный фасад над внешней системой.

**Подход:** Реализовать `GET /cards/{cardId}/blocks` как три независимых AD-10 stub-порта (`SfmPort`, `FsspPort`, `AntifraudPort`), каждый со своим stub-адаптером в уже засеянных Историей 0 пакетах `adapters/out/external/{sfm,fssp,antifraud}/`, собранные в один ответ — зеркалируя уже отгруженный offers-service'ом паттерн с одним stub-портом, умноженный на три. Одна сознательная поправка формата, которую мок не делает: сумма ареста (AD-3 явно связывает «`risk-blocks` (arrest sum)») выносится в структурированное поле в копейках рядом с неизменным текстом отображения.

## Границы и ограничения (Boundaries & Constraints)

**Всегда:**
- Форма ответа `{sfm: {title, text}, arrest: {title, text, amount}, af: {title, text}}` на `GET /cards/{cardId}/blocks`, зеркалящая `mockData.blocks` — AD-8.
- `arrest.amount` — знаковое целое число в копейках (AD-3) — встроенная в `arrest.text` сумма мока `"сумма ареста 15 000 ₽"` становится новым полем `1500000`; `arrest.text` остаётся побайтово строкой мока (фронтенд отображает её как есть, эта история не меняет фронтенд).
- Каждая блокировка обслуживается через собственный порт (`SfmPort`, `FsspPort`, `AntifraudPort`) и собственный stub-адаптер (AD-10) — один порт на внешнюю систему, никогда общий/catch-all порт.
- Все три блокировки безусловно присутствуют для любого `cardId` в этой демо-фикстуре, по прецеденту `StubOfferEngineAdapter` offers-service (безусловная фикстура независимо от входа) и текущему безусловному доступу `BlocksPage.jsx` к `blocks.sfm.title`/`blocks.arrest.title`/`blocks.af.title` без null-проверок. «Какие блокировки активны» из SPEC.md CAP-7 для этой демо на стабах — это весь набор фикстуры, всегда активный.
- `cardId` принимается как есть, никогда не проверяется против `card-account-service` (AD-9).
- `@RestControllerAdvice` этого сервиса пишется в уже финальной форме с самого начала (та же форма, что у каждого сервиса начиная с Истории 6).

**Никогда:**
- Никакой персистентности, никакого чейнджсета Liquibase — как у `offers-service` (История 2), `risk-blocks-service-postgres` остаётся выделенным, но неиспользуемым; это чистый фасад над stub-портами без собственных данных.
- Никаких симулированных отказов внешних систем в stub-адаптерах — по прецеденту детерминированной фикстуры `offers-service`. `UPSTREAM_UNAVAILABLE` уже зарезервирован в общем словаре кодов ошибок (решено на этапе планирования Истории 1 — см. Карту кода) для будущего пути отказа реальной интеграции; эта история не создаёт его искусственно.
- Встроенная в `sfm.text` сумма (`"проверка операции на 5 000 ₽"`) НЕ выносится в поле — связка AD-3 называет именно «сумму ареста»; цифра СФМ описывает, какая транзакция вызвала проверку, а не денежный факт на уровне блокировки, как сумма ареста.
- Никаких write/мутационных эндпоинтов — критерий успеха CAP-7 чисто read-only.

## Матрица ввода-вывода и краевых случаев (I/O & Edge-Case Matrix)

| Сценарий | Вход / состояние | Ожидаемый результат | Обработка ошибок |
|----------|--------------|---------------------------|----------------|
| Любая карта | `GET /cards/card-4019/blocks` | `200`, `sfm`+`arrest`+`af`, совпадающие с `mockData.blocks`, добавлен `arrest.amount` как `1500000` | N/A |
| Другая карта | `GET /cards/card-7274/blocks` | `200`, идентичная фикстура (безусловная — пока не персонализирована, как и предложение Истории 2) | N/A |
| Не-GET метод | `POST /cards/card-4019/blocks` | — | `405` с конвертом ошибки спайна, присутствует заголовок `Allow` |

</frozen-after-approval>

## Карта кода (Code Map)

- `ARCHITECTURE-SPINE.md` AD-1 — `risk-blocks` фасадит 3 stub-адаптера; AD-3 — связывает «`risk-blocks` (arrest sum)» с копейками; AD-8 — REST-форма зеркалит `client.js`; AD-9 — никаких синхронных межсервисных вызовов; AD-10 — один порт на внешнюю систему (`SfmPort`/`FsspPort`/`AntifraudPort`)
- `ARCHITECTURE-SPINE.md` Consistency Conventions — словарь кодов ошибок (`NOT_FOUND`, `VALIDATION_ERROR`, `INTERNAL_ERROR`, `UPSTREAM_UNAVAILABLE`) уже решён на этапе планирования Истории 1 — это закрывает бывший открытый вопрос 1 SPEC.md и заметку `invoke_dev_with` Истории 7 из `stories.yaml`; новое решение не нужно, `UPSTREAM_UNAVAILABLE` остаётся зарезервированным и неиспользуемым детерминированным стабом этой истории (см. Границы)
- Экспорт `blocks` из `src/api/mockData.js` — точная фикстура; `"15 000 ₽"` в `arrest.text` — то, что извлекается в `amount`
- `getBlocks(cardId)` из `src/api/client.js` → `GET /cards/{cardId}/blocks`
- `src/pages/BlocksPage.jsx` — безусловный доступ к `blocks.sfm.title`/`blocks.arrest.title`/`blocks.af.title`, без null-проверок — доказательство, что все три блокировки должны присутствовать всегда
- `backend/offers-service/` (История 2) — соседний прецедент по всей форме: паттерн безусловной фикстуры `OfferEnginePort`/`StubOfferEngineAdapter`, слой `Offer`/`GetTopOfferUseCase`/`OfferController`/`OfferExceptionHandler` (копировать форму, здесь — трижды по числу портов), и `offers-service-postgres`, остающийся выделенным-но-неиспользуемым
- `backend/risk-blocks-service/` (История 0) — существующий пустой скелет; уже есть `adapters/out/external/{sfm,fssp,antifraud}/package-info.java`, предсозданные для этой истории
- `backend/gateway/src/main/resources/application.yml` — существующий маршрут `/risk-blocks/**` (`StripPrefix=1`) уже проксирует сюда; изменений в gateway не требуется

## Задачи и приёмка (Tasks & Acceptance)

**Выполнение:**
- [x] `domain/SfmBlock.java`, `ArrestBlock.java`, `AntifraudBlock.java` -- framework-free value objects; только `ArrestBlock` несёт `amountMinorUnits`
- [x] `domain/RiskBlocks.java` -- агрегат `{sfm, arrest, af}`, возвращаемый use case'ом
- [x] `domain/SfmPort.java`, `FsspPort.java`, `AntifraudPort.java` -- один порт на внешнюю систему (AD-10)
- [x] `application/GetRiskBlocksUseCase.java` -- вызывает все три порта, собирает `RiskBlocks`
- [x] `adapters/out/external/sfm/StubSfmAdapter.java`, `adapters/out/external/fssp/StubFsspAdapter.java`, `adapters/out/external/antifraud/StubAntifraudAdapter.java` -- каждый возвращает свою фиксированную фикстуру независимо от `cardId`, по прецеденту `StubOfferEngineAdapter`
- [x] `adapters/in/web/RiskBlocksController.java` + DTO ответа -- `GET /cards/{cardId}/blocks`
- [x] `adapters/in/web/RiskBlocksExceptionHandler.java` + `ErrorResponse.java` -- финальная форма с самого начала
- [x] `src/test/.../RiskBlocksControllerIntegrationTest.java` -- `@SpringBootTest` + `WebTestClient`, автоконфигурация DataSource/JPA отключена (реальный Postgres не нужен, по прецеденту `OfferControllerIntegrationTest`); покрывает все 3 строки матрицы I/O

**Критерии приёмки:**
- Даны stub-адаптеры, когда вызывается `GET /cards/card-4019/blocks` напрямую на сервисе, тогда `200` с `sfm`+`arrest`+`af`, совпадающими с `mockData.blocks`, `arrest.amount` равен `1500000`.
- Дан другой `cardId`, когда вызывается `GET /cards/card-7274/blocks`, тогда `200` с идентичной фикстурой.
- Дан поднятый через `docker compose up -d --wait` стек, когда вызывается `GET http://localhost:8080/risk-blocks/cards/card-4019/blocks` через Gateway, тогда приходит тот же `200`.
- Дан не-`GET` метод, когда отправляется `POST`, тогда `405` с конвертом ошибки спайна и заголовком `Allow`.

## Заметки по реализации (Implementation Notes)

- Форма с тремя портами/тремя stub-адаптерами, агрегат `RiskBlocks` и финальная форма `RiskBlocksExceptionHandler` (скопирована побайтово из `OperationsExceptionHandler` сервиса `operations-service` — уже финальной формы, которую каждый сервис несёт начиная с Истории 6) легли точно по плану из Карты кода/Задач — без отклонений.
- `RiskBlocksResponse` — один файл с тремя вложенными record'ами (`SfmResponse`/`ArrestResponse`/`AfResponse`), по прецеденту вложенных record'ов `OperationsResponse`, а не три отдельных файла DTO.
- `risk-blocks-service/pom.xml` потребовал одного добавления сверх скелета Истории 0: тестовую зависимость `spring-boot-starter-webflux`, чтобы `WebTestClient` (используемый `RiskBlocksControllerIntegrationTest`) был доступен для этого Servlet-стек приложения — то же добавление, что уже несёт pom `offers-service`.
- Javadoc `RiskBlocksServiceApplication.java` оставлен без изменений (всё ещё говорит "No business capability is implemented here yet") — по прецеденту `offers-service`, не обновлявшего Javadoc этого класса после реализации своей способности.

## Журнал изменений спеки (Spec Change Log)

## Журнал триажа ревью (Review Triage Log)

- **Находка (Blind Hunter):** в диффе нет файла-близнеца `7-risk-blocks.ru.md`. **Вердикт: false.** Русский дубликат существует на диске, создан вместе с одобрением спеки; он оказался за пределами области путей, с которой был сгенерирован дифф ревьюера, а не реальный пробел.
- **Находка (Blind Hunter):** полностью немаппированный маршрут (без совпадающего обработчика вообще) проваливается в страницу ошибки Spring Boot по умолчанию, а не в JSON-конверт спайна. **Вердикт: medium, если реально, но defer.** Идентичный, предсуществующий пробел во всех девяти скелетах сервисов (та же находка, что в Истории 6); не привнесён и не затронут диффом этой истории.
- **Находка (Blind Hunter):** общая (не-валидационная, не-4xx) ветка `handleExceptionInternal` передаёт необработанный `exception.getMessage()`, в то время как отдельный catch-all `handleUnexpected` всегда возвращает фиксированное общее сообщение — непоследовательная утечка деталей в зависимости от того, какая ветка поймает отказ. **Вердикт: medium, если реально, но defer.** Побайтово идентично обработчику ошибок каждого другого сервиса с тех пор, как форма была финализирована в Истории 6; предсуществующее, не вызвано здесь.
- **Находка (Blind Hunter):** ни один unit-тест не проверяет `Objects.requireNonNull`-защиту в `AntifraudBlock`/`ArrestBlock`/`SfmBlock`/`RiskBlocks`. **Вердикт: отклонена.** Все три stub-адаптера передают в эти конструкторы только жёстко заданные ненулевые литералы — в этой истории нет пути кода, способного передать null, поэтому защита — недостижимый защитный код, а не недотестированное поведение.
- **Находка (Blind Hunter):** ветка `MethodArgumentNotValidException` никогда не прогоняется ни одним тестом. **Вердикт: отклонена.** Подтверждено через grep: в этом сервисе нет ни одной аннотации `@Valid`/`@NotBlank`/`@NotNull`/`@Size` — у CAP-7 нет тела запроса для валидации (read-only, без мутационных эндпоинтов), поэтому эта ветка доказуемо недостижима в этом сервисе, оставлена только для единства формы с соседями, которые её используют.
- **Находка (Blind Hunter):** путь `handleUnexpected`/500 `INTERNAL_ERROR` не покрыт тестами. **Вердикт: low, defer.** Тот же пробел идентично существует у каждого соседнего сервиса; ни у одного из их read-only срезов нет естественного триггера для нераспознанного исключения.
- **Находка (Blind Hunter):** `GetRiskBlocksUseCase.execute` вызывает все три stub-порта последовательно, а не параллельно, что станет проблемой задержки, когда реальные сетевые адаптеры заменят стабы. **Вердикт: отклонена.** Преждевременная оптимизация для сценария, явно отложенного AD-10 (реальных интеграций пока не существует); сегодняшние вызовы стабов — в памяти и мгновенны. Пересмотреть, когда реальный адаптер действительно появится, а не умозрительно сейчас.
- **Находка (Blind Hunter):** Javadoc класса `RiskBlocksServiceApplication` всё ещё гласит «No business capability is implemented here yet», оставлен устаревшим сознательно. **Вердикт: отклонена.** Подтверждено: эквивалентный `OffersServiceApplication` сервиса `offers-service` несёт идентичную устаревшую формулировку со времён Истории 2 — это предсуществующий, последовательный стиль кодобазы, а не регрессия, привнесённая этой историей.
- **Находка (Blind Hunter):** ни один тест не покрывает пустой/необычный сегмент пути `cardId`, хотя он явно не валидируется. **Вердикт: false.** Каждый из трёх stub-адаптеров полностью игнорирует параметр `cardId` и возвращает жёстко заданную фикстуру независимо от его значения — в этой истории нет пути кода, который читал бы или действовал на основании содержимого `cardId`, поэтому никакое значение входа не может привести к иному поведению.
- **Находка (Blind Hunter):** у нового поля `arrest.amount` нет суффикса вида «MinorUnits», из-за чего будущий потребитель API не сможет понять из одной только нагрузки, что это копейки. **Вердикт: false.** Это совпадает с уже установленной во всей кодобазе конвенцией (`balance` Истории 3, `sum` Истории 6) голых имён полей для денежных значений — AD-3 документирует правило копеек универсально, один раз на уровне архитектуры, а не по полю; суффикс здесь нарушил бы согласованность именования с соседями, а не улучшил бы её.
- **Находка (Edge Case Hunter):** обработка `MethodArgumentNotValidException` дала бы пустую строку `message`, если у `BindingResult` есть только глобальные ошибки и ноль ошибок полей (нет фолбэка на `getGlobalErrors()`). **Вердикт: medium, если реально, но defer.** Предсуществует в унаследованной форме обработчика ошибок, идентична в каждом соседнем сервисе, у которого есть эта ветка; и, как установлено выше, у этого сервиса вообще нет аннотаций валидации, поэтому ветка недостижима именно здесь. Стоит исправить у истока формы, а не по сервису.
- **Находка (Verification Gap):** нет находок — подтверждено, что все 3 строки матрицы I/O покрыты `RiskBlocksControllerIntegrationTest`, который поднимает настоящее DI-собранное приложение (не замоканное) и проходит.

## Заметки по дизайну (Design Notes)

Два решения по объёму стоит зафиксировать. Первое: `arrest.amount` — новое поле; в моке такого поля нет, только цифра, встроенная в прозу `arrest.text`. Явная, узкая связка AD-3 («`risk-blocks` (arrest sum)») трактуется как сознательная: структурированное поле получает только сумма ареста; собственная встроенная в `sfm.text` сумма `"5 000 ₽"` (описывающая, какая транзакция вызвала проверку СФМ, а не денежный факт на уровне блокировки) оставлена без изменений — это соответствует точной формулировке спайна, а не избыточному применению AD-3 к любой цифре, встретившейся в тексте любой блокировки. Второе: все три блокировки возвращаются безусловно для любого `cardId` — `BlocksPage.jsx` сегодня обращается к `blocks.sfm.title` и т. д. без единой null-проверки, поэтому `null`-блокировка для нераспознанной карты сломала бы существующий фронтенд; возврат той же всегда заполненной фикстуры независимо от `cardId` (точно как `StubOfferEngineAdapter` offers-service) — единственная форма, не несущая этого риска, а «какие блокировки активны» из SPEC.md выполняется тривиально, поскольку в этой демо на стабах активны всегда все три.

## Проверка (Verification)

**Команды:**
- `docker compose -f backend/docker-compose.yml build risk-blocks-service` -- ожидается: образ собирается
  - **Результат: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- ожидается: `risk-blocks-service` отчитывается healthy (миграций нет; его контейнер Postgres остаётся поднятым, но неиспользуемым, как `offers-service-postgres`)
  - **Результат: PASS.** Весь стек из 20 контейнеров healthy (`docker compose up -d --wait`), включая `risk-blocks-service-postgres` — healthy, но неиспользуемый.
- `curl -s http://localhost:8080/risk-blocks/cards/card-4019/blocks` -- ожидается: `200`, совпадает с `mockData.blocks` с добавленным `arrest.amount`
  - **Результат: PASS.** `{"sfm":{"title":"Блокировка СФМ / Списки","text":"Карта •••• 4019 · с 15.09.2026 · причина: проверка операции на 5 000 ₽"},"arrest":{"title":"Арест ФССП","text":"Пристав: ОСП по г. Москве · сумма ареста 15 000 ₽ · с 02.08.2026","amount":1500000},"af":{"title":"Блокировка АФ","text":"Карта •••• 7274 · неактивна"}}`.
- `curl -s http://localhost:8080/risk-blocks/cards/card-7274/blocks` -- ожидается: `200`, идентичное тело
  - **Результат: PASS.** Побайтово идентично ответу для `card-4019`.
- `curl -i -X POST http://localhost:8080/risk-blocks/cards/card-4019/blocks` -- ожидается: `405`, присутствует заголовок `Allow`
  - **Результат: PASS.** `HTTP/1.1 405`, `Allow: GET`, тело `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'POST' is not supported"}}`.
- Набор тестов модуля -- ожидается: все проходят
  - **Результат: PASS.** `Tests run: 3, Failures: 0, Errors: 0` -- запущено независимо в одноразовом контейнере `eclipse-temurin:25` (хостовый JDK — 17, по ограничению проекта на сборку только через Docker). Стек остановлен (`docker compose down`) после проверки.
