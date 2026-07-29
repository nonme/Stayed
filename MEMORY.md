# Hearthbound — Dev Memory

> Этот файл — технический контекст для LLM-ассистента. Читай перед началом работы.
> Обновляй после каждой сессии.
> Геймдизайн → SPEC.md | Ресёрч → RESEARCH.md

---

## Что это

Мод для Hytale — строительство мультирасовой деревни с NPC. Конкурс "New Worlds" на CurseForge, дедлайн **28 апреля 2026**, призовой фонд $100K. Категории: NPCs + Experiences.

Название мода: **Hearthbound** (временное, может измениться).

---

## Текущий статус

**Этап R (Ресёрч):** ✅ Завершён (сессии 1-2)
**Этап 0 (PoC):** ✅ Завершён (сессия 3) — все 8 тестов пройдены
**Этап 1 (Founding flow):** ✅ Завершён (сессии 4-6) — полный flow основания деревни
**Этап 2 (Ресурсная стройка):** ✅ Завершён (сессии 7-8) — block-by-block строительство
**Этап 2.5 (Внешность эльфа):** ✅ Завершён (сессия 9) — кастомный PlayerSkin

### Что работает сейчас:
1. Эльф спавнится у world spawn, F-key диалог, Watch на игрока (Range 3)
2. Founding Stone → ghost preview (Filter_Air_Block) → F → TownHallPage
3. До основания: выбор имени + "Основать" → ghost убирается, эльф у двери
4. После основания: вкладка деревни + вкладка строительства с ресурсами
5. Ctrl+F → нативный контейнер, "Внести ресурсы" переносит из инвентаря
6. "Начать строительство" → эльф телепортируется, ждёт 600мс, freeze, строит block-by-block
7. Эльф держит текущий блок в руке, смотрит на блок (пакетная ротация)
8. Ресурсы потребляются из контейнера, пауза если нет ресурсов
9. Стройка завершена → эльф unfreeze, возвращается к нормальному поведению
10. Кастомная внешность эльфа: белые волосы, зелёные глаза, эльфийские уши, одежда Forest Guardian

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

```
hytale-mod/
├── SPEC.md                    # Геймдизайн документ
├── RESEARCH.md                # Результаты ресёрча
├── MEMORY.md                  # Этот файл — контекст для LLM
├── BLOCKS.md                  # Все 2958 item ID из Assets.zip
├── MOD_NOTES.md               # Анализ декомпилированных модов (читай для паттернов!)
├── HytaleModding-site/        # Клонированная официальная вики по моддингу
│   └── content/docs/en/official-documentation/
│       ├── npc/               # NPC behavior (idle, combat, sleep, inter-NPC)
│       └── custom-ui/         # UI элементы, layout, markup
├── OTHER_MODS_EXAMPLES/       # Декомпилированные моды (подробнее в MOD_NOTES.md)
│   ├── KyuubiSoftCore-decompiled/   # NPC rotation, Frozen, animation
│   ├── Cubia_Companions_unzipped/   # NPC JSON Role patterns (Watch, Sequence, Flock)
│   ├── JET-decompiled/              # UI: item grid, search, pagination
│   ├── HyUI-decompiled/            # Все нативные UI элементы (справочник)
│   ├── NPCQuests-decompiled/       # Inventory add/remove через ItemStackTransaction
│   ├── WhereThisAt-decompiled/     # Block Container API, SimpleBlockInteraction
│   ├── SimplyTrash-decompiled/     # Custom BlockState + TickableBlockState
│   ├── BetterWardrobes/            # JSON container pattern
│   └── AutoSorter-decompiled/      # Crouch detection, UseBlockEvent.Pre intercept
└── convergence/               # Gradle проект мода
    └── src/main/
        ├── java/dev/hearthbound/
        │   ├── HearthboundPlugin.java    # Точка входа
        │   ├── building/                 # BuildingSystem, BuildingGenerator, ResourceBlockPlacer, BlockPlacer
        │   ├── events/                   # BlockPlaceHandler, FoundingStoneHandler, PlayerJoinHandler
        │   ├── npc/                      # ElfSage, NpcManager, BuilderBehavior
        │   ├── ui/                       # TownHallPage, DialogEventData, VillageHud
        │   ├── village/                  # VillageManager, VillageData, BuildingRecord, BuildingType
        │   ├── util/                     # TickScheduler
        │   └── commands/                 # HearthboundCommands, SkinCommand, CosmeticsCommand, ResetCommand
        └── resources/
            ├── Common/UI/Custom/         # .ui файлы (DSL формат!)
            └── Server/NPC/Roles/         # NPC Role JSON файлы
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
```

---

## Правила конкурса (важное)
- AI-визуальные ассеты **ЗАПРЕЩЕНЫ** (текстуры, модели, иконки) = дисквалификация
- AI-код **разрешён**
- Дедлайн: 28 апреля 2026
- Промежуточные розыгрыши: 31 марта, 14 апреля

---

## Известные проблемы
- `ServerVersion: "*"` в manifest.json вызывает WARN — нужно указать конкретную версию (`2026.02.19-1a311a592`)
- JetBrains JDK не установлен → DCEVM hot-swap недоступен (не критично)
- `compdef:153: _comps: assignment to invalid subscript range` — косметический баг zsh + SDKMAN, игнорировать
- Ghost blocks (Filter_Air_Block) не ставятся в определённой зоне вокруг спавна — вероятно spawn protection движка Hytale. Не критично для геймплея (здания ставятся вдали от спавна).

---

## Открытые вопросы
- Финальное название мода
- Нужен ли ServerVersion в manifest.json (сейчас "*")

---

## История сессий

### Сессии 1-2 (19 марта 2026) — Ресёрч
- Полный ресёрч API, рас, механик → RESEARCH.md, SPEC.md
- Настройка окружения, hello world плагин
- 0 зависимостей, якорные блоки, NPC Role system

### Сессия 3 — PoC
- Все 8 тестов пройдены: спавн NPC, блоки, диалог, HUD, persistence, speech bubbles

### Сессии 4-6 — Этап 1 (Founding flow)
- F-key диалог с эльфом, Founding Stone → ghost preview → TownHallPage
- Основание деревни, спавн village elf, полный flow протестирован

### Сессия 7 — Дизайн + декомпиляция модов
- Контейнер Founding Stone, ротация зданий, UI перестройка
- Декомпиляция: WhereThisAt, SimplyTrash, AutoSorter, BetterWardrobes → MOD_NOTES.md

### Сессия 8 — Этап 2 (Ресурсная стройка + поведение NPC)
- ResourceBlockPlacer: block-by-block из контейнера, пауза при нехватке
- BuilderBehavior: Frozen + пакетная ротация к блоку + moveTo перед стройкой
- Elf_Sage.json: Watch player (Range 3), Sequence idle (stand/wander), Wander params
- Декомпиляция: KyuubiSoftCore (rotation, Frozen), Cubia_Companions (JSON patterns)
- Клонирован HytaleModding-site (официальная вики)

### Сессия 9 — Внешность эльфа (PlayerSkin API)
- Исследование CosmeticsModule API: compound IDs (PartId.TextureKey), градиентные наборы
- Декомпиляция `/hb cosmetics` → дамп всех косметик с ключами (devserver/cosmetics_dump.txt)
- Role JSON: `"Appearance": "Player"` вместо `"Outlander"` — обязательно для PlayerSkin
- Программный buildCompound(): registry lookup → gradient set → exact/partial match ключей
- Фикс: cape IDs (типа `Cape_Forest_Guardian.NoNeck`) — bare ID в реестре, НЕ compound
- Фикс: тон кожи — exact match числовых ключей ("01") вместо partial match
- Результат: светлокожий эльф с белыми волосами, зелёными глазами, одежда Forest Guardian
- `/hb skin` теперь делегирует в `ElfSage.createSageSkin()` для единого источника правды
