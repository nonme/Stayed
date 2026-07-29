# Заметки по декомпилированным модам

Декомпилированный код: `OTHER_MODS_EXAMPLES/*-decompiled/`
Декомпилятор: CFR 0.152 (`OTHER_MODS_EXAMPLES/cfr.jar`)

---

## JET (Just Enough Tales) v1.10.4

**Пакет:** `dev.hytalemod.jet`
**Тип:** Item encyclopedia с рецептами, поиском, гридом предметов

### Архитектура UI
- Использует `.ui` DSL файлы (`Pages/JET_Gui.ui`) + `builder.appendInline()` для динамических элементов
- Один огромный `JETGui extends InteractiveCustomUIPage<GuiData>` — ~1500 строк
- GuiData — сложный EventData с ~20 полями для разных UI событий

### Ключевые UI паттерны

**Текстовый ввод (поиск):**
```java
// В .ui файле: TextField элемент с id #SearchInput
// В build():
cmd.set("#SearchInput.Value", this.searchQuery);
events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput",
    EventData.of("@SearchQuery", "#SearchInput.Value"), false);
// В handleDataEvent():
if (data.searchQuery != null) { this.searchQuery = data.searchQuery.trim(); }
```

**Dropdown:**
```java
List<DropdownEntryInfo> entries = new ArrayList<>();
entries.add(new DropdownEntryInfo(LocalizableString.fromString("Label"), "value"));
cmd.set("#DropdownId.Entries", entries);
cmd.set("#DropdownId.Value", currentValue);
events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DropdownId",
    EventData.of("@FieldName", "#DropdownId.Value"), false);
```

**Динамическое добавление UI элементов (item grid):**
```java
// appendInline() — добавляет DSL-строку внутрь контейнера
cmd.appendInline("#GridContainer",
    "ItemSlotButton #Item_" + idx + " { Anchor: (Width: 50, Height: 50); }");
// Потом устанавливаем свойства:
cmd.set("#Item_" + idx + ".ItemId", itemId);
```

**ItemIcon** — нативный UI элемент для отображения иконки предмета:
```java
cmd.appendInline("#Container", "ItemIcon { Anchor: (Width: 64, Height: 64); Visible: true; }");
cmd.set("#Container[0].ItemId", "Some_Item_Id");
```

**ItemSlotButton** — кликабельный слот предмета:
- Поддерживает Activating, DoubleClicking, RightClicking, MouseEntered, MouseExited
- Свойства: `.ItemId`, `.LayoutMode`

**Checkbox (нативный):**
```java
// В .ui: CheckBox элемент
cmd.set("#CheckBoxId #CheckBox.Value", true/false);
events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CheckBoxId #CheckBox",
    EventData.of("@FieldName", "#CheckBoxId #CheckBox.Value"), false);
```

**Pagination:**
```java
events.addEventBinding(CustomUIEventBindingType.Activating, "#PrevPage",
    EventData.of("PageAction", "prev"), false);
events.addEventBinding(CustomUIEventBindingType.Activating, "#NextPage",
    EventData.of("PageAction", "next"), false);
```

**Открытие вложенной страницы (из одной CustomUIPage в другую):**
```java
this.close(); // закрыть текущую
Player player = store.getComponent(ref, Player.getComponentType());
player.getPageManager().openCustomPage(ref, store, newPage);
```

### Другие находки
- `InventoryScanner` — сканирует инвентарь игрока для подсчёта материалов
- `RecipeHud extends CustomUIHud` — HUD для пиннутых рецептов
- `BrowserState` — сохраняет состояние UI между открытиями (через ECS component)
- Использует `AssetModule`, `Item`, `CraftingRecipe`, `MaterialQuantity` из API
- `cmd.set("#El[index].Prop", val)` — обращение к динамическим дочерним элементам по индексу

---

## HyUI v0.9.4

**Пакет:** `au.ellie.hyui`
**Тип:** UI-библиотека (builder API + HYUIML парсер)
**Зависимости:** jsoup (HTML parser), встроен в -all.jar

### Архитектура
- `HyUIPage extends InteractiveCustomUIPage` — базовая страница
- `HyUIHud extends CustomUIHud` — базовый HUD
- Builders генерируют .ui DSL динамически через `appendInline()` или подгружают .ui файлы
- Каждый builder = один UI элемент (кнопка, текстовое поле, прогрессбар и т.д.)

### Доступные UI элементы (из builders)

**Нативные (встроены в Hytale engine):**
- `Label` — текст
- `TextButton` — кнопка с текстом
- `Button` — кнопка с иконкой
- `ItemSlotButton` — слот предмета (кликабельный)
- `ItemSlot` — слот предмета (только отображение)
- `ItemIcon` — иконка предмета
- `ItemGrid` — грид предметов
- `TextField` — текстовое поле ввода (одна строка)
- `MultilineTextField` — многострочный ввод
- `ProgressBar` — прогрессбар (линейный)
- `CircularProgressBar` — круговой прогрессбар
- `CheckBox` — чекбокс
- `DropdownBox` — выпадающий список (с DropdownEntryInfo)
- `Slider` / `FloatSlider` — ползунок
- `NumberField` — числовое поле
- `TimerLabel` — таймер
- `HotkeyLabel` — отображение горячей клавиши
- `TabNavigation` / `TabContent` — вкладки
- `Sprite` — спрайт/анимация
- `Image` / `DynamicImage` — изображения
- `HyvatarImage` — аватар Hytale
- `SceneBlur` — блюр сцены за UI
- `BlockSelector` — выбор блока
- `CodeEditor` — редактор кода (!)
- `ColorPicker` — пикер цвета
- `ReorderableList` — перетаскиваемый список
- `MenuItem` / SubMenu — контекстное меню

**Ключевые свойства TextField (текстовый ввод):**
```
.Value — текущий текст
.PlaceholderText — плейсхолдер
.MaxLength — макс длина
.IsReadOnly / .ReadOnly — только чтение
.Password — режим пароля
.AutoGrow — автоувеличение
```
Events: ValueChanged, FocusLost, FocusGained, Validating

**Ключевые свойства ProgressBar:**
```
.Value — float 0.0-1.0
.Direction — направление
.Alignment — выравнивание
.BarTexturePath, .EffectTexturePath — текстуры
```

**ItemSlot свойства:**
```
.ItemId — ID предмета для отображения
.ShowQualityBackground — показать фон качества
.ShowQuantity — показать количество
```

### Событийная модель
- `ValueChanged` — для текстовых полей, чекбоксов, dropdown'ов
- `Activating` — для кнопок
- `DoubleClicking`, `RightClicking` — для ItemSlotButton
- `MouseEntered`, `MouseExited` — для ховеров
- `SlotClicking` — для слотов инвентаря
- `FocusLost`, `FocusGained` — для текстовых полей
- `SelectedTabChanged` — для вкладок

### Утилиты
- `PropertyBatcher` — батчинг UI property updates
- `MultiHudWrapper` — поддержка нескольких HUD одновременно
- `UIFileParser` — парсинг .ui DSL файлов программно

---

## NPCDialog v1.2.3

**Пакет:** (не декомпилирован)
**Описание из txt:** F-key NPC диалоги с пагинацией
- Использует InteractiveCustomUIPage
- Поддержка custom buttons (2 на страницу) с командами
- Текстовое форматирование (bold, italic, цвет)
- Сохранение в JSON файлы (не ECS)
- Idle анимации NPC при взаимодействии
- Custom interaction hints

---

## NPCQuests v1.0.3

**Пакет:** (не декомпилирован)
**Описание:** Квестовая система (аддон к NPCDialog)
- Требования: предметы, NPC, кастомные
- Награды: предметы, команды
- Цепочки квестов (branching)
- API: RequirementChecker, RewardProvider

---

## MoreNPC v2.0.5

**Описание:** Новые расы NPC с деревнями
- Grungs (болото), Tuluk (льды), Slothian (джунгли), Saurian (хищные), Bramblekin (горы), Humans
- Использует prefab'ы для деревень
- Каждая раса со своим оружием и поведением
- Зависимость: Shared Structures (заброшена!)

---

## MultipleHUD v1.0.6

**Описание:** Обёртка для нескольких HUD
- `MultipleHUD.getInstance().setCustomHud(player, playerRef, "HudName", hud)`
- Решает проблему Hytale "один HUD на игрока"

---

## AdvancedItemInfo v1.0.6

**Описание:** Браузер предметов с доп. инфой
- Команда `/advancedinfo`
- Показывает ID, quality, durability, crafting info

---

## Важные выводы для Hearthbound

1. **TextField** — нативный элемент Hytale, работает через `.Value` + `ValueChanged`. Можно реализовать ввод имени деревни без зависимостей.

2. **ItemSlotButton** — нативный, показывает иконку предмета. Можно использовать для UI инвентаря (отображение требуемых/имеющихся ресурсов).

3. **appendInline()** — ключевой метод для динамических UI. Генерируем DSL-строку, вставляем в контейнер.

4. **ProgressBar** — нативный, для отображения прогресса стройки.

5. **DropdownBox** — нативный, для выбора (профессии, стиля здания и т.д.).

6. **PageManager.openCustomPage()** — для навигации между UI страницами.

7. **InventoryScanner** — JET сканирует инвентарь игрока. Значит API позволяет читать содержимое инвентаря.

8. **MaterialQuantity** — API класс для "X единиц материала Y". Используется в рецептах крафта.

9. **Inventory item removal** (из NPCQuests):
```java
CombinedItemContainer container = player.getInventory().getCombinedHotbarFirst();
ItemStack itemToRemove = new ItemStack(itemId, (int)((short)quantity));
ItemStackTransaction tx = container.removeItemStack(itemToRemove, true, true);
if (tx.succeeded()) { /* items removed */ }
// Добавление:
ItemStackTransaction tx2 = container.addItemStack(new ItemStack(itemId, quantity));
```

10. **ItemStackTransaction** — результат операции с инвентарём. `.succeeded()` для проверки.

11. **ItemGrid** — нативный UI элемент грида предметов. Свойства: `.Slots`, `.SlotsPerRow`, `.AreItemsDraggable`, `.InventorySectionId`, `.DisplayItemQuantity`.

12. **ItemGridSlot** — нативный класс для слотов. `.setActivatable(true)` для кликабельности.

---

## WhereThisAt v1.0.8

**Пакет:** `com.buuz135.wherethisat`
**Тип:** Inventory browser — ищет предметы во всех контейнерах рядом, может извлекать/класть
**Код:** `OTHER_MODS_EXAMPLES/WhereThisAt-decompiled/`

### Ключевое открытие: Block Container Java API

**Доступ к инвентарю блока-контейнера (сундук, бочка и т.д.):**
```java
// world.getState() возвращает BlockState блока
BlockState blockState = world.getState(x, y, z, true);
if (blockState instanceof ItemContainerState) {
    ItemContainerState containerState = (ItemContainerState) blockState;
    ItemContainer inventory = containerState.getItemContainer();

    // Чтение содержимого:
    for (short i = 0; i < inventory.getCapacity(); i++) {
        ItemStack stack = inventory.getItemStack(i);
        if (stack != null && !stack.isEmpty()) {
            String itemId = stack.getItem().getId();
            int qty = stack.getQuantity();
        }
    }

    // Добавление предметов в контейнер:
    ItemStackTransaction tx = inventory.addItemStack(itemStack);
    if (tx.succeeded()) { /* ok */ }

    // Извлечение из конкретного слота:
    ItemStackSlotTransaction tx = inventory.removeItemStackFromSlot(slot, amount);
    if (tx.succeeded()) { tx.getOutput(); /* extracted ItemStack */ }

    // Добавление в конкретный слот:
    inventory.addItemStackToSlot(slot, itemStack);

    // Проверка наличия:
    ItemStack.isSameItemType(stack1, stack2); // сравнение типов
    inventory.canAddItemStack(stack); // boolean — есть ли место

    // Позиция блока в мире:
    Vector3d centered = containerState.getCenteredBlockPosition();

    // Открытые окна (кто смотрит в контейнер):
    containerState.getWindows(); // пустой = никто не смотрит

    // Очистка:
    inventory.clear();
}
```

### Классы:
- `com.hypixel.hytale.server.core.universe.world.meta.BlockState` — базовый BlockState
- `com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState` — BlockState для контейнеров
- `com.hypixel.hytale.server.core.inventory.container.ItemContainer` — интерфейс инвентаря (тот же что у игрока!)
- `com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction` — транзакция для конкретного слота

### SimpleBlockInteraction — кастомное взаимодействие с блоком
```java
public class MyInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<MyInteraction> CODEC =
        BuilderCodec.builder(MyInteraction.class, MyInteraction::new).build();

    protected void interactWithBlock(World world, CommandBuffer<EntityStore> cb,
        InteractionType type, InteractionContext ctx, ItemStack item,
        Vector3i pos, CooldownHandler cooldown) {
        Ref ref = ctx.getEntity();
        Store store = ref.getStore();
        Player player = (Player)store.getComponent(ref, Player.getComponentType());
        // ...
    }
}
// Регистрация:
this.getCodecRegistry(Interaction.CODEC).register("MyInteraction", MyInteraction.class, MyInteraction.CODEC);
```

### ChunkStore компоненты — данные на уровне чанка
```java
// Регистрация компонента для блоков (привязан к чанку, не к entity):
ComponentType<ChunkStore, MyComponent> type =
    this.getChunkStoreRegistry().registerComponent(MyComponent.class, "name", MyComponent.CODEC);

// Получение:
Ref blockRef = worldChunkComponent.getBlockComponentEntity(x, y, z);
MyComponent comp = chunkStore.getStore().getComponent(chunkStoreRef, type);
```

### Config API — файл конфигурации мода
```java
Config<FindConfig> CONFIG = this.withConfig("ModName", FindConfig.CODEC);
CONFIG.save();
CONFIG.get().getRange(); // чтение конфига
```

### UI паттерны из WhereThisAt
- `cmd.clear("#ContainerId")` — очистить содержимое контейнера UI
- `cmd.append("#Container[row]", "Pages/MyElement.ui")` — добавить .ui файл как дочерний элемент
- `cmd.set("#Container[row][col] #ElementId.Prop", value)` — обращение по вложенным индексам
- Tooltip: `cmd.set("#El.TooltipTextSpans", MessageHelper.multiLine().append(...).build())`
- `Message.translation("key").bold(true).color("#hex")` — форматированный текст
- `Message.raw("text")` — обычный текст
- `ItemStackSlotTransaction` — отличается от `ItemStackTransaction`: работает с конкретным слотом
- `item.getMaxStack()` — максимальный размер стека предмета
- `item.getTranslationKey()` — ключ локализации предмета
- `I18nModule.get().getMessage(language, translationKey)` — получить переведённое название

### Ещё полезные API из WhereThisAt
- `world.getBlockType(x, y, z)` → `BlockType` — тип блока по координатам
- `blockType.getId()` — строковый ID блока
- `blockType.getHitboxType()` — тип хитбокса
- `ParticleUtil.spawnParticleEffect("EffectId", position, store)` — спавн частиц
- `SoundEvent.getAssetMap().getIndex("SoundId")` — получить индекс звука
- `SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, SoundCategory.UI)` — проиграть звук
- `Universe.get().getPlayer(uuid)` — получить PlayerRef по UUID

---

## Simply-Trash v1.0.2

**Пакет:** `com.blamejared.simplytrash`
**Тип:** Trash can block — удаляет предметы при закрытии контейнера
**Код:** `OTHER_MODS_EXAMPLES/Simply-Trash-decompiled/`

### Кастомный BlockState с контейнером и тиком
```java
public class TrashCanBlockState extends ItemContainerState implements TickableBlockState {
    public static final Codec<TrashCanBlockState> CODEC =
        BuilderCodec.builder(TrashCanBlockState.class, TrashCanBlockState::new).build();

    public void tick(float v, int i, ArchetypeChunk<ChunkStore> chunk,
                     Store<ChunkStore> store, CommandBuffer<ChunkStore> commandBuffer) {
        if (this.getWindows().isEmpty()) {  // никто не смотрит
            this.getItemContainer().clear();  // очистить
        }
    }
}
// Регистрация:
this.getBlockStateRegistry().registerBlockState(
    TrashCanBlockState.class, "StateName", TrashCanBlockState.CODEC,
    ItemContainerState.ItemContainerStateData.class,
    ItemContainerState.ItemContainerStateData.CODEC);
```

### Ключевые выводы:
- `ItemContainerState` можно наследовать для кастомной логики
- `TickableBlockState` — интерфейс для блоков с тиком (периодическая логика)
- `getWindows().isEmpty()` — проверка, не открыт ли контейнер кем-то
- `getItemContainer().clear()` — очистить содержимое
- `getBlockStateRegistry().registerBlockState()` — регистрация кастомного BlockState

---

## GrabFromFar v1.4.0

**Пакет:** `com.linceros.grabfromfar`
**Тип:** Увеличивает радиус поиска материалов для крафта из сундуков
**Код:** `OTHER_MODS_EXAMPLES/GrabFromFar-decompiled/`

### Ключевые находки:
- Использует reflection для изменения `CraftingConfig` полей:
  - `benchMaterialHorizontalChestSearchRadius`
  - `benchMaterialVerticalChestSearchRadius`
  - `benchMaterialChestLimit`
- `GameplayConfig.getAssetMap().getAsset("Default")` → `CraftingConfig`
- `this.getDataDirectory()` — папка мода для конфигов (Files API)
- `player.getPageManager().openCustomPage(ref, store, page)` — открытие UI из команды
- `context.senderAs(Player.class)` — получить Player из CommandContext
- `Universe.get().getPlayer(player.getUuid())` — PlayerRef из Player

---

## BetterWardrobes v1.0.3

**Тип:** Чисто JSON-мод — добавляет инвентарь к шкафам (без Java)
**Ключевой JSON для превращения блока в контейнер:**
```json
{
  "BlockType": {
    "Flags": { "IsUsable": true },
    "Interactions": {
      "Primary": "Break_Container",
      "Use": "Open_Container"
    },
    "State": {
      "Id": "container",
      "Capacity": 54,
      "Definitions": {
        "OpenWindow": {
          "InteractionSoundEventId": "SFX_Chest_Wooden_Open",
          "CustomModelAnimation": "Blocks/Animations/Wardrobe/Wardrobe_Open.blockyanim"
        },
        "CloseWindow": {
          "InteractionSoundEventId": "SFX_Chest_Wooden_Close",
          "CustomModelAnimation": "Blocks/Animations/Wardrobe/Wardrobe_Close.blockyanim"
        }
      }
    },
    "VariantRotation": "NESW"
  }
}
```

### Важно для Hearthbound:
- `"Primary": "Break_Container"` — при разрушении дропает содержимое
- `"Use": "Open_Container"` — F-key открывает контейнер
- `"Flags": { "IsUsable": true }` — флаг для интерактивных блоков
- `"Capacity": 54` — количество слотов
- `"State": { "Id": "container" }` — это связывает блок с `ItemContainerState`

---

## Thorium-Chests v1.0.1

**Тип:** Чисто JSON-мод — добавляет сундуки из разных металлов (разный Capacity)
- Аналогичная структура JSON что и BetterWardrobes
- Подтверждает что `Capacity` можно задать любой (от 27 до 63)
