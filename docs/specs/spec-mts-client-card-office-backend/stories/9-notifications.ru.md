---
title: 'История 9 — CAP-9: Уведомления'
type: 'feature'
created: '2026-10-11'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '61803af44a766a41ac1a02580180f50c311077d0'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<!-- Русская версия — перевод 9-notifications.md для удобства чтения; при расхождении английский оригинал остаётся источником истины и единственным файлом, который читает воркфлоу bmad-build. -->

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Суть (Intent)

**Проблема:** `notifications-service` (История 0) имеет только health-эндпоинт. CAP-9 нужно обслужить вызов `getNotifications(cardId)` фронтенда тумблерами каналов уведомлений плюс основным способом связи, а также подключить `toggleNotification(cardId, notificationId, on)` к настоящей мутации — первому подлинному изменению состояния существующей строки в этом бэкенде (каждая предыдущая мутация, Истории 4/5, лишь запускала побочный эффект или создавала новую строку; эта впервые переключает поле уже существующей строки).

**Подход:** Срез, владеющий данными через Liquibase (как `cashback-service`, История 8), с `GET /cards/{cardId}/notifications`, возвращающим единую демо-фикстуру на весь срок, и `POST /cards/{cardId}/notifications/{notificationId}`, персистирующим новое состояние `on`, переиспользуя форму `DocumentNotFoundException`/404 из `card-account-service` для неизвестного канала.

## Границы и ограничения (Boundaries & Constraints)

**Всегда:**
- `GET /cards/{cardId}/notifications` возвращает `{items: [{id, title, subtitle, on, action}], method: {title, current}}`, зеркалящий то, что `getNotifications` из `client.js` собирает из `mockData.notifications` + `mockData.notificationMethod` — AD-8.
- `POST /cards/{cardId}/notifications/{notificationId}` с телом `{on: boolean}` переключает состояние этого канала и возвращает `{ok: true}` (базовый ответ мутации по Consistency Conventions), зеркаля `toggleNotification(cardId, notificationId, on)`.
- Уведомления и способ связи — единая демо-фикстура на весь срок, не привязанная к `cardId` — как `offers`/`risk-blocks`/`cashback` (Истории 2/7/8), `cardId` принимается по форме пути AD-8, но ничего не фильтрует. Ничто в моке или безусловном доступе `NotificationsPage.jsx` к `data.method.title`/`data.items.map` не привязывает эти данные к идентичности конкретной карты. Переключение — по-настоящему общее, персистируемое изменение состояния — первая настоящая update-in-place мутация в этом бэкенде.
- Неизвестный `notificationId` в пути `POST` возвращает `404 NOT_FOUND` с конвертом спайна — в отличие от всегда-`200` коллекционных эндпоинтов (Истории 2/6/7/8), здесь цель — один идентифицированный ресурс, тот же прецедент `NOT_FOUND`, который уже установили `/clients/{id}` Истории 1 и send/print документов Истории 4.
- `action` остаётся литеральной отображаемой строкой мока (например, `"Отключить"`, `"Настроить"`) без изменений — вторичная `<Button>` в `NotificationsPage.jsx` только отображает её, без единого `onClick`; она не несёт бэкенд-смысла.
- Персистируемая булева колонка называется `enabled`, а не `on` — `on` — ключевое слово SQL, зарезервированное Postgres для `JOIN ... ON`; избегание его как имени колонки избегает необходимости квотировать его повсюду. Проводное поле и доменный аксессор остаются `on`/`isOn()`, зеркаля мок точно.
- Exception handler этого сервиса пишется в уже финальной форме с самого начала, расширенной одним маппингом `NotFoundException`, точно зеркалящим прецедент `DocumentNotFoundException` из `card-account-service` (один advice-класс, без второго).

**Никогда:**
- Никакой мутации смены способа связи — критерий успеха CAP-9 покрывает только действие переключения; у кнопки «Изменить» способа уведомлений в `NotificationsPage.jsx` вообще нет `onClick`, поэтому смена способа — вне объёма.
- Никакой вариации сида по картам для уведомлений или способа связи — единая демо-фикстура (та же логика, что у кэшбэка Истории 8), а не набор строк на карту.
- Переключение никогда не проверяет `cardId` против `card-account-service` (AD-9) и никогда не использует его для определения, какой канал обновлять — только `notificationId` идентифицирует целевую строку.

## Матрица ввода-вывода и краевых случаев (I/O & Edge-Case Matrix)

| Сценарий | Вход / состояние | Ожидаемый результат | Обработка ошибок |
|----------|--------------|---------------------------|----------------|
| Любая карта, чтение | `GET /cards/card-4019/notifications` | `200`, `items`+`method` совпадают с моком, демо-фикстура на весь срок | N/A |
| Другая карта, чтение | `GET /cards/card-7274/notifications` | `200`, идентичная фикстура | N/A |
| Переключение известного канала | `POST /cards/card-4019/notifications/sms-ops` тело `{"on": false}` | `200 {"ok": true}`; последующий `GET` отражает `on: false` для `sms-ops` | N/A |
| Переключение неизвестного канала | `POST /cards/card-4019/notifications/nope` тело `{"on": false}` | — | `404 NOT_FOUND` |
| Переключение с пропущенным полем тела | `POST /cards/card-4019/notifications/sms-ops` тело `{}` | — | `400 VALIDATION_ERROR`, чистое сообщение |
| Не-POST метод на пути переключения | `PUT /cards/card-4019/notifications/sms-ops` | — | `405` с конвертом ошибки спайна, присутствует заголовок `Allow` |

</frozen-after-approval>

## Карта кода (Code Map)

- `ARCHITECTURE-SPINE.md` AD-1 — `notifications` владеет данными тумблеров/способа связи; AD-2 — ссылка только по `cardId`; AD-8 — REST-форма зеркалит `client.js`; AD-9 — никаких синхронных межсервисных вызовов; AD-12 — миграции Liquibase; Consistency Conventions — мутации это POST, базовый ответ `{ ok: true }`
- Экспорты `notifications` (3 канала, все `on: true` сегодня) и `notificationMethod` из `src/api/mockData.js` — точные фикстуры
- `getNotifications(cardId)` из `src/api/client.js` → `GET /cards/{cardId}/notifications`; `toggleNotification(cardId, notificationId, on)` → `POST /cards/{cardId}/notifications/{notificationId}`
- `src/pages/NotificationsPage.jsx` — каждый элемент рендерит `<Toggle>`, привязанный к `handleToggle`, независимо от метки `action`; вторичная `<Button>{item.action}</Button>` и кнопка «Изменить» способа связи — обе без `onClick` — доказательство, что `action` декоративен, а смена способа — вне объёма
- `backend/card-account-service/` (История 4) — `DocumentNotFoundException` + маппинг `@ExceptionHandler(DocumentNotFoundException.class)` в `CardExceptionHandler` — точная форма для зеркалирования для неизвестного `notificationId`; `OkResponse` — точная форма `{ok: true}` для зеркалирования
- `backend/cashback-service/` (История 8) — соседний прецедент для сервиса, владеющего данными (не stub-портом): разбивка Liquibase `001-create-schema` + `002-seed`, слой сущность/JPA-репозиторий, и паттерн «единая демо-фикстура, cardId не используется»
- `backend/notifications-service/` (История 0) — существующий пустой скелет; уже есть `adapters/out/persistence/package-info.java`, подтверждающий, что сервис владеет данными
- `backend/gateway/src/main/resources/application.yml` — существующий маршрут `/notifications/**` (`StripPrefix=1`) уже проксирует сюда; изменений в gateway не требуется

## Задачи и приёмка (Tasks & Acceptance)

**Выполнение:**
- [x] `db/changelog/changes/001-create-notifications-schema.xml` -- создать таблицы `notification_channels` (id, title, subtitle, enabled, action, sort_order) и `notification_method` (id PK singleton, title, current) -- AD-12
- [x] `db/changelog/changes/002-seed-notifications.xml` -- засеять 3 канала `mockData.notifications` (`sort_order` 1-3, `enabled=true`) + 1 строку `notificationMethod`
- [x] `domain/NotificationChannel.java`, `NotificationMethod.java`, `NotificationsSummary.java` -- framework-free value objects; `NotificationsSummary` агрегирует упорядоченные каналы + способ связи
- [x] `domain/NotificationChannelNotFoundException.java` -- зеркалит форму `DocumentNotFoundException` точно
- [x] `domain/NotificationsRepository.java` -- порт: `NotificationsSummary getSummary(String cardId)`; `void setChannelState(String notificationId, boolean on)`, выбрасывающий `NotificationChannelNotFoundException` для неизвестного id
- [x] `application/GetNotificationsUseCase.java`, `ToggleNotificationUseCase.java` -- оркестрируют порт
- [x] `adapters/out/persistence/NotificationChannelEntity.java`, `NotificationMethodEntity.java`, их Spring Data интерфейсы, `JpaNotificationsRepository.java` -- маппит обе таблицы (булева колонка `enabled`), упорядочивает каналы по `sort_order`
- [x] `adapters/in/web/NotificationsController.java` + `ToggleNotificationRequest.java` (`@NotNull Boolean on`) + DTO ответа -- `GET /cards/{cardId}/notifications`, `POST /cards/{cardId}/notifications/{notificationId}`
- [x] `adapters/in/web/NotificationsExceptionHandler.java` + `ErrorResponse.java` + `OkResponse.java` -- финальная форма с самого начала, плюс маппинг `NotificationChannelNotFoundException` → `404`
- [x] `src/test/.../NotificationsControllerIntegrationTest.java` -- покрывает все 6 строк матрицы I/O на реальном Testcontainers Postgres, включая проверку, что переключение действительно персистируется (через последующий `GET`)

**Критерии приёмки:**
- Дан сид, когда вызывается `GET /cards/card-4019/notifications` напрямую на сервисе, тогда `200` с 3 каналами (все `enabled`) и способом связи, совпадающими с моком.
- Дан известный `notificationId`, когда вызывается `POST /cards/card-4019/notifications/sms-ops` с `{"on": false}`, тогда `200 {"ok": true}`, и последующий `GET` показывает `on` у `sms-ops` как `false`.
- Дан неизвестный `notificationId`, когда вызывается `POST /cards/card-4019/notifications/nope`, тогда `404` с конвертом ошибки спайна.
- Дано пропущенное поле `on`, когда вызывается `POST /cards/card-4019/notifications/sms-ops` с `{}`, тогда `400` с чистым сообщением валидации.
- Дан не-`POST` метод на пути переключения, когда отправляется `PUT`, тогда `405` с конвертом ошибки спайна и заголовком `Allow`.

## Заметки по реализации (Implementation Notes)

- Разбивка на две таблицы (`notification_channels` + singleton `notification_method`), колонка детерминированного порядка `sort_order` и финальная форма `NotificationsExceptionHandler` (скопированная буквально из уже финальной формы, которую каждый сервис несёт с Истории 6, расширенная ровно одним маппингом `NotificationChannelNotFoundException` → `404`) — всё приземлилось точно как планировалось в Карте кода/Задачах, без отклонений.
- `notification_method.current` назван буквально `current`, совпадая с формулировкой списка задач — в отличие от `on`, `CURRENT` — неиспользуемое (non-reserved) ключевое слово в PostgreSQL (может использоваться как неквотированный идентификатор), поэтому ловушка переименования здесь не применима.
- `spring-boot-starter-validation` добавлен явной Maven-зависимостью (не подтягивается транзитивно из `spring-boot-starter-web`) — тот же прецедент, что `@NotBlank` в `card-issuance-service`; это первое использование `@NotNull` в данной истории.
- Три XML-комментария Liquibase изначально содержали буквальный двойной дефис (`--`) внутри `<!-- -->`, что XML запрещает внутри комментария (поймано реальной ошибкой парсинга Liquibase при верификации, а не просто догадкой линтера); переформулированы, чтобы избежать этого — та же повторяющаяся ловушка, которую уже отметила заметка Истории 8 (Истории 1, 3, 4, 5).
- Интеграционный тест выстраивает `POST`, затем `GET` в одном тестовом методе (по Заметкам по дизайну), и — поскольку контекст теста Spring (и его единственный Testcontainers Postgres) переиспользуется во всех шести тестовых методах класса — использует `@TestMethodOrder`/`@Order`, чтобы тест переключения-и-проверки шёл последним, и восстанавливает канал в исходное состояние `on: true` в конце, чтобы мутация не оставляла наблюдаемого побочного эффекта для любого последующего запуска тестов.
- Тест `toggleWithMissingOnFieldReturns400...` проверяет, что `$.error.message` непусто, а не точную строку, по прецеденту самого `IssuanceControllerIntegrationTest` — интерполятор сообщений Jakarta Bean Validation по умолчанию зависит от локали (подтверждено локально: рендерится на русском на хост-JVM с русской локалью по умолчанию, на английском внутри Docker-контейнера самого сервиса), поэтому проверка буквального сообщения была бы хрупкой к окружению.
- Проведена независимая повторная верификация полного стека (чистый `docker compose up -d --wait`, все контейнеры): все 6 строк матрицы I/O подтверждены через `curl` через Gateway (порт 8080), включая проверку персистентности переключения через последующий `GET` и восстановление `sms-ops` обратно в `on: true`. Набор тестов модуля перезапущен в одноразовом JDK 25-тулчейне, эквивалентном `eclipse-temurin:25` (хостовый JDK — 17, по ограничению проекта на сборку только через Docker), на реальном Testcontainers Postgres: `Tests run: 6, Failures: 0, Errors: 0`. Стек остановлен после верификации.

## Журнал изменений спеки (Spec Change Log)

## Журнал триажа ревью (Review Triage Log)

- **Находка (Blind Hunter):** дифф добавляет `9-notifications.md` без закоммиченного файла-близнеца `9-notifications.ru.md`. **Вердикт: false.** Русский дубликат существует на диске, создан вместе с одобрением спеки; он был за пределами области путей, с которой был сгенерирован дифф ревьюера, а не реальный пробел.
- **Находка (Blind Hunter + Edge Case Hunter):** каждая 4xx-ошибка, определённая Spring (404 на немаппированный маршрут, 405, 415, некорректное тело), сворачивается в `VALIDATION_ERROR`, и собственный тест `405` этой истории закрепляет это. **Вердикт: defer.** Побайтово идентично обработчику ошибок каждого соседнего сервиса начиная с Истории 6; предсуществующее, не привнесено здесь.
- **Находка (Blind Hunter + Edge Case Hunter):** `JpaNotificationsRepository.setChannelState` делает обычный find-then-save без оптимистичной блокировки (`@Version`), поэтому два одновременных переключения одного канала могут столкнуться, и запись проигравшего тихо потеряется. **Вердикт: отклонена.** Реально в принципе, но это демо/прототип-бэкенд с одним оператором (явное, уже зафиксированное решение спайна архитектуры по уровню масштаба: «не производственного уровня») — один сотрудник банка, нажимающий один тумблер за раз, а не параллельный доступ множества пользователей. Фикс (поле `@Version` плюс новый путь ответа на конфликт) — больше, чем тривиальное исправление, и добавляет публичную поверхность (новую форму ошибки) для сценария, который архитектура уже исключила из объёма.
- **Находка (Blind Hunter):** нет unit-тестов уровня domain/use case, только один класс интеграционного теста на Testcontainers. **Вердикт: отклонена.** Совпадает с уже установленной, общей для всего проекта конвенцией «только интеграционные тесты», уже идентично принятой в Историях 7/8.
- **Находка (Blind Hunter):** `NotificationChannel`/`NotificationMethod`/`NotificationsSummary` не реализуют `equals()`/`hashCode()`/`toString()`. **Вердикт: отклонена.** Согласуется с каждым другим доменным value object в этой кодобазе (`Operation`, `CashbackCategory`, `RiskBlocks` и т. д. — ни один их не реализует); не уникальный для этой истории пробел.
- **Находка (Blind Hunter + Edge Case Hunter):** отсутствующая singleton-строка `notification_method` выбрасывает обычный `IllegalStateException` без выделенного `@ExceptionHandler`, поэтому падает в общий обработчик 500 и теряет диагностическую деталь. **Вердикт: отклонена.** Та же категория, что аналогичная находка Истории 8 — недостижимо в нормальной работе, поскольку чейнджсет сида Liquibase либо полностью применяется до того, как приложение начинает обслуживать трафик, либо приложение не стартует вовсе; падение в общий 500 — корректное поведение для этого действительно исключительного состояния.
- **Находка (Blind Hunter):** ответ переключения `{"ok": true}` никогда не отражает итоговое состояние канала, вынуждая вызывающих делать второй `GET`. **Вердикт: отклонена.** Замороженные Границы и ограничения и Consistency Conventions спайна явно фиксируют базовый ответ мутации как `{ ok: true }` (совпадает с идентичной формой `OkResponse` Историй 4/5) — это уже решённое архитектурное решение, а не пробел для заполнения.
- **Находка (Blind Hunter):** тест переключения-и-проверки полагается на `@Order(6)` плюс ручное восстановление в конце, без `@AfterEach`/`@AfterAll` страховки, если более раннее утверждение в этом же методе упадёт первым. **Вердикт: отклонена.** Это последний по порядку тест в классе, поэтому падение в середине метода не может протечь в любой другой тест того же прогона; свежий Testcontainers Postgres выделяется для следующего независимого прогона тестов в любом случае, а само падение уже видимо репортится.
- **Находка (Blind Hunter):** новые координаты зависимостей `pom.xml` (`spring-boot-liquibase`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`) обоснованы только встроенными комментариями, без доказательства, что они резолвятся. **Вердикт: false.** Прямо опровергнуто независимой перепроверкой: сервис был пересобран из чистого Docker-образа, и его полный набор тестов модуля был успешно перепрогнан отдельным проходом проверки.
- **Находка (Blind Hunter + Edge Case Hunter):** нет теста на некорректный/пустой `cardId` на любом из эндпоинтов. **Вердикт: false.** `cardId` никогда не читается ни одним запросом или фильтром в этом сервисе — `JpaNotificationsRepository.getSummary(cardId)` вообще не обращается к своему параметру, а `setChannelState` вообще не принимает его — поэтому никакое значение `cardId` не может привести к иному поведению.
- **Находка (Edge Case Hunter):** `notification_channels` может оказаться пустой, пока строка `notification_method` существует, что даёт `200` с `items: []` вместо явной ошибки. **Вердикт: отклонена.** Та же логика, что в аналогичной находке Истории 8 — достижимо только через состояние с упавшей посреди миграции, которое самоисправляется при следующем перезапуске приложения через собственное отслеживание чейнджлога Liquibase; не риск устойчивого состояния.
- **Находка (Verification Gap):** ветка чистого сообщения валидации проверяется только на непустоту, а не на то, что это действительно чистая форма «поле: причина», а не необработанный дамп исключения, который эта ветка должна предотвращать — регрессия здесь прошла бы тихо зелёной. **Вердикт: defer**, по собственной диспозиции ревьюера: идентичный, предсуществующий паттерн слабого утверждения уже присутствует в аналогичном тесте `card-issuance-service`; ужесточение здесь в одиночку не закрыло бы пробел по всему репозиторию, поэтому это кросс-сервисный фикс, а не единичный патч для этой истории.

## Заметки по дизайну (Design Notes)

Переключение — первая настоящая update-in-place мутация этого бэкенда: каждая предыдущая мутация (send/print документов Истории 4, создание заявки Истории 5) лишь запускала побочный эффект или вставляла новую строку; эта переключает поле уже существующей строки, и следующий `GET` обязан это отразить. Поэтому интеграционный тест выстраивает `POST`, затем `GET` в одном тестовом методе, а не проверяет только ответ `POST`. Именование колонки `enabled` (в отличие от проводного `on`) — небольшое, но сознательное решение: `ON` — часть синтаксиса `JOIN` в SQL, и хотя технически его можно использовать как квотированный идентификатор в Postgres, именование вокруг него избегает потенциальной ловушки для любого будущего запроса к этой таблице.

## Проверка (Verification)

**Команды:**
- `docker compose -f backend/docker-compose.yml build notifications-service` -- ожидается: образ собирается
  - **Результат: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- ожидается: `notifications-service` и его Postgres отчитываются healthy
  - **Результат: PASS.** Весь стек (gateway + все девять сервисов + Kafka + экземпляры Postgres) healthy.
- `curl -s http://localhost:8080/notifications/cards/card-4019/notifications` -- ожидается: `200`, совпадает с фикстурой мока
  - **Результат: PASS.** `items` вернул все 3 канала (`sms-ops`, `sms-plan`, `push`) с `on:true` и точными строками `title`/`subtitle`/`action` из мока; `method` совпал с `{"title":"Способ уведомлений (ОМТ)","current":"Email · v***@mail.ru"}` точно.
- `curl -s -X POST -H 'Content-Type: application/json' -d '{"on":false}' http://localhost:8080/notifications/cards/card-4019/notifications/sms-ops` -- ожидается: `200 {"ok":true}`
  - **Результат: PASS.**
- `curl -s http://localhost:8080/notifications/cards/card-4019/notifications` (повторно) -- ожидается: `on` у `sms-ops` теперь `false`
  - **Результат: PASS.** `sms-ops` показал `on:false`; `sms-plan`/`push` не затронуты. Восстановлено в `on:true` после проверки, чтобы оставить демо-фикстуру в исходном состоянии.
- `curl -i -X POST http://localhost:8080/notifications/cards/card-4019/notifications/nope -d '{"on":false}' -H 'Content-Type: application/json'` -- ожидается: `404`
  - **Результат: PASS.** `404`, тело `{"error":{"code":"NOT_FOUND","message":"Notification channel not found: nope"}}`.
- `curl -i -X PUT http://localhost:8080/notifications/cards/card-4019/notifications/sms-ops` -- ожидается: `405`, присутствует заголовок `Allow`
  - **Результат: PASS.** `405`, `Allow: POST`, тело `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'PUT' is not supported"}}`.
- Набор тестов модуля -- ожидается: все проходят
  - **Результат: PASS.** `Tests run: 6, Failures: 0, Errors: 0` -- покрывает все 6 строк матрицы I/O на реальном Testcontainers Postgres, включая случай «переключение персистируется и наблюдается через GET» (с восстановлением состояния в конце этого теста).
