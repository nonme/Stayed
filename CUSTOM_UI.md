# Hytale Custom UI — Reference for LLM

This document covers everything needed to write `.ui` files and wire them to Java without hitting "Failed to load CustomUI documents" client crash. It is written for an LLM that will generate or modify UI code.

---

## File location

`.ui` files go in:
```
src/main/resources/Common/UI/Custom/<Name>.ui
```

Referenced in Java as:
```java
builder.append("ElfDialog.ui");
```

The name is just the filename, no path prefix.

---

## Format — DSL only, never JSON

The `.ui` file MUST use the DSL syntax. JSON object syntax crashes the client immediately with "Failed to load CustomUI documents". There is no error message that tells you this — the client just refuses to connect.

**Wrong (crashes):**
```json
{ "Type": "Group", "LayoutMode": "Middle" }
```

**Correct:**
```
Group {
  LayoutMode: Middle;
}
```

---

## Syntax rules

- Every property ends with `;`
- Children are nested in `{ }` blocks directly inside the parent element
- Element IDs use `#` prefix: `Label #MyLabel { ... }`
- Comments: `// single line only`
- Named expressions: `@Name = Value;` — declared before the tree or at top of any block, referenced with `@Name`
- Colors: `#rrggbb`, `#rrggbb(alpha)` where alpha is 0.0–1.0, or `#rrggbbaa`

---

## Elements

Full list from official docs. Only elements marked "interactive" support event binding in Java.

| Element | Children | Interactive | Notes |
|---|---|---|---|
| `Group` | Yes | No | Container. Main layout primitive |
| `Panel` | Yes | No | Like Group, adds Dismissing/Validating events |
| `Label` | No | No | Text display |
| `TimerLabel` | No | No | Countdown/countup display |
| `TextButton` | No | **Yes** | Button with text label. Supports `Activating` event |
| `Button` | No | **Yes** | Button without text (icon button) |
| `ItemSlotButton` | No | **Yes** | Clickable item slot |
| `ItemGrid` | No | Partial | Item grid, draggable slots |
| `ItemIcon` | No | No | Displays item icon |
| `TextField` | No | **Yes** | Text input. `ValueChanged` event |
| `CheckBox` | No | **Yes** | |
| `DropdownBox` | No | **Yes** | |
| `ProgressBar` | No | No | |
| `Sprite` | No | No | Spritesheet animation |

**CRITICAL: `Group` does NOT support `Activating` or any click event binding.** Only `TextButton`/`Button`/`ItemSlotButton` do. Binding an event to `#SomeGroup` will silently do nothing.

---

## Properties shared by all elements

```
Visible: false;                          // hides + removes from layout (takes no space)
Anchor: (Width: 200, Height: 40);
Padding: (Top: 10, Bottom: 10, Left: 16, Right: 16);
Padding: (Vertical: 10, Horizontal: 16); // shorthand
Padding: (Full: 10);                     // all sides
Background: #1a2030;
Background: #1a2030(0.8);               // with alpha
OutlineColor: #ffffff(0.2);
OutlineSize: 1;                          // float, pixels
FlexWeight: 1;                           // distributes remaining space
```

---

## Anchor

Controls how an element sizes/positions itself inside its container.

```
Anchor: (Width: 200, Height: 40);               // fixed size
Anchor: (Full: 0);                              // stretch to fill parent (all edges at 0)
Anchor: (Full: 10);                             // fill with 10px margin all sides
Anchor: (Top: 8, Height: 2);                    // 2px tall, 8px from top
Anchor: (Bottom: 10, Right: 10, Width: 100, Height: 30); // pinned to bottom-right
Anchor: (Left: 0, Right: 0, Height: 40);        // full width, fixed height
```

`Anchor.Bottom` on a child inside `LayoutMode: Top` adds gap AFTER that child (spacing between items).
`Anchor.Right` on a child inside `LayoutMode: Left` adds gap after that child.

---

## LayoutMode

Applies to container elements (Group, Panel). Controls how children are arranged.

| Value | Behavior |
|---|---|
| `Top` | Vertical stack, top-aligned |
| `Bottom` | Vertical stack, bottom-aligned |
| `Left` | Horizontal stack, left-aligned |
| `Right` | Horizontal stack, right-aligned |
| `Middle` | Vertical stack, vertically centered in parent |
| `Center` | Horizontal stack, horizontally centered in parent |
| `CenterMiddle` | Horizontal stack, centered both axes |
| `MiddleCenter` | Vertical stack, centered both axes |
| `Full` | Children use absolute positioning via their own Anchor |
| `TopScrolling` | Like Top but adds scrollbar when content overflows |
| `LeftScrolling` | Like Left with horizontal scrollbar |
| `LeftCenterWrap` | Horizontal wrap, each row centered |

**`Visible: false` removes the element from layout entirely** — siblings close the gap. Use this to toggle between choice buttons and primary button.

---

## Label

```
Label #MyLabel {
  Anchor: (Full: 0);
  Style: (
    HorizontalAlignment: Center,   // Start | Center | End
    VerticalAlignment: Center,     // Start | Center | End
    TextColor: #c8d4da,
    FontSize: 15,
    Wrap: true,
    RenderBold: true,
    RenderItalics: false,
    LetterSpacing: 1,
    FontName: "Secondary"          // Default | Secondary | Mono
  );
  Text: "Hello";
}
```

**`Alignment` is a shorthand that maps to `HorizontalAlignment`.** Only `Center` works reliably as `Alignment: Center`. **`Alignment: Left` and `Alignment: Right` cause parse failure** ("Could not resolve expression for property Alignment to type LabelAlignment"). Always use `HorizontalAlignment: Start` / `HorizontalAlignment: End` instead.

LabelAlignment values: `Start`, `Center`, `End` — NOT `Left`/`Right`.

For wrapping text to fill a container correctly:
```
Group {
  FlexWeight: 1;
  Padding: (Top: 20, Bottom: 16, Left: 24, Right: 24);

  Label #DialogText {
    Anchor: (Full: 0);   // IMPORTANT: needed so Wrap works against full width
    Style: (FontSize: 14, Wrap: true, TextColor: #b8c8d4);
  }
}
```

---

## TextButton

```
TextButton #BtnOk {
  Anchor: (Height: 36);
  Text: "";   // set from Java via builder.set("#BtnOk.Text", "Click me")

  Style: (
    Default: (
      Background: #2d4a30,
      LabelStyle: (
        HorizontalAlignment: Center,
        VerticalAlignment: Center,   // IMPORTANT: without this text is top-aligned
        TextColor: #b8d4bc,
        FontSize: 14,
        RenderBold: true
      )
    ),
    Hovered: (Background: #3d6440, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #e8f5ea, FontSize: 14, RenderBold: true)),
    Pressed: (Background: #1e3221, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #b8d4bc, FontSize: 14, RenderBold: true))
  );
}
```

**`VerticalAlignment: Center` is required in LabelStyle to center text vertically within the button.** Without it the text renders at the top of the button area regardless of button height.

**`LabelStyle` inside `TextButtonStyleState` does NOT accept `Alignment: Left`** — same restriction as Label.Style. Use `HorizontalAlignment: Start` or omit (default is Start).

Named expressions avoid repetition:
```
@MyBtnStyle = TextButtonStyle(
  Default: (Background: #1e2a35, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #8fa8b8, FontSize: 14)),
  Hovered: (Background: #2a3f52, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #d8eaf5, FontSize: 14)),
  Pressed: (Background: #162028, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #8fa8b8, FontSize: 14))
);

TextButton #BtnChoice1 {
  Anchor: (Height: 32, Bottom: 6);
  Style: @MyBtnStyle;
  Text: "";
}
```

---

## FlexWeight

Distributes remaining space after fixed-size children are laid out.

```
Group {
  LayoutMode: Top;
  Anchor: (Width: 560, Height: 376);

  Group { Anchor: (Height: 48); }   // fixed: 48px
  Group { Anchor: (Height: 1); }    // fixed: 1px
  Group { FlexWeight: 1; }          // gets: 376 - 48 - 1 - 1 - 132 - 42 = 152px
  Group { Anchor: (Height: 1); }    // fixed: 1px
  Group { Anchor: (Height: 132); }  // fixed: 132px
  Group { Anchor: (Height: 42); }   // fixed: 42px
}
```

FlexWeight only works when the parent has a defined size (either explicit Anchor or is itself FlexWeight inside a sized parent).

---

## Named expressions and spread

```
@BaseStyle = LabelStyle(FontSize: 14, TextColor: #ffffff);

Label {
  Style: (...@BaseStyle, FontSize: 18);  // override FontSize, keep rest
}
```

Spread `...` lets you extend a named style without full repetition.

---

## Sizing gotchas

When `Visible: false`, the element takes zero space in layout. Plan button container heights for the **maximum visible state**.

For a container with 3 buttons (32px each, 6px gap after first two, padding top 12 + bottom 10):
```
Height = 12 + 32 + 6 + 32 + 6 + 32 + 10 = 130px → use 132px
```

When only 2 buttons show (no BtnChoice3), container is still 132px but only 82px of content fills it — the empty space just shows the container background. This is fine.

---

## Java wiring

### Page class

```java
public class MyPage extends InteractiveCustomUIPage<MyEventData> {
    public MyPage(PlayerRef playerRef) {
        // CanDismiss = Escape closes it
        // CanDismissOrCloseThroughInteraction = Escape + walking away / interacting with something else closes it
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, MyEventData.CODEC);
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events, Store<EntityStore> store) {
        builder.append("MyPage.ui");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnOk", EventData.of("action", "ok"), false);
        render(builder);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, MyEventData data) {
        // ... update state ...
        UICommandBuilder b = new UICommandBuilder();
        render(b);
        sendUpdate(b, false);  // MUST call sendUpdate or client hangs on next interaction
    }
}
```

**`sendUpdate()` must be called at the end of every `handleDataEvent`, or the client freezes.**

### Setting properties from Java

```java
builder.set("#ElementId.Property", value);
```

Properties that work:
```java
builder.set("#MyLabel.Text", "Hello");
builder.set("#MyLabel.Visible", false);
builder.set("#MyBtn.Text", "Click");
builder.set("#MyBtn.Visible", true);
builder.set("#MyGroup.Visible", false);
```

The `#ElementId` must match exactly the id declared in the `.ui` file. The property name is case-sensitive and must match the documented property name.

### EventData codec

```java
public class MyEventData {
    public static final String ACTION_KEY = "action";
    public static final BuilderCodec<MyEventData> CODEC = BuilderCodec.builder(MyEventData.class, MyEventData::new)
            .append(new KeyedCodec<>(ACTION_KEY, Codec.STRING), MyEventData::setAction, MyEventData::getAction).add()
            .build();

    private String action;
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
```

### Binding multiple buttons

```java
events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnPrimary", EventData.of("action", "primary"), false);
events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnChoice1", EventData.of("action", "choice1"), false);
events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnChoice2", EventData.of("action", "choice2"), false);
```

Then dispatch in handleDataEvent:
```java
String action = data.getAction();
switch (action) {
    case "primary" -> { ... }
    case "choice1" -> { ... }
}
```

---

## Common crash causes

| Symptom | Cause | Fix |
|---|---|---|
| "Failed to load CustomUI documents" | `Alignment: Left` or `Alignment: Right` in any LabelStyle | Replace with `HorizontalAlignment: Start` / `HorizontalAlignment: End` |
| "Failed to load CustomUI documents" | JSON format in `.ui` file | Rewrite as DSL |
| "Failed to load CustomUI documents" | Unknown property name or wrong type | Check property name against docs exactly |
| "Failed to load CustomUI documents" | `.ui` file in wrong directory | Must be in `Common/UI/Custom/` |
| Client hangs after button click | `sendUpdate()` not called in `handleDataEvent` | Add `sendUpdate(builder, false)` at end |
| Button text top-aligned | Missing `VerticalAlignment: Center` in LabelStyle | Add it to all states (Default/Hovered/Pressed) |
| Buttons overflow container | Height math wrong | Sum: padding_top + (btn_height + gap) * n - last_gap + padding_bottom |
| Event binding does nothing | Bound to `Group` instead of `TextButton` | Only `TextButton`, `Button`, `ItemSlotButton` support `Activating` |

---

## Working example

`ElfDialog.ui` — full working dialog with named styles, FlexWeight text area, toggle between choice buttons and primary button, header bar with separator. Located at `convergence/src/main/resources/Common/UI/Custom/ElfDialog.ui`.

`TownHall.ui` — multi-tab UI with tab switching, scrollable content, resource labels, construction progress. Located at `convergence/src/main/resources/Common/UI/Custom/TownHall.ui`.

Both files compile and render correctly as of session 10.

---

## Official docs location

Full element/property reference is in `HytaleModding-site/content/docs/en/official-documentation/custom-ui/`. Key files:
- `markup.mdx` — DSL syntax, named expressions, spread, types
- `layout.mdx` — Anchor, Padding, LayoutMode, FlexWeight with diagrams
- `type-documentation/elements/` — one `.md` per element
- `type-documentation/enums/labelalignment.md` — confirms values are `Start`, `Center`, `End`
- `type-documentation/property-types/labelstyle.md` — all LabelStyle properties
- `type-documentation/property-types/textbuttonstyle.md` and `textbuttonstylestate.md`
