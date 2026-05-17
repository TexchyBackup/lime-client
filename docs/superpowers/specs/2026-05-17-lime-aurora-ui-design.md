# Lime Client · Aurora UI Redesign

**Date:** 2026-05-17
**Status:** Design draft · awaiting plan
**Author:** TexchyBackup (with Claude)

## Goal

Replace the existing Meteor GUI with a custom-designed "Aurora" theme — translucent glass panels with real backdrop blur, soft lime glow accents, smooth animated transitions, and a curated typographic system. Visual language: Apple Vision Pro × Arc Browser × premium hi-fi instrument.

The redesign covers every GUI surface the user touches: module browser, module settings, HUD editor, tab screens (friends, profiles, macros, accounts, baritone, etc.), and the in-game HUD chrome (chat prefix, info-display panels).

## Non-Goals

- Touching mod logic, packet handling, or any non-GUI subsystem.
- Renaming Java packages, class names, or the mod ID (already decided: only user-facing strings were rebranded).
- Preserving the old Meteor theme as a user-selectable option — it is being replaced.
- Theme-engine extensibility for third parties. The renderer additions are general-purpose, but no public theming API is promised.

## Aesthetic Spec

### Color tokens

A single user setting (`Setting<SettingColor> accent`, default `#88EE55`) drives every accent value. All other colors are fixed design tokens.

```
Background gradient        #050A08 → #0A0F0A   (radial, top-right warm)
Panel base                 rgba(20, 30, 22, 0.85)
Panel highlight (top 1px)  rgba(255, 255, 255, 0.08)
Panel border               rgba(<accent>, 0.15)
Panel shadow outer         rgba(0, 0, 0, 0.60)   60px blur, +24 y-offset
Panel ambient glow         rgba(<accent>, 0.08)  80px blur, no offset

Text primary               #E8F5E0
Text secondary             #8A9A8A
Text muted                 #5A6E5A
Text accent                derive(accent, lighten 20%)

Divider                    rgba(255, 255, 255, 0.04)
Divider strong             rgba(255, 255, 255, 0.08)

Active surface             linear(rgba(<accent>, 0.12), transparent)
Hover surface              rgba(255, 255, 255, 0.03)

Slider track               rgba(255, 255, 255, 0.06)
Slider fill                linear(<accent>, lighten(<accent>, 15%))
Slider thumb               #FFFFFF, 4px shadow
Glow halo                  rgba(<accent>, 0.5)  8-12px blur
```

A small palette utility (`AuroraPalette`) exposes derived colors (lightened/darkened accent variants) so widgets reference tokens, not raw values.

### Typography

Three TTFs ship in `src/main/resources/assets/lime-client/fonts/`:

| Font | Use | Sizes |
|------|-----|-------|
| **Inter** (Regular, Medium, SemiBold) | Body UI, setting names, hint text | 11px body, 9-10px secondary |
| **Space Grotesk** (SemiBold, Bold) | Display — module titles, big numbers | 13-16px titles, 22-38px hero |
| **JetBrains Mono** (Regular, Medium) | Numeric values, breadcrumbs, status footer | 10-11px |

All three are SIL OFL licensed. Total bundle: ~1.5 MB.

A new `FontFamily` enum in `gui/renderer/text/` maps logical names (UI, DISPLAY, MONO) to loaded fonts so widgets request fonts symbolically.

### Spacing scale

`4, 8, 12, 16, 22, 32` pixels. No values outside this scale in any Aurora widget code. Defined as constants in `AuroraTheme.Spacing`.

### Corner radius

`Panel = 14px`, `Card / row = 10px`, `Pill / chip = 10px`, `Slider thumb = 6px`. SDF-rendered (see render-kit additions below).

### Motion

All state transitions use `EaseOutCubic` over `180ms` unless noted:

| Event | Property | Duration | Curve |
|-------|----------|----------|-------|
| Window open | scale 0.96→1, opacity 0→1 | 220ms | EaseOutCubic |
| Window close | scale 1→0.98, opacity 1→0 | 140ms | EaseInCubic |
| Toggle flip | thumb x position | 180ms | EaseOutBack |
| Hover enter/leave | bg opacity, glow radius | 120ms | EaseOutCubic |
| Sidebar select | active-bar y position, glow | 220ms | EaseOutCubic |
| Dropdown open | height 0→content, opacity | 200ms | EaseOutCubic |
| Slider drag | value (interpolated, not snap) | continuous | linear |
| Accent pulse (active modules) | glow opacity 0.8↔1.0 | 2.4s loop | sine |

A `Tween` helper in `gui/renderer/anim/` interpolates `float`/`Vec2`/`Color` values toward targets each render frame.

## Architecture

### File layout (additions)

```
src/main/java/meteordevelopment/meteorclient/gui/
├── themes/
│   ├── meteor/                          (existing - deleted in final phase)
│   └── aurora/                          NEW
│       ├── AuroraGuiTheme.java          new GuiTheme subclass
│       ├── AuroraPalette.java           color token + derivation
│       ├── AuroraWidget.java            base for themed widgets
│       └── widgets/
│           ├── WAuroraWindow.java
│           ├── WAuroraView.java
│           ├── WAuroraSection.java
│           ├── WAuroraSidebar.java      NEW widget (sidebar nav)
│           ├── WAuroraBreadcrumb.java   NEW widget
│           ├── WAuroraStatusBar.java    NEW widget
│           ├── WAuroraLabel.java
│           ├── WAuroraModule.java
│           ├── WAuroraTooltip.java
│           ├── WAuroraTopBar.java
│           ├── WAuroraQuad.java
│           ├── WAuroraSeparator.java
│           ├── input/
│           │   ├── WAuroraSlider.java
│           │   ├── WAuroraDropdown.java
│           │   └── WAuroraTextBox.java
│           └── pressable/
│               ├── WAuroraButton.java
│               ├── WAuroraToggle.java   NEW (replaces checkbox visually)
│               ├── WAuroraCheckbox.java
│               ├── WAuroraPlus.java
│               ├── WAuroraMinus.java
│               ├── WAuroraTriangle.java
│               ├── WAuroraFavorite.java
│               ├── WAuroraConfirmedButton.java
│               └── WAuroraConfirmedMinus.java
└── renderer/
    ├── (existing renderer files)
    ├── primitives/                      NEW
    │   ├── RoundedRect.java             SDF rounded-rect quad
    │   ├── Glow.java                    soft outer/inner glow halo
    │   ├── Gradient.java                linear / radial gradient quads
    │   └── BackdropBlur.java            gaussian backdrop blur pass
    ├── text/
    │   └── FontFamily.java              NEW symbolic font lookup
    └── anim/                            NEW
        ├── Tween.java                   value interpolator
        ├── Easing.java                  curves (cubic, back, sine)
        └── Animated.java                wrapper for animated state
```

### Theme class

`AuroraGuiTheme extends GuiTheme` overrides every widget factory method. Settings exposed to the user:

```java
public final Setting<SettingColor> accent;      // default #88EE55
public final Setting<Boolean>      backdropBlur; // default true
public final Setting<Double>       motionScale;  // 0.0-1.5, default 1.0
public final Setting<Boolean>      reducedMotion;// default false
```

Just four user-facing settings (down from ~83 in MeteorGuiTheme). `motionScale` lets users speed up/slow down animations globally; `reducedMotion` disables all easing.

### Render-kit additions

#### `BackdropBlur`

Two-pass separable gaussian blur of the framebuffer. Renders a downscaled (½ or ¼) capture of the scene behind the GUI, blurs it horizontally then vertically, samples it from window draw calls. Radius and downscale exposed for perf tuning.

Cost target: < 1.5ms at 1080p with ½-res + 16-tap radius. Falls back to fake darkening when `backdropBlur=false` or when the user's GPU advertises < OpenGL 3.3.

Reuses the existing `Blur` post-process module's shader where possible.

#### `RoundedRect`

Single quad rendered with an SDF (signed-distance-field) fragment shader for crisp anti-aliased corners at any radius. Inputs: position, size, radius, fill color, stroke color, stroke width. One uniform-bound draw per panel.

#### `Glow`

Soft halo around any rect — implemented as a SDF outer-glow shader (Gaussian falloff) or as 4 stretched edge quads for cheap mode. Used for: panel ambient glow, slider fill bloom, active toggle glow, pulsing module-active indicator.

#### `Gradient`

Linear and radial gradient quads with up to 4 color stops. Used for backdrop gradients, slider fills, accent transitions, the orb in module headers.

#### `Tween` & `Animated<T>`

`Animated<Float>` / `Animated<Vec2>` / `Animated<Color>` wrap a current value and a target value. `update(dt)` eases the current toward the target. Widgets call `set(target)` and read `get()` each frame. Easing curves enumerated in `Easing`.

## Surface Designs

### Module Browser (`ModulesScreen` replacement)

Full-screen overlay with backdrop-blurred game scene.

```
┌─────────────────────────────────────────────────────────────┐
│  ● Lime  / Combat / Killaura                       ⌘ K     │
│ ───────────────────────────────────────────────────────────  │
│                                                              │
│  ┌────────────┬────────────────────────────────────────┐    │
│  │ Combat   ◉│  Killaura                               │    │
│  │ Movement  │  Automatically attacks nearby entities  │    │
│  │ Render    │                                         │    │
│  │ Player    │  ┌─────────────────────────────────┐    │    │
│  │ World     │  │ Enabled               [toggle]  │    │    │
│  │ Misc      │  │ Range          ──●────── 4.5    │    │    │
│  │           │  │ Mode            [ Smart ▾ ]    │    │    │
│  │ ● 3 mods  │  │ Targets         [ Players +2 ] │    │    │
│  │ ○ 47 idle │  └─────────────────────────────────┘    │    │
│  └────────────┴────────────────────────────────────────┘    │
│                                                              │
│ ───────────────────────────────────────────────────────────  │
│  FPS 240 · TPS 20.0                          3 mods active   │
└─────────────────────────────────────────────────────────────┘
```

- **Title bar**: accent dot, "Lime" wordmark in Space Grotesk SemiBold, breadcrumb in Inter Regular, ⌘K hint pill on the right (opens the search palette, see below). Bottom 1px gradient divider that fades at the edges.
- **Sidebar (`WAuroraSidebar`)**: 110px fixed width. Category items with leading dot, active item gets a `linear(accent 0.12 → transparent)` row background and a 2px left border in accent. Below categories: status block with active/inactive module count.
- **Content area**: Module title in Space Grotesk 16 SemiBold, hint line in Inter 10 muted. Settings grouped into rows separated by `divider` lines. Each row uses appropriate widget (toggle / slider / pill-dropdown / pill-multiselect).
- **Status footer**: JetBrains Mono 10. FPS, TPS, accent-colored values. Always present at the bottom.

Search palette (⌘K): floating centered card with text input and live-filtered module results, fuzzy-matched. Each result shows category breadcrumb + module name; enter selects.

### Module Settings (separate window mode)

When a module is opened standalone (right-click flow), it appears as a smaller floating card centered on screen with the same row-of-settings layout. Backdrop-blurred. Drag handle on title bar.

### HUD Editor

Different problem: a canvas of draggable HUD elements over the live game. Aurora treatment:

- Translucent toolbar at top: same title-bar styling, contains buttons (Add, Lock, Reset, Done).
- Selected HUD element gets a glowing accent dotted outline (1.5px, dash 4/4, animated dash offset for marching ants).
- Snap guides draw as 1px accent lines with 8px glow when alignment is detected.
- Right-click on element → contextual menu styled as small Aurora popup (same blur + border + accent).

### Tab Screens (friends, profiles, macros, accounts, baritone, etc.)

Each existing `TabScreen` uses the same shell: title bar + body. Body is composed from existing widget containers, so once the Aurora widget set replaces the Meteor one, these inherit the new look automatically. Tabs themselves render as a horizontal strip below the title bar — Aurora pill style with active tab getting accent underline.

### In-Game HUD

- **Chat prefix** (`[Lime]`): Inter SemiBold 11, accent color, soft 1px glow on the bracket characters only.
- **Info-display panels** (the side text HUD): drawn inside a small Aurora glass card — translucent panel, 8px corner radius, soft drop shadow, accent-tinted left border. Numeric values use JetBrains Mono.
- **Active-module list**: each module name shown as a small pill (rounded rect, accent bg, dark text).

## Implementation Strategy

Build alongside, swap default, delete old — three phases.

### Phase 1 · Foundation (no user-visible change yet)

1. Add `gui/renderer/primitives/` with `RoundedRect`, `Glow`, `Gradient`, `BackdropBlur`. Each is unit-testable via headless render to FBO.
2. Add `gui/renderer/anim/` with `Tween`, `Easing`, `Animated<T>`.
3. Add `FontFamily` to `gui/renderer/text/`. Bundle the three TTFs as resources. Load on mod init.
4. Add `WidgetPalette` interface to existing `GuiTheme` so themes can vend color tokens.

### Phase 2 · Aurora theme (parallel to Meteor)

5. Create `themes/aurora/` package. Implement `AuroraGuiTheme` and each `WAurora*` widget using the new primitives.
6. Implement the four new widgets: `WAuroraSidebar`, `WAuroraBreadcrumb`, `WAuroraStatusBar`, `WAuroraToggle`.
7. Implement the redesigned `ModulesScreen` layout with sidebar. Keep the old layout reachable via theme — the screen asks the theme for "browser layout" (`Sidebar` or `TopTabs`), Aurora returns `Sidebar`.
8. Implement search palette as a new screen `CommandPaletteScreen`.

During phase 2 both themes coexist; the default is still Meteor. You can switch in settings to dogfood Aurora.

### Phase 3 · Switch + cleanup

9. Apply Aurora styling to HUD editor and in-game HUD renderer.
10. Change default theme to Aurora. Verify every tab screen renders correctly.
11. Delete `themes/meteor/` package and all `WMeteor*` files.
12. Remove the old theme settings migration path (or write a one-time importer if we want to migrate user accent color).

## Risks & Open Questions

| Risk | Mitigation |
|------|------------|
| Backdrop blur is GPU-expensive on low-end machines | Auto-disable on < GL 3.3 or if frametime budget exceeded; user setting to force off |
| Custom TTFs bloat the JAR | ~1.5 MB total; acceptable for a utility client. Strip unused glyphs (Latin + symbols only) if needed |
| Existing user color customizations get wiped | Single-time migration: try to read user's old "background" color, use its hue as Aurora accent default; otherwise default to lime |
| Animation cost on slower CPUs | `motionScale=0` and `reducedMotion=true` settings provide escape hatches |
| Some tab screens use hardcoded Meteor widget references | Audit during phase 2; fix references to go through `theme.widget*()` factory methods |
| `Hud.GROUP` and other display names hardcoded "Lime" might leak into setting JSON serialization keys | Verify only display names not config keys changed |

## Success Criteria

- All five surfaces (browser, settings, HUD editor, tab screens, in-game HUD) render in Aurora style with no leftover Meteor visuals.
- 60fps maintained on a mid-range GPU (GTX 1060 class) with backdrop blur enabled, in a populated world.
- Single accent color setting changes every accent-derived value in the UI in real time.
- Reduced-motion setting eliminates all animations.
- `themes/meteor/` package deleted.

## Out of Scope (explicit)

- A theme marketplace, additional packaged themes, or runtime theme swapping by addons.
- Keyboard navigation overhaul (e.g., tab-key focus management) — current behavior preserved.
- Mobile / Bedrock support.
- New mod features dressed up as a UI redesign — this is purely a visual + interaction-polish change.
