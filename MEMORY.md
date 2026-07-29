# Stayed — Dev Memory

> Этот файл — **долгоживущий технический контекст** для LLM-ассистента: стек,
> проверенные API-паттерны, грабли. Читай перед началом работы.
>
> ⚠️ **Здесь НЕТ актуального состояния фич.** Что уже сделано, что в работе и
> что менялось последним — смотри в истории коммитов: `git log --oneline`,
> `git log -p <файл>`. Сообщения коммитов подробные и разбиты по подсистемам,
> они и есть источник правды по прогрессу.
>
> Карта исходников → CLAUDE.md | Геймдизайн → SPEC.md | Ресёрч → RESEARCH.md
> Референсные моды → ModsSamples/README.md, MOD_NOTES.md

---

## Что это

Мод для Hytale — строительство мультирасовой деревни с NPC-эльфом (Aelin) в
роли архитектора и советника.

Название мода: **Stayed** (Java-пакет остался `dev.hearthbound` — переименование
пакета сломало бы BSON-ключи компонентов, см. раздел Backward compatibility в
CLAUDE.md).

Делался под конкурс "New Worlds" на CurseForge (дедлайн был 28 апреля 2026,
призовой фонд $100K, категории NPCs + Experiences) — дата прошла, работа
продолжается.

---

## Как узнать текущее состояние

| Вопрос | Где ответ |
|---|---|
| Что уже работает / что сломано | `git log --oneline` + сообщения коммитов |
| Какие есть здания, анкоры, UI-страницы | `village/BuildingType.java`, `Server/Item/Items/Stayed/`, `Common/UI/Custom/` |
| Что за файл и зачем | CLAUDE.md → "Source structure" |
| Известные баги | KNOWN_ISSUES.md |
| Что почитать перед фиксом | ModsSamples/README.md |

---

## Стек и окружение

| Что | Детали |
|---|---|
| Java | OpenJDK 25.0.2 через SDKMAN (`sdk use java 25.0.2-open`) |
| IDE | VS Code / IntelliJ IDEA |
| Сборка | Gradle 9.2.0, Kotlin DSL, ScaffoldIt plugin |
| Шаблон | `HytaleModding/plugin-template` |
| Dev сервер | `./gradlew devServer` (из директории convergence/) |
| Setup сервер | `./gradlew setupServer` (создаёт devserver/) |
| Hytale клиент | Flatpak, ассеты в `~/.var/app/com.hypixel.HytaleLauncher/` |
| Hot reload | JSON/data — нативно. Java — JetBrains JDK (DCEVM) или MDevTools |

---

## Структура проекта

Пофайловая карта живёт в **CLAUDE.md → "Source structure"** — она обновляется
вместе с кодом, здесь дубликат быстро устаревал. Верхний уровень:

```
hytale-mod/                    # локальный рабочий каталог (без remote)
├── *.md                       # SPEC, RESEARCH, BLOCKS, ITEMS, MOD_NOTES, CUSTOM_UI, ...
├── HytaleModding-site/        # клон официальной вики по моддингу (в .gitignore)
├── OTHER_MODS_EXAMPLES/       # ~15 декомпилированных модов (в .gitignore, см. MOD_NOTES.md)
└── convergence/               # git-репозиторий мода → github.com/nonme/Stayed
    ├── *.md                   # копии важных документов, чтобы репо был самодостаточным
    ├── ModsSamples/           # HyCitizens / KyuubiSoftCore / FH_CompanionNPCs — читать перед фиксами
    └── src/main/{java,resources}
```

---

## API шпаргалка

```java
// Точка входа плагина
public class HearthboundPlugin extends JavaPlugin {
    @Override
    protected void setup() {
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, handler::method);
        this.getCommandRegistry().registerCommand(new MyCommand("name", "desc"));
        this.getEntityStoreRegistry().registerSystem(new MyECSHandler()); // ECS события
    }
}

// Спавн NPC
Pair<Ref<EntityStore>, INonPlayerCharacter> result =
    NPCPlugin.get().spawnNPC(store, "Kweebec_Sapling", null, position, rotation);

// Блоки
world.setBlock(x, y, z, blockId);
chunk.setBlock(x, y, z, blockState);
// Команда: /block set <x> <y> <z> <block>
// Заполнение: /fillblocks <pattern>

// Persistent data (BSON)
store.putComponent(entityRef, data);    // сохраняется между сессиями
store.addComponent(entityRef, data);    // только в памяти (временно)
store.getComponent(entityRef, componentType);
store.ensureAndGetComponent(entityRef, componentType); // с дефолтами

// Сериализация через BuilderCodec + KeyedCodec

// Инвентарь NPC
npcComponent.setInventorySize(3, 9, 0);
inventory.getHotbar().addItemStackToSlot((short) 0, new ItemStack("Item", 1));

// Инвентарь игрока
player.getInventory().getStorage().addItemStack(new ItemStack("Stone", 64));

// UI: нативный, 0 зависимостей
// InteractiveCustomUIPage — интерактивные страницы с событиями
// player.getPageManager().openCustomPage(ref, store, new MyPage(playerRef))
// player.getPageManager().setPage(ref, store, Page.None) — закрыть
// .ui файлы в resources/Common/UI/Custom/ (аналог HTML/CSS)
// UICommandBuilder: append("file.ui"), set("#id", value)
// UIEventBuilder: addEventBinding(ValueChanged, "#input", EventData.of("@key", "#el.Value"), false)
// ВАЖНО: после handleDataEvent() обязательно sendUpdate()!
// CustomUIHud — постоянные HUD элементы

// Взаимодействие с NPC: Role JSON interaction → UseEntity (RMB) → Java handler → openCustomPage()
// Speech bubbles: invisible entity + Nameplate компонент

// Prefab система
// PrefabStore.getServerPrefab("name") → BlockSelection
// blockSelection.forEachBlock((x,y,z,block) -> { block.blockId(); })
// blockSelection.place() — разом, или итерация + world.setBlock() — поблочно

// NPC поведение — JSON Role файлы, 150+ типов элементов
// Beacon — система broadcast-сообщений между NPC

// NPC Frozen (останавливает behavior tree):
store.addComponent(ref, Frozen.getComponentType(), Frozen.get());
// ВАЖНО: Frozen НЕ останавливает анимацию! Нужно также:
AnimationUtils.stopAnimation(ref, AnimationSlot.Movement, store);
// Unfreeze:
store.tryRemoveComponent(ref, Frozen.getComponentType());

// NPC перемещение:
entity.moveTo(ref, x, y, z, store); // teleport() НЕ существует

// NPC rotation (пакетная, через клиент):
// yaw = (float)(Math.atan2(dx, dz) + Math.PI); pitch = (float)Math.atan2(dy, hDist);
// Direction → ModelTransform → TransformUpdate → EntityUpdate → EntityUpdates → playerRef.getPacketHandler().write()
// См. BuilderBehavior.java, паттерн из KyuubiSoftCore CitizenRotationManager

// Получить Ref и PlayerRef:
world.getEntityRef(uuid);           // → Ref<EntityStore> (Entity НЕ имеет .getRef()!)
Universe.get().getPlayer(uuid);     // → PlayerRef по UUID

// Block Container API (верифицировано):
BlockState state = world.getState(x, y, z, true);
if (state instanceof ItemContainerState cs) {
    ItemContainer inv = cs.getItemContainer(); // тот же API что и инвентарь игрока
}

// PlayerSkin API (кастомизация NPC):
// Требует "Appearance": "Player" в Role JSON
CosmeticsModule cosmetics = CosmeticsModule.get();
PlayerSkin skin = cosmetics.generateRandomSkin(new Random(42)); // базовый валидный скин
skin.ears = "Elf_Ears_Small";        // bare ID (уши, лицо, рот)
skin.haircut = "ElfBackBun.White";    // compound: PartId.TextureKey
skin.cape = null;                     // null допустим для необязательных полей
Model model = cosmetics.createModel(skin, 1.0f); // бросает InvalidSkinException если невалидный
store.putComponent(ref, PlayerSkinComponent.getComponentType(), new PlayerSkinComponent(skin));
store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(model));
// Дамп всех косметик с ключами: devserver/cosmetics_dump.txt

// События
// Стандартные: registerGlobal(EventClass.class, handler::method)
// ECS: extends EntityEventSystem<EntityStore, EventType>
// ВАЖНО: внутри EntityEventSystem.handle() нельзя store.putComponent() — defer через world.execute()

// Логирование — свой слой, НЕ java.util.logging напрямую:
private static final Log LOG = dev.hearthbound.util.log.Log.get("npc.dup"); // категория с иерархией
LOG.info("...");                                  // простая строка
LOG.with("uuid", uuid).with("role", r).debug(msg); // структурный контекст (уходит в NDJSON-архив)
LOG.atMostEvery("gate:" + pos, 5_000).debug(msg);  // троттлинг ТОЛЬКО консоли, архив видит всё
// Уровни по категориям: mods/HearthboundData/logging.json, рантайм — /hb log set|tail|mute|reload
// Архив: mods/HearthboundData/logs/*.ndjson (ротация 5×5MB) + ring-буфер, /hb log dump → zip для багрепортов
// WARN/ERROR всегда попадают в консоль и не склеиваются
```

---

## Правила конкурса (важное)
- AI-визуальные ассеты **ЗАПРЕЩЕНЫ** (текстуры, модели, иконки) = дисквалификация
- AI-код **разрешён**

---

## Грабли окружения
- JetBrains JDK не установлен → DCEVM hot-swap недоступен (не критично)
- `compdef:153: _comps: assignment to invalid subscript range` — косметический баг zsh + SDKMAN, игнорировать
- `useVersion("latest")` в settings.gradle.kts тянет API с maven и может разрезолвиться
  в версию новее установленной игры — тогда `make build` падает пачкой
  `cannot find symbol` на классах движка (например `com.hypixel.hytale.math.vector.Vector3d`).
  Это не регресс мода: проверяется компиляцией против локального
  `~/.var/app/com.hypixel.HytaleLauncher/.../Server/HytaleServer.jar`.
- `ServerVersion` в manifest.json запинен на конкретный билд движка — при обновлении
  игры его нужно поднимать руками, иначе сервер ругается.

Актуальные баги геймплея → **KNOWN_ISSUES.md** (этот файл их не дублирует).

---

## История разработки

Здесь её больше нет — она жила в формате "сессия N" и устаревала быстрее, чем
обновлялась. Хронология работы полностью лежит в git:

```bash
git log --oneline                 # что и когда делалось
git log -p -- <файл>              # почему конкретная строка выглядит так
git log --stat -1 <commit>        # объём изменения
```

Сообщения коммитов пишутся подробно и разбиты по подсистемам — по ним
восстанавливается и мотивация, и порядок работ. Если нужно понять "что было
сделано в прошлый раз" — это `git log`, а не этот файл.

Что осталось здесь: стек, проверенные API-паттерны выше и грабли окружения.
Их и обновляй — новую проверенную находку по API дописывай в шпаргалку.
