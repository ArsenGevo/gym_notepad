# Gym Notepad — модель данных MVP

Этот документ описывает логическую модель и рекомендуемое отображение в Room. Имена полей приведены в стиле Kotlin/SQL; пользовательские тексты интерфейса определяются отдельно.

## 1. Принципы

1. Room — локальный источник истины.
2. Фактическая тренировка хранит снимок плана на момент выполнения.
3. Изменение шаблона не переписывает историю.
4. Разминочные и рабочие подходы различаются явно.
5. Пропущенный, запланированный и выполненный подход — разные состояния.
6. Пустой вес не означает автоматически предыдущий вес.
7. Тип нагрузки определяет смысл числового значения.
8. Масса хранится в канонической целочисленной единице.
9. Все синхронизируемые сущности имеют UUID и метаданные изменения.
10. Удаление пользовательских сущностей выполняется логически, чтобы позднее поддержать синхронизацию.

## 2. Общие поля синхронизируемых сущностей

```text
id: UUID
ownerId: UUID?                 // null в локальном MVP
createdAt: Instant
updatedAt: Instant
deletedAt: Instant?            // soft delete
revision: Long                 // локально увеличивается при изменении
syncState: SyncState           // LOCAL_ONLY в MVP
```

`syncState`:

- `LOCAL_ONLY` — запись существует только локально;
- `PENDING_UPLOAD` — зарезервировано для будущей синхронизации;
- `SYNCED` — зарезервировано;
- `CONFLICT` — зарезервировано.

MVP не реализует сетевое поведение этих состояний, но схема не должна требовать миграции первичных ключей при появлении аккаунтов.

## 3. Единицы и числовое хранение

### 3.1. Масса

Каноническое поле массы — целое число граммов:

```text
loadGrams: Long?
```

Примеры:

- `32 кг` → `32000`;
- `25 lb` → приблизительно `11340` после контролируемого округления для хранения.

Дополнительно сохраняются:

```text
enteredValue: DecimalText?     // исходное нормализованное значение, например "25"
enteredUnit: MassUnit?         // KG или LB
```

`enteredValue` нужен для стабильного редактирования без накопления ошибок повторной конвертации. Для расчётов используется `loadGrams`.

### 3.2. Схема доступной нагрузки

Единого шага веса нет. У каждого упражнения хранится `LoadProgressionType`:

- `DUMBBELL_2_KG` — следующая гантель тяжелее на 2 кг; значение относится к одной руке;
- `CUSTOM_VALUES` — упорядоченный список фактически доступных значений конкретного тренажёра;
- `BARBELL_PLATES` — вес грифа и симметричные блины `1.25, 2.5, 5, 10, 15, 20, 25 кг`;
- `PLATE_LOADED` — тренажёр, нагружаемый симметричными блинами;
- `NONE` — автоматическое увеличение веса недоступно.

Для `CUSTOM_VALUES` значения хранятся отдельными строками `ExerciseLoadOptionEntity`. Для штанги сохраняется `barWeightGrams`, поскольку гриф может весить не только 20 кг. Если следующего доступного значения нет, приложение не генерирует произвольный вес.

### 3.3. Вес тела

Вес тела также хранится в граммах. В тренировочной сессии сохраняется снимок, чтобы последующее изменение веса тела не меняло прошлые результаты.

## 4. Перечисления

### `MassUnit`

- `KG`
- `LB`

### `LoadMode`

- `EXTERNAL_TOTAL` — общий внешний вес, например штанга;
- `DUMBBELL_PER_HAND` — вес одной гантели;
- `MACHINE` — значение стека или шкалы тренажёра;
- `BODYWEIGHT` — собственный вес без дополнительной нагрузки;
- `BODYWEIGHT_ADDED` — в нагрузке хранится дополнительный вес;
- `ASSISTANCE` — величина помощи, например гравитрон.

### `SetKind`

- `WARMUP`
- `WORK`

### `SetStatus`

- `PLANNED`
- `COMPLETED`
- `SKIPPED`

### `EffortMark`

- `UNKNOWN`
- `RIR_2_PLUS`
- `RIR_1`
- `FAILURE`

### `WorkoutStatus`

- `ACTIVE`
- `COMPLETED`
- `DISCARDED`

### `WorkoutOrigin`

- `TEMPLATE`
- `COPIED_SESSION`
- `EMPTY`

### `RecommendationOutcome`

- `SUCCESS`
- `BORDERLINE`
- `PROGRESSING`
- `MISSED`
- `BLOCKED`
- `INSUFFICIENT_DATA`

### `RecommendationAction`

- `INCREASE_LOAD`
- `KEEP_LOAD_ADD_REPS`
- `KEEP_LOAD_REPEAT`
- `CONSIDER_REDUCTION`
- `MANUAL_REVIEW`
- `NONE`

## 5. Сущности

## 5.1. `LocalProfileEntity`

Локальный профиль и настройки пользователя.

```text
id: UUID
displayName: String?
preferredMassUnit: MassUnit
defaultRestTimerEnabled: Boolean
defaultAutoStartRestTimer: Boolean
defaultRestSeconds: Int
createdAt: Instant
updatedAt: Instant
```

В MVP существует одна активная запись, но внешние ключи не должны использовать фиксированное значение вроде `1`.

## 5.2. `ExerciseEntity`

Каноническое упражнение.

```text
id: UUID
ownerId: UUID?
name: String
muscleGroup: String?
equipmentName: String?
loadMode: LoadMode
defaultIncrementGrams: Long?
defaultIncrementEnteredValue: String?
defaultIncrementUnit: MassUnit?
notes: String?
createdAt: Instant
updatedAt: Instant
deletedAt: Instant?
revision: Long
syncState: SyncState
```

Одинаковое движение на разных тренажёрах может быть отдельным упражнением, если значения нагрузки нельзя сопоставлять.

Индексы:

- нормализованное имя;
- `ownerId`;
- `deletedAt`.

## 5.3. `ExerciseAliasEntity`

Альтернативное название упражнения.

```text
id: UUID
exerciseId: UUID
alias: String
normalizedAlias: String
createdAt: Instant
updatedAt: Instant
deletedAt: Instant?
```

Уникальность: `(exerciseId, normalizedAlias)` среди неудалённых записей.

## 5.4. `ExerciseResourceEntity`

Пояснение или ссылка на технику.

```text
id: UUID
exerciseId: UUID
title: String
url: String?
note: String?
sortOrder: Int
createdAt: Instant
updatedAt: Instant
deletedAt: Instant?
```

## 5.5. `WorkoutTemplateEntity`

Шаблон A/B/C или произвольная программа.

```text
id: UUID
ownerId: UUID?
name: String
description: String?
isArchived: Boolean
createdAt: Instant
updatedAt: Instant
deletedAt: Instant?
revision: Long
syncState: SyncState
```

## 5.6. `TemplateExerciseEntity`

Упражнение внутри шаблона.

```text
id: UUID
templateId: UUID
exerciseId: UUID
sortOrder: Int
warmupSetCount: Int
workSetCount: Int
repMin: Int
repMax: Int
restSeconds: Int?
incrementGramsOverride: Long?
incrementEnteredValueOverride: String?
incrementUnitOverride: MassUnit?
successesRequired: Int             // по умолчанию 2
notes: String?
createdAt: Instant
updatedAt: Instant
deletedAt: Instant?
revision: Long
syncState: SyncState
```

Ограничения:

- `sortOrder >= 0`;
- `warmupSetCount >= 0`;
- `workSetCount > 0`;
- `repMin > 0`;
- `repMax >= repMin`;
- `successesRequired >= 1`.

## 5.7. `WorkoutSessionEntity`

Конкретная тренировка.

```text
id: UUID
ownerId: UUID?
templateId: UUID?                  // только источник, история не зависит от шаблона
copiedFromSessionId: UUID?
origin: WorkoutOrigin
status: WorkoutStatus
name: String
startedAt: Instant
completedAt: Instant?
bodyWeightGramsSnapshot: Long?
bodyWeightEnteredValue: String?
bodyWeightEnteredUnit: MassUnit?
notes: String?
createdAt: Instant
updatedAt: Instant
deletedAt: Instant?
revision: Long
syncState: SyncState
```

Одновременно допускается не более одной активной сессии на локальный профиль. Ограничение проверяется транзакцией репозитория.

## 5.8. `SessionExerciseEntity`

Фактическое упражнение внутри тренировочной сессии.

```text
id: UUID
sessionId: UUID
exerciseId: UUID
sourceTemplateExerciseId: UUID?
sortOrder: Int
plannedWarmupSetCount: Int
plannedWorkSetCount: Int
repMinSnapshot: Int?
repMaxSnapshot: Int?
restSecondsSnapshot: Int?
incrementGramsSnapshot: Long?
loadModeSnapshot: LoadMode
exerciseNameSnapshot: String
hasPain: Boolean
hadTechniqueBreakdown: Boolean
notes: String?
recommendationOutcome: RecommendationOutcome?
createdAt: Instant
updatedAt: Instant
deletedAt: Instant?
revision: Long
syncState: SyncState
```

Поля `Snapshot` защищают историю от изменений упражнения или шаблона.

## 5.9. `SetRecordEntity`

Запланированный или выполненный подход.

```text
id: UUID
sessionExerciseId: UUID
kind: SetKind
status: SetStatus
sortOrder: Int
targetRepMinSnapshot: Int?
targetRepMaxSnapshot: Int?
loadGrams: Long?
enteredValue: String?
enteredUnit: MassUnit?
reps: Int?
effort: EffortMark
completedAt: Instant?
notes: String?
createdAt: Instant
updatedAt: Instant
deletedAt: Instant?
revision: Long
syncState: SyncState
```

Ограничения:

- `sortOrder >= 0`;
- `reps > 0`, если значение задано;
- `loadGrams >= 0`, если значение задано;
- `COMPLETED` рабочий подход обычно требует `reps` и осмысленную нагрузку для соответствующего `LoadMode`;
- `SKIPPED` не участвует в прогрессии;
- `WARMUP` не участвует в серии успехов.

Для `BODYWEIGHT` поле `loadGrams` может быть `null`, потому что используется снимок веса тела. Для `BODYWEIGHT_ADDED` оно содержит только дополнительный вес. Для `ASSISTANCE` оно содержит величину помощи.

## 5.10. `BodyWeightEntryEntity`

История веса тела.

```text
id: UUID
ownerId: UUID?
measuredAt: Instant
weightGrams: Long
enteredValue: String
enteredUnit: MassUnit
notes: String?
createdAt: Instant
updatedAt: Instant
deletedAt: Instant?
revision: Long
syncState: SyncState
```

Индекс: `(ownerId, measuredAt DESC)`.

## 5.11. `RecommendationSnapshotEntity`

Сохранённое объяснимое предложение, сформированное перед следующим выполнением упражнения.

```text
id: UUID
exerciseId: UUID
targetSessionExerciseId: UUID?
basedOnSessionExerciseIds: List<UUID> // хранится через дочернюю таблицу
outcome: RecommendationOutcome
action: RecommendationAction
suggestedLoadGrams: Long?
suggestedRepMin: Int?
suggestedRepMax: Int?
successStreak: Int
successesRequired: Int
reasonCode: String
reasonTextSnapshot: String
accepted: Boolean?
createdAt: Instant
updatedAt: Instant
deletedAt: Instant?
revision: Long
syncState: SyncState
```

`reasonCode` используется логикой и тестами, а `reasonTextSnapshot` сохраняет то объяснение, которое видел пользователь.

## 5.12. `RecommendationSourceEntity`

Связь рекомендации с использованными результатами.

```text
recommendationId: UUID
sessionExerciseId: UUID
sortOrder: Int
```

Составной первичный ключ: `(recommendationId, sessionExerciseId)`.

## 5.13. `RestTimerStateEntity`

Состояние активного таймера, которое переживает перезапуск процесса.

```text
sessionId: UUID                    // primary key
sourceSetId: UUID?
startedAt: Instant
endsAt: Instant
originalDurationSeconds: Int
isRunning: Boolean
updatedAt: Instant
```

Оставшееся время всегда вычисляется как `endsAt - now`. Строка удаляется или помечается остановленной после завершения таймера.

## 6. Связи

```text
LocalProfile
  ├─ BodyWeightEntry*
  ├─ WorkoutTemplate*
  └─ WorkoutSession*

Exercise
  ├─ ExerciseAlias*
  ├─ ExerciseResource*
  ├─ TemplateExercise*
  └─ SessionExercise*

WorkoutTemplate
  └─ TemplateExercise*

WorkoutSession
  ├─ SessionExercise*
  │    └─ SetRecord*
  └─ RestTimerState 0..1

RecommendationSnapshot
  └─ RecommendationSource*
       └─ SessionExercise
```

## 7. Удаление и внешние ключи

- Пользовательские исторические данные удаляются логически через `deletedAt`.
- Физическое каскадное удаление допустимо только для никогда не сохранённого черновика или при полном сбросе локальной базы с явным подтверждением.
- Удаление шаблона не удаляет сессии.
- Удаление упражнения не удаляет прошлые `SessionExercise`; они сохраняют имя и тип нагрузки в снимке.
- DAO по умолчанию возвращают только записи с `deletedAt IS NULL`.

## 8. Транзакции

Следующие операции выполняются транзакционно:

1. Создание сессии из шаблона вместе с упражнениями и подходами.
2. Копирование прошлой сессии.
3. Изменение порядка упражнений или подходов.
4. Завершение сессии и формирование результатов рекомендаций.
5. Применение принятой рекомендации к новой сессии.
6. Восстановление резервной копии.

## 9. Запросы аналитики

### 9.1. Последнее выполнение упражнения

Фильтры:

- тот же `exerciseId`;
- сессия `COMPLETED`;
- упражнение не удалено из сессии;
- рабочие подходы `COMPLETED`;
- сортировка по `completedAt DESC`.

### 9.2. Повторения при одинаковом весе

Группировка выполняется по:

- `exerciseId`;
- `loadModeSnapshot`;
- `loadGrams`;
- завершённой сессии.

Для `ASSISTANCE` снижение `loadGrams` означает увеличение сложности. Для разных `exerciseId` значения тренажёров не объединяются.

### 9.3. Серия успехов

Алгоритм читает последовательные завершённые выполнения упражнения в обратном хронологическом порядке и останавливается при первом результате, который не является `SUCCESS`.

`UNKNOWN` в оценке усилия само по себе не блокирует успех, если выполнены повторения и отсутствуют признаки боли, нарушения техники и явного отказа. Объяснение рекомендации получает более низкую уверенность.

## 10. Псевдокод рекомендации

```text
evaluate(sessionExercise):
    if hasPain or hadTechniqueBreakdown:
        return BLOCKED

    workSets = completed WORK sets ordered by sortOrder

    if workSets.count < plannedWorkSetCount:
        return MISSED

    if any set has missing reps or required load:
        return MISSED

    if any set before last has effort == FAILURE:
        return MISSED

    if every set.reps >= repMax:
        if last.effort == FAILURE:
            return BORDERLINE
        return SUCCESS

    if any set.reps < repMin:
        return MISSED

    if performance improved at the same comparable load:
        return PROGRESSING

    return BORDERLINE

recommend(history, successesRequired, increment):
    current = evaluate(history.latest)

    if current == BLOCKED:
        return MANUAL_REVIEW

    if consecutive SUCCESS count >= successesRequired:
        if increment is known:
            return INCREASE_LOAD by one available increment
        return MANUAL_REVIEW to choose next available load

    if current == PROGRESSING:
        return KEEP_LOAD_ADD_REPS

    if current == SUCCESS or current == BORDERLINE:
        return KEEP_LOAD_REPEAT

    if latest two comparable outcomes are MISSED:
        return CONSIDER_REDUCTION

    return KEEP_LOAD_REPEAT
```

Для `ASSISTANCE` действие `INCREASE_LOAD` преобразуется в уменьшение помощи на доступный шаг. Для `BODYWEIGHT` сначала повышается цель повторений или сложность варианта; дополнительный вес предлагается только в режиме `BODYWEIGHT_ADDED`.

## 11. Резервная копия

JSON содержит:

```text
schemaVersion
exportedAt
appVersion
profile
exercises
exerciseAliases
exerciseResources
templates
templateExercises
sessions
sessionExercises
sets
bodyWeightEntries
recommendations
recommendationSources
```

Состояние активного таймера можно не включать. Активная тренировочная сессия включается, чтобы восстановление не теряло введённые данные.

Перед восстановлением приложение:

1. проверяет версию схемы;
2. валидирует обязательные поля и ссылки;
3. создаёт временную базу;
4. проверяет ограничения;
5. только затем атомарно заменяет текущие данные.

## 12. Подготовка к будущей синхронизации

В MVP не требуется сетевой код. Для будущего расширения сохраняются следующие свойства:

- UUID создаётся на устройстве;
- локальная база остаётся источником чтения для UI;
- запись имеет `updatedAt`, `revision` и `deletedAt`;
- Repository скрывает источник данных;
- сетевой слой позднее записывает результаты синхронизации обратно в Room;
- фоновые попытки синхронизации могут выполняться через WorkManager;
- стратегия разрешения конфликтов определяется отдельно до реализации аккаунтов.

## 13. Минимальный набор тестов модели

1. Конвертация кг и фунтов без накопления ошибки при повторном открытии.
2. Вес одной гантели не удваивается в сохранённом значении.
3. Пустой вес не наследуется без явного действия.
4. Разминочные подходы не влияют на прогрессию.
5. Два последовательных `SUCCESS` создают предложение следующего шага.
6. Один `MISSED` не снижает вес.
7. Два последовательных `MISSED` создают только предложение, а не автоматическое изменение.
8. `FAILURE` до последнего подхода не считается успехом.
9. Боль или нарушение техники блокируют увеличение нагрузки.
10. Для гравитрона уменьшение помощи считается прогрессией.
11. Изменение шаблона не меняет прошлую сессию.
12. Активная сессия и таймер восстанавливаются после перезапуска процесса.
13. Удаление шаблона не удаляет историю.
14. Резервная копия проходит цикл экспорт → восстановление без потери данных.

