# Результаты ресёрча — Этап R

> Дата: 19 марта 2026
> Статус: Завершён

---

## R1 — Расы и NPC в игре

### Расы, которые реально спавнятся в мире

Hytale (Early Access с 13 января 2026). Мир — планета Орбис, 4 зоны:

| Зона | Раса | Отношение | Биом | Структуры |
|------|------|-----------|------|-----------|
| 1 — Emerald Wilds | **Kweebec** | Дружелюбные | Леса, болота, равнины | Деревни на деревьях |
| 1 — Emerald Wilds | **Trork** | Враждебные | Леса, предгорья | Лагеря, укреплённые аванпосты |
| 2 — Howling Sands | **Feran** | Нейтральные | Саванна, оазисы | Племенные поселения |
| 2 — Howling Sands | **Scarak** | Враждебные | Пустыня, каньоны | Ульевые структуры |
| 3 — Whisperfrost | **Outlander** | Враждебные | Снежные равнины, тайга | Лагеря, заброшенные деревни |
| 3 — Whisperfrost | **Faun** | Неизвестно | Зона 3 | Неподтверждено (возможно не реализованы) |
| 4 — Devastated Lands | **Void Spawn** | Враждебные | Вулканические пустоши | Руины, крепости |
| Несколько зон | **Humans** | Разное | Несколько зон | Подтверждены в Howling Sands |

### Детали по ключевым расам

**Kweebec:** небольшие гуманоиды, живут в деревнях среди деревьев. Дружелюбны, но паникуют и убегают если у игрока топор. Философски связаны с природой — не рубят деревья. Идеальная база для мирных жителей деревни.

**Feran:** кошко/лисоподобные существа, живут племенными общинами в саванне. Нейтральны, гордая раса. Хороши для нейтральных торговых поселений.

**Trork:** крупные тролли-охотники, индустриальная фракция. Враждебны. Могут быть интересны как "индустриальная" раса деревни.

**Humans:** универсальная база, уже используются в моде MoreNPC.

### Мод MoreNPC (CurseForge)

- **Автор:** BlueEyesWhiteMen
- **Загрузки:** 16 900+
- **Обновлён:** 19 февраля 2026
- **Зависимость:** Shared Structures

**Добавленные расы:** Grung (болота), Tuluk (лёд), Slothian (джунгли), Saurian (джунгли, враждебные), Bramblekin (горы, враждебные), Humans.

**Что уже сделано:** рандомизация скинов, деревни и структуры, торговцы. Мод фокусируется на **добавлении новых рас** — наш мод про **строительство и управление деревней**, конкуренции нет.

### Другие NPC-моды на CurseForge

- NPC Dialog — система диалогов
- NPC Quests — система квестов
- Hycompanion — AI-компаньоны
- NPC Control — управление NPC
- HyRise NPCs — доп. NPC
- Heroes MMORPG — левелинг, классы, фракции

### Решение по расам для MVP

**Рекомендация: Kweebec + Feran**
- Kweebec — стартовая раса (зона 1, дружелюбные, уже имеют деревни)
- Feran — вторая раса (зона 2, нейтральные, другой биом = причина исследовать мир)
- Обе имеют мирное поведение, что проще для жителей деревни
- Разные биомы = разные ресурсы и эстетика

---

## R2 — Modding API и инструменты

### Общая архитектура

- **Серверная архитектура** — даже в сингле запускается локальный сервер
- Плагины = Java `.jar` с манифестом, наследуют `JavaPlugin`
- Игрокам не нужно скачивать клиентские моды

### Стек разработки

- **Java 25 JDK** (обязательно)
- **IntelliJ IDEA** (рекомендована, подойдёт Community Edition; VS Code тоже можно)
- **Gradle** (Kotlin DSL)
- Шаблон: `git clone https://github.com/HytaleModding/plugin-template.git`
- Dev-сервер: `./gradlew devServer`

### Система событий

**Два типа:**

1. **Стандартные события** — `registerGlobal()`:
```java
this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);
```

2. **ECS-события** — наследование `EntityEventSystem`:
```java
class ExampleHandler extends EntityEventSystem<EntityStore, PlaceBlockEvent> { ... }
this.getEntityStoreRegistry().registerSystem(new ExampleHandler());
```

**Доступные события:** `PlayerReadyEvent`, `PlayerChatEvent`, `PlaceBlockEvent`, `BreakBlockEvent`, `AddWorldEvent`, `RemoveWorldEvent`, `CraftRecipeEvent.Pre`, `PlayerInteractEvent` (устарело — использовать PlayerInteractLib).

### Спавн NPC

```java
Pair<Ref<EntityStore>, INonPlayerCharacter> result =
    NPCPlugin.get().spawnNPC(store, "Kweebec_Sapling", null, position, rotation);
```

Поведение задаётся через JSON Role-файлы (150+ типов элементов: сенсоры, действия, движения). Менять роль NPC можно динамически.

### Кастомный UI

- **Custom Pages** — полноэкранные интерактивные оверлеи (магазины, диалоги, меню)
- **Custom HUDs** — постоянные элементы поверх игры (квест-трекеры, полоски здоровья)
- Разметка через `.ui` файлы (аналог HTML+CSS)
- Java-код строит UI через `UICommandBuilder`
- Селекторы: `#MyButton`, `#List[0]`, `#Label.TextColor`
- Библиотека **HyUI** на CurseForge — готовые кнопки, инпуты, layouts

### Система Beacon (сообщения между NPC)

NPC рассылают broadcast-сообщения, другие NPC слушают через Beacon-сенсор:
```json
{ "Sensor": { "Type": "Beacon", "Message": "Annoy_Ogre", "Range": 5 } }
```
Также есть FlockBeacon (координация групп) и Spawn Beacons (вызов существ).

### Примеры модов и шаблоны на GitHub

| Репозиторий | Описание |
|---|---|
| `HytaleModding/plugin-template` | Официальный шаблон |
| `Build-9/Hytale-Example-Project` | Gradle-проект для IDEA |
| `realBritakee/hytale-template-plugin` | Минимальный шаблон |
| `Elliesaur/Hytale-Example-UI-Project` | Пример с HyUI |
| `Tutorials-By-Kaupenjoe/Hytale-Tutorial-Plugin` | К YouTube-туториалам |

### Документация

- [hytalemodding.dev](https://hytalemodding.dev/en) — главный общественный ресурс, 21 язык
- [britakee-studios.gitbook.io](https://britakee-studios.gitbook.io/hytale-modding-documentation) — подробная документация
- [hytale-docs.com](https://hytale-docs.com/docs/modding/plugins/overview) — общественная документация
- [doctale.dev](https://doctale.dev/) — серверный моддинг
- **Kaupenjoe** (@ModdingByKaupenjoe) — официальный Модинг-Амбассадор Hytale

---

## R3 — Технические ограничения

### Программная расстановка блоков — РАБОТАЕТ

```java
world.setBlock(x, y, z, blockId);           // через World API
chunk.setBlock(x, y, z, blockState);         // через Chunk API
```
Также доступны команды: `/block set`, `/fillblocks` (заполнение области).

**Вывод:** строительство зданий программно полностью реализуемо.

### Инвентарь NPC и передача предметов — РАБОТАЕТ

```java
npcComponent.setInventorySize(3, 9, 0);
inventory.getHotbar().addItemStackToSlot((short) 0, new ItemStack("Weapon_Mace_Thorium", 1));
```
Передача NPC→игрок: извлекаешь `ItemStack` из инвентаря NPC, добавляешь в инвентарь игрока. Также система NPC Dialog поддерживает выдачу предметов как наград.

### API для зон/территорий — ЧАСТИЧНО

- Встроенная система `Zone` — для генерации мира (биомы, регионы), **не для защиты территорий**.
- Для игровых территорий **надо делать своё** или использовать OrbisGuard/EasySafeZone.
- Для нашего мода: собственная простая система зон (радиус от Town Hall).

### Persistent Data — РАБОТАЕТ

Через ECS кастомные компоненты:
```java
store.putComponent(entityRef, data);    // персистентно (сохраняется между сессиями)
store.addComponent(entityRef, data);    // только в памяти (временно)
```
Формат хранения: BSON. Есть `BuilderCodec` для типобезопасной сериализации.

**Вывод:** состояние деревни можно сохранять через `putComponent()`.

### Правила конкурса и AI

- **AI-визуальные ассеты ЗАПРЕЩЕНЫ** (текстуры, модели, иконки, логотипы) — дисквалификация
- **AI-код НЕ запрещён** — правила не содержат ограничений на использование AI для кода
- Для уточнений: Discord организаторов

### Hot Reload

- **JSON/Data-файлы** — горячая перезагрузка нативно
- **UI-файлы (.ui)** — требуют "жёсткую" перезагрузку
- **Java-код** — через мод MDevTools (отслеживает JAR и перезагружает автоматически)

---

## R4 — Настройка окружения (чеклист)

- [ ] Установить **Java 25 JDK** (OpenJDK 25+)
- [ ] Установить **IntelliJ IDEA Community** (или VS Code + Extension Pack for Java)
- [ ] Клонировать шаблон: `git clone https://github.com/HytaleModding/plugin-template.git convergence-mod`
- [ ] Запустить `./gradlew devServer` — убедиться что dev-сервер стартует
- [ ] Написать hello world: сообщение в чат при `PlayerReadyEvent`
- [ ] Установить **MDevTools** для hot reload Java-кода
- [ ] Протестировать цикл: изменить код → `./gradlew shadowJar` → MDevTools перезагрузит

---

## Конкурс "New Worlds"

| Параметр | Значение |
|---|---|
| Призовой фонд | $100,000 |
| Победителей | 65 |
| Наши категории | **NPCs** + **Experiences** |
| Открытие приёма заявок | 3 марта 2026 |
| **Дедлайн** | **28 апреля 2026** |
| Объявление финалистов | 5 мая 2026 |
| Объявление победителей | 12 мая 2026 |
| Промежуточные розыгрыши | 17 марта, 31 марта, 14 апреля (по $300 x 10) |

### Призы (за категорию)

| Место | Сумма |
|---|---|
| 1-е | $10,000 |
| 2-е | $7,500 |
| 3-е | $2,500 |
| 4-10 | $1,000 каждому |
| Community Favorites (5 мест) | $2,000 |

---

## Оценка рисков после ресёрча

| Риск из SPEC.md | Статус | Комментарий |
|---|---|---|
| Блоки недоступны в API | **СНЯТ** | `world.setBlock()` + `/fillblocks` работают |
| Persistent data не работает | **СНЯТ** | `putComponent()` сохраняет через BSON |
| Расы не спавнятся | **СНЯТ** | Kweebec и Feran подтверждены, имеют деревни |
| Не успеваем Этап 3 | Остаётся | До дедлайна ~5.5 недель |
| UI слишком сложный | **СНИЖЕН** | HyUI библиотека + .ui файлы сильно упрощают |

---

## R5 — Детальный ресёрч механик (Сессия 2)

> Дата: 19 марта 2026
> Источники: веб-документация, серверные логи, ассет-анализ

### Prefab-система (blueprints для строительства)

**Статус: РАБОТАЕТ — поблочное строительство реализуемо**

Три компонента:
- **PrefabStore** — загрузка/сохранение префабов: `getServerPrefab()`, `getAssetPrefab()`, `savePrefab()`
- **BlockSelection** — контейнер блоков (thread-safe), поддерживает блоки, жидкости, сущности
- **PrefabRotation** — повороты: `ROTATION_0, ROTATION_90, ROTATION_180, ROTATION_270`

**Итерация блоков:**
```java
blockSelection.forEachBlock((x, y, z, block) -> {
    int blockId = block.blockId();
    int rotation = block.rotation();
    // ... ставим по одному через world.setBlock()
});
```

**Размещение:**
- `placeNoReturn()` / `place()` — весь префаб разом (с опциональным BlockMask для фильтрации)
- Для поблочного: итерация + `world.setBlock()` по таймеру
- `canPlace()` — проверка что целевая область свободна
- `matches()` — проверка что блоки совпадают с миром

**Трансформации:** повороты на 90°, отражение (flip), смещение координат, объединение через `add()`.

**Мод [PrefabBuilder](https://www.curseforge.com/hytale/mods/prefabbuilder)** — упрощает создание и работу с префабами.

**План поблочного строительства для мода:**
1. Создаём здание в Asset Editor → сохраняем как prefab
2. `PrefabStore.getServerPrefab("building_name")` → `BlockSelection`
3. `forEachBlock()` → собираем `List<(x, y, z, blockId)>` сортированный по Y (снизу вверх)
4. Каждые N тиков NPC ставит один блок через `world.setBlock()`
5. NPC ходит к точке стройки через pathfinding

Источники: [Prefabs Guide](https://hytalemodding.dev/en/docs/guides/prefabs) | [Prefab System Docs](https://hytale-docs.pages.dev/modding/content/prefabs/)

---

### NPC Навигация и Pathfinding

**Статус: РАБОТАЕТ — A* pathfinding**

Архитектура (`com.hypixel.hytale.server.npc.navigation`):
- **AStarWithTarget** — `findPath(startPos, targetPos, evaluator)` находит путь
- **PathFollower** — следует по waypoints: `update(pos, dt)`, `getCurrentWaypoint()`
- **Три контроллера движения:** Walk (ходьба, прыжки, спуск), Fly (полёт), Dive (плавание)

**Конфигурация коллизий в Role:**
```java
collisionProbeDistance;    // дальность обнаружения
collisionRadius;           // радиус детекции
entityAvoidanceStrength;   // сила обхода
avoidanceMode;             // All / Enemies / NonFlock / None
```

**Ограничение:** pathfinding медленный (не критично для производительности, но дальние пути заметно задерживаются).

**Паттерны движения:** Follow Entity, Patrol Path, Wander, Flee — все есть из коробки.

Источник: [Navigation & Pathfinding Docs](https://hytale-docs.pages.dev/modding/npc-ai/navigation/)

---

### NPC Roles — JSON-конфигурация поведения

**938 NPC-конфигураций** загружается при старте сервера.

**Структура Role:**
- Identity: `roleIndex`, `roleName`, `appearance`
- Подсистемы: `CombatSupport`, `StateSupport`, `EntitySupport`, `WorldSupport`, `PositionCache`
- Инвентарь: `inventorySlots`, `hotbarItems[]`, `offHandItems[]`
- Физика: `inertia`, `knockbackScale`, `breathesInAir/Water`

**Динамическая смена Role из Java:**
```java
npcEntity.changeRole(newRoleIndex);  // old role → deactivate → new role → activate
```

**Жизненный цикл:** `activate()` → `tick()` (каждый кадр) → `stateChanged()` → `deactivate()`

**Расписание NPC:** DayNightSensor + TimeSensor в Role JSON. Анимации Sleep, Wake, Eat, Laydown подтверждены для большинства моделей.

Источник: [Roles Docs](https://hytale-docs.pages.dev/modding/npc-ai/roles/) | [NPC Technical Rundown](https://hytale.com/news/2026/2/npc-technical-rundown)

---

### Кастомные предметы и взаимодействия

**Статус: РАБОТАЕТ**

**3,014 Item ассетов** загружается из `/Server/Item/Items/`. Мебель (`Bench_*`, `Furniture_*`) — размещаемые предметы.

**Создание кастомного предмета:**
1. JSON в `assets/Server/Item/Items/Founding_Stone.json`
2. Свойства: `TranslationProperties`, `MaxStack`, `Icon`, `Categories`, `Model`, `Texture`
3. Взаимодействия через Interaction system: `SimpleInstantInteraction`, `Condition`, `Charging`, `Serial`
4. Регистрация: `this.getCodecRegistry(Interaction.CODEC).register("id", Class.class, CODEC)`

**Ловля размещения:** `PlaceBlockEvent` (ECS) — перехватываем постановку Founding Stone и запускаем основание деревни.

Источник: [Create Custom Item](https://hytalemodding.dev/en/docs/guides/plugin/item-interaction) | [Adding a Block](https://britakee-studios.gitbook.io/hytale-modding-documentation/packs-content-creation/03-adding-a-block)

---

### HyUI — библиотека UI

**Статус: РАБОТАЕТ — мощная библиотека**

Установка через Cursemaven (`curse.maven:hyui-1431415:<file-id>`). Зависимости: jsoup (встроен), MultipleHUD (встроен).

**PageBuilder (полноэкранный UI):**
```java
String html = """
    <div class="page-overlay">
        <div class="container" data-hyui-title="Диалог">
            <p id="text">Привет, путник!</p>
            <button id="btn1">Принять</button>
            <button id="btn2">Отказать</button>
        </div>
    </div>
    """;

PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html)
    .addEventListener("btn1", CustomUIEventBindingType.Activating, ctx -> {
        // обработка
    })
    .open(store);
```

**HudBuilder (оверлей):**
```java
HudBuilder.hudForPlayer(playerRef)
    .fromHtml("<div style='anchor-top: 10;'><p>Ресурсы: 50 дерева</p></div>")
    .show(store);
```

**Компоненты:** Button, Label, Image, DynamicImage, TextField, NumberField, Dropdown, CheckBox, ColorPicker, Slider, ProgressBar, TimerLabel, Sprite, ItemIcon, ItemSlot, ItemGrid, TabNavigation.

**HYUIML:** HTML/CSS-подобный синтаксис. Поддержка `id` для event binding, `style` для позиционирования, `data-hyui-title` для заголовков.

Источник: [HyUI Docs](https://hyui.gitbook.io/docs) | [HyUI GitHub](https://github.com/Elliesaur/HyUI) | [Example Project](https://github.com/Elliesaur/Hytale-Example-UI-Project)

---

### PlayerInteractLib

**Статус: НУЖНА зависимость**

Восстанавливает `PlayerInteractionEvent` — в нативном API он не работает/отсутствует. Лёгкая серверная библиотека.

Источник: [PlayerInteractLib](https://www.curseforge.com/hytale/mods/playerinteractlib)

---

### NPC Dialog (мод)

**10,900+ загрузок**, версия 1.2.3 (17 фев 2026). Автор: Hyronix.

**Возможности:**
- F для взаимодействия → полноэкранный UI
- Multi-page диалоги с навигацией (Previous/Next/Close)
- 2 кастомные кнопки с командами (`give @p Item`, `tp @p x y z`)
- Текст-маркап: `{b}`, `{i}`, `{#RRGGBB}`, `{m}`, `{/}`
- Entity states: Frozen, Non-hostile, Invulnerable, Idle Animation
- Persistent JSON storage
- Кастомные Interaction Hints ("Press F to...")
- "Prevent Close Until Last Page" для линейных историй
- Настройка через admin GUI (`/npcdialog`)

**Для нашего мода:** NPC Dialog хорош для прототипирования, но для полного контроля лучше HyUI + PlayerInteractLib + своя система диалогов.

Источник: [NPC Dialog](https://www.curseforge.com/hytale/mods/npc-dialog)

---

### Дополнительные механики

| Механика | Статус | Детали |
|---|---|---|
| **Ресурсы деревни** | РАБОТАЕТ | ECS `putComponent()` на Town Hall entity. BSON persistence. |
| **NPC модели** | ЧАСТИЧНО | Kweebec_Sapling, Goblin, Outlander_Priest подтверждены. Human/Elf — нужна проверка. |
| **Защита территории** | РАБОТАЕТ | `BreakBlockEvent` + `setCancelled(true)`. OrbisGuard/EasySafeZone подтверждают. |
| **Частицы** | РАБОТАЕТ | 1612 ParticleSpawner + 552 ParticleSystem. Nameplate для floating text. |
| **Звуки** | РАБОТАЕТ | 1169 SoundEvent. `WorldSoundEventId` для звуков в мире. |
| **Спавн эльфа** | РАБОТАЕТ | `PlayerReadyEvent` → `NPCPlugin.spawnNPC()` рядом со спавном. |
| **Кастомные блоки** | РАБОТАЕТ | JSON в `assets/` + Asset Editor. Текстуры, материал, звуки. |

### Взаимодействие с NPC (Interaction System)

**Статус: РАБОТАЕТ нативно — 0 зависимостей нужно**

Interaction System построена на ECS:
- **Interaction Types:** Primary (LMB), Secondary (RMB), Tertiary (MMB), Ability 1-4
- **UseEntity** — клиентское взаимодействие с сущностью (RMB на NPC)
- **Server-side interactions:** `OpenPage`, `OpenContainer`, `DamageEntity`, `SpawnPrefab`, `ApplyEffect`
- Торговцы используют interaction instructions в Role JSON → открывают trade UI

**Для диалога по правому клику:**
Role JSON NPC определяет interaction instruction → при RMB вызывается Java handler → `player.getPageManager().openCustomPage(ref, store, new DialogPage(playerRef))`

**Custom UI нативно (без HyUI):**
```java
// Открытие
player.getPageManager().openCustomPage(ref, store, new MyDialogPage(playerRef));
// Закрытие
player.getPageManager().setPage(ref, store, Page.None);
```

- `InteractiveCustomUIPage` — интерактивные страницы с event handling
- `BasicCustomUIPage` — простые страницы без событий
- `CustomUIHud` — постоянные HUD элементы
- `.ui` файлы в `resources/Common/UI/Custom/` — разметка (аналог HTML/CSS)
- `UICommandBuilder` — `append("file.ui")`, `set("#id", value)`
- `UIEventBuilder` — привязка событий: `addEventBinding(CustomUIEventBindingType.ValueChanged, "#input", ...)`
- **ВАЖНО:** после `handleDataEvent()` обязательно `sendUpdate()` иначе клиент зависнет на "Loading..."

Источники: [Custom UI Guide](https://hytalemodding.dev/en/docs/guides/plugin/ui) | [Interaction System](https://hytale-docs.pages.dev/modding/systems/interactions/) | [GUI System](https://hytale-docs.pages.dev/gui/)

---

### Speech Bubbles (плавающий текст над NPC)

**Статус: РЕАЛИЗУЕМО без зависимостей**

Подходы:
1. **Nameplate** — компонент сущности (`"Nameplate": {"Text": "..."}`) — динамически меняется из Java
2. **Invisible entity + Nameplate** — спавним невидимую сущность над NPC, ставим текст, убираем через N секунд
3. Существующие моды: [BubbleChat](https://www.curseforge.com/hytale/mods/bubblechat), [Hycompanion Speech Bubbles](https://www.curseforge.com/hytale/mods/npc-speech-bubbles-hycompanion-extension-plugin), [HologramsPlugin](https://hytalehub.com/resources/hologramsplugin.90/)

---

### NPC Creation (из официальных туториалов разрабов — Pine & Joe)

> Источник: 6-частный видеотуториал от разработчиков Hytale (Pine — контент, Joe — NPC tech)

**Структура NPC:**
- **Role** = поведение + внешность + параметры. Два типа: Template (Abstract/Generic) и Variant
- **Template** — базовое поведение (Abstract Role). **Нельзя спавнить напрямую.**
- **Variant** — спавнимый NPC, ссылается на template, переопределяет: appearance, dropList, health и др.
- **Appearance** — отдельный файл: model, texture, hitbox, attachments. Поддерживает `parent` наследование.
- **Спавн:** `/npc spawn <role_name>` или Creative Tools > World > Entity > NPC (список **roles**)

**Instruction Lists — priority-based decision tree:**
1. `startState: "idle"` — начальное состояние
2. Каждый тик проверяем instructions сверху вниз, первый match → execute → STOP
3. `continue: true` — выполнить ЭТУ инструкцию И проверить СЛЕДУЮЩУЮ (параллельные действия)
4. `actionBlocking: true` — не переходить пока не закончится текущее действие
5. `once: true` — выполнить один раз при входе в состояние (инициализация/cleanup)
6. `treeMode` — вложенные instruction lists

**State Machine (опциональна но рекомендована):**
- Нет предопределённого списка состояний — добавляешь state sensor → state появляется
- Переходы через actions: `setState: "combat"`
- Random action с весами: `weight: 80` guard, `weight: 20` sleep

**State Transitions — анимации между состояниями:**
```
idle→sleep: play "laydown" 1 sec
sleep→*: play "wake"
idle→eat: inventory set hotbar (equip food item)
```
**ВАЖНО:** inventory persistent, state — нет! При перезагрузке NPC может держать item из прошлого state → нужен `once: true` fallback для cleanup.

**Components (Macro Elements):**
- Переиспользуемые блоки поведения в отдельных JSON файлах
- `component_instruction_play_animation_in_state_for_duration`
- `component_instruction_intelligent_chase` — умная погоня с памятью, soft/hard leash
- `component_instruction_state_timeout`
- `component_instruction_idle_motion_follow_path`
- Принимают `modify` блок для переопределения параметров

**NPC Groups & Attitudes:**
- NPC Group = список ролей, поддерживает wildcard (`Rat_*`)
- Attitude = отношения: `ignore, neutral, friendly, hostile, revere` (пока hardcoded)
- `defaultPlayerAttitude: "hostile"` — по умолчанию враждебен к игрокам
- Спец. группы: `self` (свой Role тип), `player` (все игроки)
- Attitudes **ничего не делают сами** — только используются в sensors

**Beacon Spawns:**
- NPC вызывает другого через `triggerSpawnBeacon` action
- Beacon размещается в мире → активация спавнит NPC в заданном state
- `lockedTarget` = вызвавший NPC (для seek behavior)

**Combat:**
- Root Interaction → chaining (swing_left → swing_right → swing_down)
- Selector = hitbox атаки. Timing = ручная подгонка под анимацию.
- Folders: `Server/Item/RootInteractions/` и `Server/Item/Interactions/` (bold = type-defining)

**Pathfinding:**
- `/path new <name>` — создать путь с маркерами
- `component_instruction_idle_motion_follow_path` — NPC следует по маркерам
- Pathfinding **дорогой** — intelligent_chase оптимизирует включение/выключение

**Debug:**
- `"debug": "displayState"` в template → state в nameplate
- `/npc debug set displayState` — команда
- `/npc debug clear` — очистить

**Asset Pack для NPC:**
- Создаётся в Asset Editor (New Asset Pack)
- Мод-папка в `saves/world_name/mods/your_pack/`
- Структура: `Server/NPC/Roles/` для ролей, `Server/NPC/Groups/` для групп
- **Bold folders** = type-defining (NPC Role, NPC Group, Item/RootInteractions и т.д.)

---

### Модель эльфа — РЕШЕНИЕ

Из пользовательского ресёрча: в Hytale аватар игрока собирается из частей (глаза, уши и т.д.). Эльфийские уши доступны в Asset Editor. Фактически "эльф" = человек + эльфийские уши.

**Appearance наследование:**
```
Elf_Sage → parent: Goblin (или humanoid) → переопределяет: model, texture, attachments (эльфийские уши)
```

**План:** создать Appearance файл с humanoid моделью + эльфийские уши через attachments. Проверить можно ли спавнить NPC с аватарной моделью.

---

### Нерешённые вопросы

1. ~~**Модель эльфа**~~ → решено: humanoid + эльфийские уши через Appearance
2. **Human NPC** — проверить `/npc list` или Creative Tools в игре
3. **Ghost blocks** — какой блок использовать для превью строительства?
4. **Time API** — как читать мировое время из Java? (TimeModule)
5. **RMB на NPC** — как именно Role JSON привязывает interaction к Java handler? Нужен пример

---

## Ключевые выводы

1. **Все критичные API доступны** — блоки, NPC, инвентарь, UI, persistent data
2. **NPC поведение data-driven** — JSON Role-файлы, не нужно программировать AI на Java
3. **Kweebec + Feran** — лучший выбор для MVP (дружелюбные + нейтральные, разные зоны)
4. **MoreNPC не конкурент** — они про добавление рас, мы про строительство деревни
5. **AI-код разрешён** на конкурсе, запрещены только визуальные ассеты
6. **Стек:** Java 25 + Gradle + IntelliJ IDEA (или VS Code)
7. **Шаблон готов:** `HytaleModding/plugin-template`
8. **До дедлайна 28 апреля — 40 дней**
9. **Поблочное строительство** — Prefab System + `forEachBlock()` + `world.setBlock()` по таймеру
10. **Диалоги** — нативный UI: InteractiveCustomUIPage + .ui файлы, 0 зависимостей
11. **0 зависимостей** — всё через нативный API (Interaction System, Custom UI, Prefabs)
12. **Модель эльфа** — humanoid + эльфийские уши через Appearance attachments
13. **NPC поведение** — подробно задокументировано из офиц. туториалов (state machine, components, attitudes, beacon spawns)
