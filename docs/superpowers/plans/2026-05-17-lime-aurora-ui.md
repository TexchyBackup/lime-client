# Lime Aurora UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the existing Meteor GUI theme with a new "Aurora" theme — translucent glass panels with real backdrop blur, soft lime glow accents, smooth easing animations, and a curated typography system — across every GUI surface (module browser, settings, HUD editor, tab screens, in-game HUD).

**Architecture:** Build alongside, swap default, delete old. New `themes/aurora/` package implements `AuroraGuiTheme extends GuiTheme` using new render-kit additions in `gui/renderer/primitives/` and `gui/renderer/anim/`. Single user-tunable accent color drives every accent value. Existing `Renderer2D` quad pipeline is extended with SDF-shaded primitives (rounded rect, glow, gradient, backdrop blur).

**Tech Stack:** Java 21, Fabric Loader, Minecraft 1.21 client, custom OpenGL via Meteor's `Renderer2D`/`Shader` classes, JUnit 5 (added in Task 1), STB-based font loading (already in `renderer/text/`).

---

## Conventions for this plan

- **TDD where it fits.** Pure logic (easing math, palette derivation, tween state, layout calculations) gets JUnit tests. Visual rendering (shaders, widgets, screens) cannot be unit-tested meaningfully — it gets a **debug screen** at `gui.screens.debug.AuroraDebugScreen` that draws every primitive/widget, and each visual task's verification step is "open the debug screen via the dev keybind and confirm by eye against the mockup."
- **Open `aesthetic-v2.html`** in the `.superpowers/brainstorm/` folder as the visual ground truth — the Aurora card is the target.
- **Existing Meteor widgets stay functional through Phase 2.** The user can switch themes in `GuiThemes` to A/B between them while building Aurora.
- **Reuse `FontFamily` (already exists in `renderer/text/`).** The spec called my new enum "FontFamily" but it collides with this existing class. Renamed in this plan to **`LimeFonts`** (a holder of three `FontFamily` instances).
- **All new files start with the same copyright header used elsewhere in the repo** (`/* This file is part of the Lime Client distribution... */`). Adjust the existing Meteor header text — it currently says Meteor — to "Lime Client" only on files you newly create. Don't touch headers on existing files in this plan.
- **Commit after every task.** Granular history lets you bisect visual regressions.

---

# Phase 0 · Test infrastructure

### Task 1: Add JUnit 5 to gradle

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Add JUnit dependency**

In `build.gradle.kts` `dependencies { ... }` block, add (use the latest 5.10+ patch):

```kotlin
testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
testRuntimeOnly("org.junit.platform:junit-platform-launcher")
```

And add at the bottom of the file:

```kotlin
tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 2: Refresh gradle**

Run: `./gradlew --refresh-dependencies build -x test 2>&1 | tail -20`
Expected: build succeeds.

- [ ] **Step 3: Verify with a trivial smoke test**

Create `src/test/java/lime/SmokeTest.java`:

```java
package lime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SmokeTest {
    @Test void truthy() { assertTrue(true); }
}
```

Run: `./gradlew test`
Expected: 1 test, PASS.

- [ ] **Step 4: Commit**

```bash
git add build.gradle.kts src/test
git commit -m "test: add JUnit 5 + smoke test"
```

---

# Phase 1 · Render-kit foundation

### Task 2: Easing curves

**Files:**
- Create: `src/main/java/meteordevelopment/meteorclient/gui/renderer/anim/Easing.java`
- Create: `src/test/java/meteordevelopment/meteorclient/gui/renderer/anim/EasingTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package meteordevelopment.meteorclient.gui.renderer.anim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EasingTest {
    private static final double E = 1e-4;

    @Test void linearEndpoints() {
        assertEquals(0.0, Easing.LINEAR.apply(0.0), E);
        assertEquals(1.0, Easing.LINEAR.apply(1.0), E);
        assertEquals(0.5, Easing.LINEAR.apply(0.5), E);
    }

    @Test void easeOutCubicEndpoints() {
        assertEquals(0.0, Easing.OUT_CUBIC.apply(0.0), E);
        assertEquals(1.0, Easing.OUT_CUBIC.apply(1.0), E);
    }

    @Test void easeOutCubicIsMonotonic() {
        double prev = -1;
        for (int i = 0; i <= 100; i++) {
            double v = Easing.OUT_CUBIC.apply(i / 100.0);
            assertTrue(v >= prev, "OUT_CUBIC must be monotonic at t=" + i);
            prev = v;
        }
    }

    @Test void easeOutBackOvershoots() {
        // OUT_BACK should exceed 1 around t=0.7-0.8
        boolean overshoots = false;
        for (int i = 60; i <= 90; i++) {
            if (Easing.OUT_BACK.apply(i / 100.0) > 1.0) overshoots = true;
        }
        assertTrue(overshoots, "OUT_BACK should overshoot above 1.0");
        assertEquals(1.0, Easing.OUT_BACK.apply(1.0), E);
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL**

Run: `./gradlew test --tests EasingTest`
Expected: compile error or all 4 tests FAIL (class not yet defined).

- [ ] **Step 3: Implement `Easing`**

```java
package meteordevelopment.meteorclient.gui.renderer.anim;

public interface Easing {
    double apply(double t);

    Easing LINEAR     = t -> t;
    Easing OUT_CUBIC  = t -> 1 - Math.pow(1 - t, 3);
    Easing IN_CUBIC   = t -> t * t * t;
    Easing IN_OUT_CUBIC = t -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    Easing OUT_BACK   = t -> { double c1 = 1.70158, c3 = c1 + 1; return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2); };
    Easing OUT_SINE   = t -> Math.sin((t * Math.PI) / 2);
    Easing IN_OUT_SINE = t -> -(Math.cos(Math.PI * t) - 1) / 2;
}
```

- [ ] **Step 4: Run tests — expect PASS**

Run: `./gradlew test --tests EasingTest`
Expected: 4 PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/meteordevelopment/meteorclient/gui/renderer/anim/Easing.java src/test/java/meteordevelopment/meteorclient/gui/renderer/anim/EasingTest.java
git commit -m "feat(gui): add Easing curves with tests"
```

---

### Task 3: Animated value wrapper

**Files:**
- Create: `src/main/java/meteordevelopment/meteorclient/gui/renderer/anim/Animated.java`
- Create: `src/test/java/meteordevelopment/meteorclient/gui/renderer/anim/AnimatedTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package meteordevelopment.meteorclient.gui.renderer.anim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimatedTest {
    @Test void startsAtInitialValue() {
        Animated a = new Animated(5.0, 0.180, Easing.OUT_CUBIC);
        assertEquals(5.0, a.get(), 1e-6);
    }

    @Test void reachesTargetAfterFullDuration() {
        Animated a = new Animated(0.0, 0.180, Easing.LINEAR);
        a.set(1.0);
        a.update(0.180);
        assertEquals(1.0, a.get(), 1e-4);
    }

    @Test void linearAtHalfDuration() {
        Animated a = new Animated(0.0, 0.180, Easing.LINEAR);
        a.set(1.0);
        a.update(0.090);
        assertEquals(0.5, a.get(), 1e-4);
    }

    @Test void retargetMidFlightContinues() {
        Animated a = new Animated(0.0, 0.200, Easing.LINEAR);
        a.set(1.0);
        a.update(0.100);   // halfway
        a.set(0.0);        // retarget back
        a.update(0.200);
        assertEquals(0.0, a.get(), 1e-4);
    }

    @Test void instantWhenDurationZero() {
        Animated a = new Animated(0.0, 0.0, Easing.LINEAR);
        a.set(7.0);
        a.update(0.001);
        assertEquals(7.0, a.get(), 1e-6);
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

Run: `./gradlew test --tests AnimatedTest`
Expected: compile error.

- [ ] **Step 3: Implement `Animated`**

```java
package meteordevelopment.meteorclient.gui.renderer.anim;

public class Animated {
    private double from;
    private double to;
    private double elapsed;
    private double duration;
    private Easing easing;

    public Animated(double initial, double duration, Easing easing) {
        this.from = initial;
        this.to = initial;
        this.elapsed = duration;
        this.duration = duration;
        this.easing = easing;
    }

    public void set(double target) {
        if (Math.abs(target - to) < 1e-9) return;
        this.from = get();
        this.to = target;
        this.elapsed = 0;
    }

    public void setInstant(double value) {
        this.from = value;
        this.to = value;
        this.elapsed = duration;
    }

    public void update(double dt) {
        if (elapsed >= duration) return;
        elapsed = Math.min(duration, elapsed + dt);
    }

    public double get() {
        if (duration <= 0 || elapsed >= duration) return to;
        double t = easing.apply(elapsed / duration);
        return from + (to - from) * t;
    }

    public void setDuration(double d) { this.duration = d; }
    public void setEasing(Easing e) { this.easing = e; }
    public double target() { return to; }
}
```

- [ ] **Step 4: Run — expect PASS**

Run: `./gradlew test --tests AnimatedTest`
Expected: 5 PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/meteordevelopment/meteorclient/gui/renderer/anim/Animated.java src/test/java/meteordevelopment/meteorclient/gui/renderer/anim/AnimatedTest.java
git commit -m "feat(gui): add Animated<double> value wrapper with tests"
```

---

### Task 4: Aurora palette and color derivation

**Files:**
- Create: `src/main/java/meteordevelopment/meteorclient/gui/themes/aurora/AuroraPalette.java`
- Create: `src/test/java/meteordevelopment/meteorclient/gui/themes/aurora/AuroraPaletteTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package meteordevelopment.meteorclient.gui.themes.aurora;

import meteordevelopment.meteorclient.utils.render.color.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuroraPaletteTest {
    @Test void accentRoundtrips() {
        Color lime = new Color(136, 238, 85);
        AuroraPalette p = new AuroraPalette(lime);
        assertEquals(lime.r, p.accent().r);
        assertEquals(lime.g, p.accent().g);
        assertEquals(lime.b, p.accent().b);
    }

    @Test void lightenedAccentIsLighter() {
        AuroraPalette p = new AuroraPalette(new Color(136, 238, 85));
        Color lit = p.accentLight();
        assertTrue(lit.r >= 136 && lit.g >= 200 && lit.b >= 85);
    }

    @Test void glowAccentIsTranslucent() {
        AuroraPalette p = new AuroraPalette(new Color(136, 238, 85));
        assertTrue(p.accentGlow().a < 255 && p.accentGlow().a > 0);
    }

    @Test void panelTokensAreFixed() {
        AuroraPalette p1 = new AuroraPalette(new Color(255, 0, 0));
        AuroraPalette p2 = new AuroraPalette(new Color(0, 0, 255));
        assertEquals(p1.panelBase().r, p2.panelBase().r);  // panel base does NOT depend on accent
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

Run: `./gradlew test --tests AuroraPaletteTest`

- [ ] **Step 3: Implement `AuroraPalette`**

```java
package meteordevelopment.meteorclient.gui.themes.aurora;

import meteordevelopment.meteorclient.utils.render.color.Color;

public final class AuroraPalette {
    private final Color accent;
    private final Color accentLight;
    private final Color accentGlow;

    public static final Color BG_TOP        = new Color(5, 10, 8);
    public static final Color BG_BOTTOM     = new Color(10, 15, 10);
    public static final Color PANEL_BASE    = new Color(20, 30, 22, 217); // 0.85 alpha
    public static final Color PANEL_TOP     = new Color(255, 255, 255, 20);  // 0.08 alpha
    public static final Color PANEL_SHADOW  = new Color(0, 0, 0, 153);
    public static final Color TEXT_PRIMARY  = new Color(232, 245, 224);
    public static final Color TEXT_SECONDARY = new Color(138, 154, 138);
    public static final Color TEXT_MUTED    = new Color(90, 110, 90);
    public static final Color DIVIDER       = new Color(255, 255, 255, 10);
    public static final Color DIVIDER_STRONG = new Color(255, 255, 255, 20);
    public static final Color HOVER         = new Color(255, 255, 255, 8);
    public static final Color SLIDER_TRACK  = new Color(255, 255, 255, 15);

    public AuroraPalette(Color accent) {
        this.accent = new Color(accent.r, accent.g, accent.b, 255);
        this.accentLight = lighten(this.accent, 0.18);
        this.accentGlow  = new Color(accent.r, accent.g, accent.b, 128);
    }

    public Color accent()       { return accent; }
    public Color accentLight()  { return accentLight; }
    public Color accentGlow()   { return accentGlow; }
    public Color panelBase()    { return PANEL_BASE; }
    public Color panelTop()     { return PANEL_TOP; }
    public Color panelBorder()  { return new Color(accent.r, accent.g, accent.b, 38); }
    public Color panelGlow()    { return new Color(accent.r, accent.g, accent.b, 20); }
    public Color textPrimary()  { return TEXT_PRIMARY; }
    public Color textSecondary(){ return TEXT_SECONDARY; }
    public Color textMuted()    { return TEXT_MUTED; }
    public Color textAccent()   { return accentLight; }
    public Color divider()      { return DIVIDER; }
    public Color hover()        { return HOVER; }
    public Color sliderTrack()  { return SLIDER_TRACK; }

    private static Color lighten(Color c, double amount) {
        int r = (int) Math.min(255, c.r + (255 - c.r) * amount);
        int g = (int) Math.min(255, c.g + (255 - c.g) * amount);
        int b = (int) Math.min(255, c.b + (255 - c.b) * amount);
        return new Color(r, g, b, c.a);
    }
}
```

> Note: verify `Color` constructor signatures in `utils/render/color/Color.java` and adjust if needed (this codebase has `Color(int, int, int)` and `Color(int, int, int, int)`).

- [ ] **Step 4: Run — expect PASS**

Run: `./gradlew test --tests AuroraPaletteTest`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/meteordevelopment/meteorclient/gui/themes/aurora/AuroraPalette.java src/test/java/meteordevelopment/meteorclient/gui/themes/aurora/AuroraPaletteTest.java
git commit -m "feat(aurora): add color palette + accent derivation"
```

---

### Task 5: Bundle TTF fonts as assets and load them

**Files:**
- Create: `src/main/resources/assets/lime-client/fonts/Inter-Regular.ttf` (download from rsms.me/inter — Inter v4, Regular weight)
- Create: `src/main/resources/assets/lime-client/fonts/Inter-Medium.ttf`
- Create: `src/main/resources/assets/lime-client/fonts/Inter-SemiBold.ttf`
- Create: `src/main/resources/assets/lime-client/fonts/SpaceGrotesk-SemiBold.ttf` (Google Fonts)
- Create: `src/main/resources/assets/lime-client/fonts/SpaceGrotesk-Bold.ttf`
- Create: `src/main/resources/assets/lime-client/fonts/JetBrainsMono-Regular.ttf` (jetbrains.com/lp/mono)
- Create: `src/main/resources/assets/lime-client/fonts/JetBrainsMono-Medium.ttf`
- Create: `src/main/java/meteordevelopment/meteorclient/gui/themes/aurora/LimeFonts.java`

- [ ] **Step 1: Download and place TTFs**

Use the URLs above to download each TTF file. Confirm file sizes (each should be 100-500KB). Strip to Latin + symbols subset later if JAR size matters.

- [ ] **Step 2: Implement `LimeFonts` loader**

Inspect `renderer/text/BuiltinFontFace.java` and `FontFamily.java` for how fonts are constructed from resources. Then:

```java
package meteordevelopment.meteorclient.gui.themes.aurora;

import meteordevelopment.meteorclient.renderer.text.BuiltinFontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;

public final class LimeFonts {
    public static FontFamily UI;       // Inter
    public static FontFamily DISPLAY;  // Space Grotesk
    public static FontFamily MONO;     // JetBrains Mono

    private LimeFonts() {}

    public static void load() {
        UI = new FontFamily("Inter",
            new BuiltinFontFace(new FontInfo("Inter", FontInfo.Type.REGULAR),    "assets/lime-client/fonts/Inter-Regular.ttf"),
            new BuiltinFontFace(new FontInfo("Inter", FontInfo.Type.MEDIUM),     "assets/lime-client/fonts/Inter-Medium.ttf"),
            new BuiltinFontFace(new FontInfo("Inter", FontInfo.Type.SEMI_BOLD),  "assets/lime-client/fonts/Inter-SemiBold.ttf")
        );

        DISPLAY = new FontFamily("Space Grotesk",
            new BuiltinFontFace(new FontInfo("Space Grotesk", FontInfo.Type.SEMI_BOLD), "assets/lime-client/fonts/SpaceGrotesk-SemiBold.ttf"),
            new BuiltinFontFace(new FontInfo("Space Grotesk", FontInfo.Type.BOLD),      "assets/lime-client/fonts/SpaceGrotesk-Bold.ttf")
        );

        MONO = new FontFamily("JetBrains Mono",
            new BuiltinFontFace(new FontInfo("JetBrains Mono", FontInfo.Type.REGULAR), "assets/lime-client/fonts/JetBrainsMono-Regular.ttf"),
            new BuiltinFontFace(new FontInfo("JetBrains Mono", FontInfo.Type.MEDIUM),  "assets/lime-client/fonts/JetBrainsMono-Medium.ttf")
        );
    }
}
```

> Inspect `FontInfo.Type` for actual enum values. If `SEMI_BOLD` isn't there, add it. Inspect `BuiltinFontFace` ctor for actual signature — adjust constructor args to match.

- [ ] **Step 3: Hook into MeteorClient init**

In `MeteorClient.java` `onInitializeClient()`, after the existing init calls, add:

```java
meteordevelopment.meteorclient.gui.themes.aurora.LimeFonts.load();
```

- [ ] **Step 4: Verify in-game**

Launch the mod (`./gradlew runClient`), open any GUI, confirm no crash on load. Logs should not show font load errors.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/assets/lime-client/fonts src/main/java/meteordevelopment/meteorclient/gui/themes/aurora/LimeFonts.java src/main/java/meteordevelopment/meteorclient/MeteorClient.java
git commit -m "feat(aurora): bundle Inter, Space Grotesk, JetBrains Mono TTFs"
```

---

### Task 6: Rounded rectangle primitive (SDF shader)

**Files:**
- Create: `src/main/resources/assets/meteor-client/shaders/aurora_rounded_rect.vert`
- Create: `src/main/resources/assets/meteor-client/shaders/aurora_rounded_rect.frag`
- Create: `src/main/java/meteordevelopment/meteorclient/gui/renderer/primitives/RoundedRect.java`
- Modify: `src/main/java/meteordevelopment/meteorclient/gui/renderer/GuiRenderer.java` — add `roundedRect(...)` method

- [ ] **Step 1: Write the fragment shader**

```glsl
// aurora_rounded_rect.frag
#version 150
in  vec2 vUv;       // 0..1 within rect
in  vec4 vColor;
flat in vec2 vSize;
flat in float vRadius;
flat in vec4 vBorder;     // rgba — if a == 0, no border
flat in float vBorderWidth;
out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 p = (vUv - 0.5) * vSize;
    float d = sdRoundBox(p, vSize * 0.5, vRadius);
    float aa = 1.0;
    float fillAlpha = smoothstep(aa, -aa, d);
    vec4 col = vColor * fillAlpha;
    if (vBorder.a > 0.0) {
        float borderAlpha = smoothstep(aa, -aa, abs(d) - vBorderWidth * 0.5);
        col = mix(col, vBorder, borderAlpha * vBorder.a);
    }
    fragColor = col;
}
```

Vertex shader writes through UV, size, radius, border uniforms (copy pattern from existing shaders in `assets/meteor-client/shaders/`).

- [ ] **Step 2: Implement `RoundedRect` Java wrapper**

```java
package meteordevelopment.meteorclient.gui.renderer.primitives;

import meteordevelopment.meteorclient.renderer.Shader;
import meteordevelopment.meteorclient.utils.render.color.Color;

public final class RoundedRect {
    private static Shader SHADER;

    public static void init() {
        SHADER = new Shader("aurora_rounded_rect.vert", "aurora_rounded_rect.frag");
    }

    public static void draw(double x, double y, double w, double h, double radius, Color fill) {
        draw(x, y, w, h, radius, fill, null, 0);
    }

    public static void draw(double x, double y, double w, double h, double radius,
                            Color fill, Color border, double borderWidth) {
        // Bind shader, set uniforms (size, radius, fill, border, borderWidth),
        // emit single quad covering (x,y)..(x+w,y+h) with UVs 0..1
        // — implementation uses Renderer2D's quad emission with a custom shader.
        // See pattern in existing `gui/renderer/operations/QuadOperation.java`.
        // (Engineer: read that file for emission pattern, mirror it here.)
    }
}
```

> Engineer: read `gui/renderer/operations/` and existing shader-driven render code (search for `new Shader(`) for the emission pattern. The Java side here is mostly plumbing; the math is in the shader.

- [ ] **Step 3: Add to `GuiRenderer`**

In `GuiRenderer.java`, add a method:

```java
public void roundedRect(double x, double y, double w, double h, double radius, Color fill) {
    RoundedRect.draw(x, y, w, h, radius, fill);
}

public void roundedRectStroke(double x, double y, double w, double h, double radius,
                              Color fill, Color border, double borderWidth) {
    RoundedRect.draw(x, y, w, h, radius, fill, border, borderWidth);
}
```

- [ ] **Step 4: Verify on the debug screen (created in Task 11)**

Defer visual verification to Task 11.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/assets/meteor-client/shaders/aurora_rounded_rect.* src/main/java/meteordevelopment/meteorclient/gui/renderer/primitives/RoundedRect.java src/main/java/meteordevelopment/meteorclient/gui/renderer/GuiRenderer.java
git commit -m "feat(aurora): SDF-shaded rounded rectangle primitive"
```

---

### Task 7: Glow primitive

**Files:**
- Create: `src/main/resources/assets/meteor-client/shaders/aurora_glow.frag`
- Create: `src/main/java/meteordevelopment/meteorclient/gui/renderer/primitives/Glow.java`
- Modify: `GuiRenderer.java`

- [ ] **Step 1: Write the glow fragment shader**

```glsl
#version 150
in  vec2 vUv;
flat in vec2 vSize;
flat in float vRadius;
flat in float vGlowRadius;
flat in vec4 vColor;
out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 p = (vUv - 0.5) * (vSize + vGlowRadius * 2.0);
    float d = sdRoundBox(p, vSize * 0.5, vRadius);
    // Gaussian-like falloff outside the rect
    float a = exp(-d * d / (vGlowRadius * vGlowRadius * 0.4));
    a = clamp(a, 0.0, 1.0);
    fragColor = vec4(vColor.rgb, vColor.a * a);
}
```

- [ ] **Step 2: Implement `Glow.java`**

```java
package meteordevelopment.meteorclient.gui.renderer.primitives;

import meteordevelopment.meteorclient.renderer.Shader;
import meteordevelopment.meteorclient.utils.render.color.Color;

public final class Glow {
    private static Shader SHADER;
    public static void init() { SHADER = new Shader("aurora_rounded_rect.vert", "aurora_glow.frag"); }

    /** Renders a soft halo around (x,y,w,h) extending glowRadius pixels outward. */
    public static void draw(double x, double y, double w, double h, double radius,
                            double glowRadius, Color color) {
        // Emit quad expanded by glowRadius on all sides
        // Pattern: same as RoundedRect but quad is bigger and glow shader runs
    }
}
```

- [ ] **Step 3: Add `GuiRenderer.glow(...)` method**

```java
public void glow(double x, double y, double w, double h, double radius, double glowRadius, Color color) {
    Glow.draw(x, y, w, h, radius, glowRadius, color);
}
```

- [ ] **Step 4: Defer visual verification to Task 11.**

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/assets/meteor-client/shaders/aurora_glow.frag src/main/java/meteordevelopment/meteorclient/gui/renderer/primitives/Glow.java src/main/java/meteordevelopment/meteorclient/gui/renderer/GuiRenderer.java
git commit -m "feat(aurora): soft glow halo primitive"
```

---

### Task 8: Linear and radial gradient primitive

**Files:**
- Create: `src/main/resources/assets/meteor-client/shaders/aurora_gradient.frag`
- Create: `src/main/java/meteordevelopment/meteorclient/gui/renderer/primitives/Gradient.java`
- Modify: `GuiRenderer.java`

- [ ] **Step 1: Shader**

```glsl
#version 150
in vec2 vUv;
flat in vec4 vColorA;
flat in vec4 vColorB;
flat in vec2 vDir;      // unit direction for linear; (0,0) = radial
out vec4 fragColor;

void main() {
    float t;
    if (length(vDir) < 0.001) {
        t = clamp(length(vUv - 0.5) * 2.0, 0.0, 1.0);   // radial
    } else {
        t = clamp(dot(vUv - 0.5, vDir) + 0.5, 0.0, 1.0); // linear
    }
    fragColor = mix(vColorA, vColorB, t);
}
```

- [ ] **Step 2: `Gradient.java`**

```java
package meteordevelopment.meteorclient.gui.renderer.primitives;

import meteordevelopment.meteorclient.utils.render.color.Color;

public final class Gradient {
    public static void linear(double x, double y, double w, double h,
                              Color a, Color b, double angleDegrees) { /* ... */ }

    public static void radial(double x, double y, double w, double h,
                              Color center, Color edge) { /* ... */ }
}
```

- [ ] **Step 3: Add `GuiRenderer.gradient(...)` overloads.**

- [ ] **Step 4: Defer visual verification.**

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(aurora): linear + radial gradient primitive"
```

---

### Task 9: Backdrop blur pass

**Files:**
- Create: `src/main/resources/assets/meteor-client/shaders/aurora_blur_h.frag` (horizontal separable gaussian)
- Create: `src/main/resources/assets/meteor-client/shaders/aurora_blur_v.frag` (vertical)
- Create: `src/main/java/meteordevelopment/meteorclient/gui/renderer/primitives/BackdropBlur.java`
- Modify: `GuiRenderer.java` — call `BackdropBlur.captureAndBlur()` in `begin(...)`

- [ ] **Step 1: Check existing `Blur` post-process module shader**

Read `src/main/java/meteordevelopment/meteorclient/systems/modules/render/Blur.java` and its shader. Reuse the existing gaussian shader code — copy and adapt for the GUI capture flow.

- [ ] **Step 2: Implement two-pass separable blur**

```java
package meteordevelopment.meteorclient.gui.renderer.primitives;

import meteordevelopment.meteorclient.renderer.Framebuffer;
import meteordevelopment.meteorclient.renderer.Shader;

public final class BackdropBlur {
    private static Framebuffer fboA, fboB;
    private static Shader BLUR_H, BLUR_V;
    private static boolean enabled = true;
    private static int downscale = 2;       // 1 = full, 2 = half, 4 = quarter
    private static float radius = 16f;

    public static void init() {
        BLUR_H = new Shader("fullscreen.vert", "aurora_blur_h.frag");
        BLUR_V = new Shader("fullscreen.vert", "aurora_blur_v.frag");
    }

    public static void setEnabled(boolean v) { enabled = v; }
    public static boolean isEnabled() { return enabled; }

    /** Captures the framebuffer, blurs, leaves result available as a sampled texture. */
    public static void captureAndBlur() {
        if (!enabled) return;
        // 1. Read main framebuffer into fboA at downscale resolution
        // 2. Run BLUR_H from fboA → fboB
        // 3. Run BLUR_V from fboB → fboA  (final blurred texture)
        // See `systems/modules/render/Blur.java` for the existing capture+blur dance
    }

    /** Samples the blurred backdrop into a rounded-rect region (used by glass widgets). */
    public static void sampleInto(double x, double y, double w, double h, double radius, double tint) {
        // Bind fboA texture, draw quad with SDF rounded mask, dim by `tint`
    }
}
```

- [ ] **Step 3: Wire into `GuiRenderer.begin(...)`**

```java
public void begin(GuiGraphicsExtractor graphics) {
    this.graphics = graphics;
    this.graphics.nextStratum();

    BackdropBlur.captureAndBlur();   // ← new

    var matrices = graphics.pose();
    // ... existing code
}
```

- [ ] **Step 4: Verify visually**

Open any module GUI in-game. Without backdrop blur calls in widgets yet, the screen should look unchanged — verify no crashes, no frame drops > 2ms over baseline (use F3 debug overlay).

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(aurora): gaussian backdrop blur pass on GUI open"
```

---

### Task 10: Theme palette interface on `GuiTheme`

**Files:**
- Modify: `src/main/java/meteordevelopment/meteorclient/gui/GuiTheme.java`

- [ ] **Step 1: Add abstract palette accessor (default null for back-compat)**

Add to `GuiTheme`:

```java
public meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette palette() {
    return null;  // non-Aurora themes return null
}
```

Subclasses that have a palette override this. This avoids forcing Meteor to migrate.

- [ ] **Step 2: Commit**

```bash
git commit -am "refactor(gui): add palette() accessor on GuiTheme base"
```

---

### Task 11: Aurora debug screen for visual verification

**Files:**
- Create: `src/main/java/meteordevelopment/meteorclient/gui/screens/debug/AuroraDebugScreen.java`
- Modify: `MeteorClient.java` — bind a dev-only keybind (F8) that opens this screen

- [ ] **Step 1: Create the screen**

It should render, on a single Aurora-styled background, samples of every primitive built so far:
1. A row of rounded rects at radii 4, 10, 14, 22, with and without borders
2. A row of glows (small, medium, large radius)
3. Linear gradients (horizontal, vertical, diagonal) and a radial gradient
4. Backdrop-blurred glass card sitting on top of the game world
5. Three text samples in Inter / Space Grotesk / JetBrains Mono at sizes 11, 16, 22

```java
package meteordevelopment.meteorclient.gui.screens.debug;

// imports...

public class AuroraDebugScreen extends Screen {
    public AuroraDebugScreen() { super(Component.literal("Aurora Debug")); }

    @Override public void render(GuiGraphics g, int mx, int my, float dt) {
        // call GuiRenderer methods to draw the catalog
    }
}
```

- [ ] **Step 2: Bind F8 to open this screen in dev only**

In `MeteorClient.onInitializeClient()` add a keybind registration (only when `FabricLoader.getInstance().isDevelopmentEnvironment()` is true) that opens `AuroraDebugScreen`.

- [ ] **Step 3: Run client, press F8, eyeball every primitive**

Run: `./gradlew runClient`. Press F8 in main menu. Compare against `aesthetic-v2.html` Aurora mockup.

If anything is off (corners pixelated, glow too sharp, gradient banding), iterate on the relevant primitive's shader and retest before moving on.

- [ ] **Step 4: Commit**

```bash
git commit -am "feat(aurora): debug screen for primitive visual verification (F8)"
```

---

# Phase 2 · Aurora theme

### Task 12: `AuroraGuiTheme` skeleton

**Files:**
- Create: `src/main/java/meteordevelopment/meteorclient/gui/themes/aurora/AuroraGuiTheme.java`
- Create: `src/main/java/meteordevelopment/meteorclient/gui/themes/aurora/AuroraWidget.java`
- Modify: `src/main/java/meteordevelopment/meteorclient/gui/GuiThemes.java` — register the new theme

- [ ] **Step 1: `AuroraWidget` marker interface**

```java
package meteordevelopment.meteorclient.gui.themes.aurora;

import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;

public interface AuroraWidget {
    default AuroraGuiTheme theme() {
        return (AuroraGuiTheme) ((meteordevelopment.meteorclient.gui.widgets.WWidget)(this)).getTheme();
    }
}
```

> If `WWidget.getTheme()` doesn't exist, route via `GuiRenderer.theme` in `onRender`. Adapt to match `MeteorWidget`'s pattern.

- [ ] **Step 2: `AuroraGuiTheme` class with the 4 user settings**

Use `MeteorGuiTheme.java` as a structural template — keep the same overrides, but each factory method initially throws `UnsupportedOperationException("not yet implemented")`. Add at the top:

```java
package meteordevelopment.meteorclient.gui.themes.aurora;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class AuroraGuiTheme extends GuiTheme {
    public final Setting<SettingColor> accent;
    public final Setting<Boolean>      backdropBlur;
    public final Setting<Double>       motionScale;
    public final Setting<Boolean>      reducedMotion;

    private AuroraPalette cachedPalette;
    private int cachedAccentHash;

    public AuroraGuiTheme() {
        super("Lime");

        SettingGroup sgGeneral = settings.getDefaultGroup();

        accent = sgGeneral.add(new ColorSetting.Builder()
            .name("accent")
            .description("Single accent color that drives every accent value in the UI.")
            .defaultValue(new SettingColor(136, 238, 85))
            .build()
        );

        backdropBlur = sgGeneral.add(new BoolSetting.Builder()
            .name("backdrop-blur")
            .description("Real gaussian blur of the game scene behind GUI panels.")
            .defaultValue(true)
            .onChanged(v -> BackdropBlur.setEnabled(v))
            .build()
        );

        motionScale = sgGeneral.add(new DoubleSetting.Builder()
            .name("motion-scale")
            .description("Speed multiplier for UI animations. 0 = instant.")
            .min(0).max(1.5).sliderMax(1.5)
            .defaultValue(1.0)
            .build()
        );

        reducedMotion = sgGeneral.add(new BoolSetting.Builder()
            .name("reduced-motion")
            .description("Disable all UI animations.")
            .defaultValue(false)
            .build()
        );
    }

    public AuroraPalette palette() {
        int hash = accent.get().hashCode();
        if (cachedPalette == null || hash != cachedAccentHash) {
            cachedPalette = new AuroraPalette(accent.get());
            cachedAccentHash = hash;
        }
        return cachedPalette;
    }

    public double animDuration(double base) {
        if (reducedMotion.get()) return 0;
        return base * motionScale.get();
    }

    // All abstract widget factory methods from GuiTheme go below — each currently:
    //   throws new UnsupportedOperationException("not yet implemented");
    // They get filled in by Tasks 13–22.
}
```

- [ ] **Step 3: Register in `GuiThemes`**

In `GuiThemes.java`, where the existing Meteor theme is registered (likely an `init()` or static block), register `AuroraGuiTheme`. Keep Meteor as default for now.

- [ ] **Step 4: Verify**

Run the client, open the GUI theme selector, confirm "Lime" appears alongside "Meteor". Selecting Lime should not crash (we'll get `UnsupportedOperationException` if you actually open any widget — fine, that's expected mid-build).

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(aurora): AuroraGuiTheme skeleton + 4 user settings"
```

---

### Task 13: `WAuroraWindow` and `WAuroraView`

**Files:**
- Create: `src/main/java/meteordevelopment/meteorclient/gui/themes/aurora/widgets/WAuroraWindow.java`
- Create: `src/main/java/meteordevelopment/meteorclient/gui/themes/aurora/widgets/WAuroraView.java`
- Modify: `AuroraGuiTheme.java` — implement `window(...)` and `view(...)`

- [ ] **Step 1: Implement `WAuroraWindow`**

Use `WMeteorWindow` as the structural template, then replace the body of `onRender` with Aurora-style draws:

```java
@Override
protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    if (!(expanded || animProgress > 0)) return;
    AuroraPalette p = ((AuroraGuiTheme) theme()).palette();
    double bodyY = y + header.height;
    double bodyH = height - header.height;

    // 1. Soft outer glow (ambient)
    renderer.glow(x, bodyY, width, bodyH, 14, 24, p.panelGlow());
    // 2. Backdrop-blurred glass body
    BackdropBlur.sampleInto(x, bodyY, width, bodyH, 14, 0.5);
    // 3. Panel tint
    renderer.roundedRectStroke(x, bodyY, width, bodyH, 14, p.panelBase(), p.panelBorder(), 1);
    // 4. Top inner highlight (1px gradient line)
    renderer.gradient(x + 12, bodyY, width - 24, 1,
        new Color(0,0,0,0), p.panelTop(), new Color(0,0,0,0));  // fade-in-fade-out
}
```

- [ ] **Step 2: Implement `WAuroraView`** (simple — usually no rendering, just a clipping container — mirror `WMeteorView` pattern).

- [ ] **Step 3: Implement `AuroraGuiTheme.window(...)` and `AuroraGuiTheme.view(...)` factory methods.**

```java
@Override public WWindow window(WWidget icon, String title) { return new WAuroraWindow(icon, title); }
@Override public WView view() { return new WAuroraView(); }
```

- [ ] **Step 4: Visual verify on debug screen**

Add a sample window with body content to `AuroraDebugScreen`. Press F8 in-game, compare to mockup.

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(aurora): window + view widgets with glass body, glow, border"
```

---

### Task 14: `WAuroraSection` (header), `WAuroraTopBar`, `WAuroraTitleBar` (within window)

**Files:**
- Create: `WAuroraSection.java`, `WAuroraTopBar.java`, and the inner header class on `WAuroraWindow`
- Modify: `AuroraGuiTheme.java`

- [ ] **Step 1: Implement title bar (inner class on `WAuroraWindow`)**

Title bar has: accent dot (4-5px), wordmark text in Space Grotesk SemiBold, breadcrumb in Inter Regular muted, ⌘K hint pill on the right. Renders as:

```java
@Override
protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    AuroraPalette p = ((AuroraGuiTheme) theme()).palette();
    // 1. background — slightly darker than body
    BackdropBlur.sampleInto(x, y, width, height, 14, 0.6);
    renderer.roundedRect(x, y, width, height, 14, p.panelBase());
    // 2. accent dot at x+12, y+height/2 — radius 3
    renderer.glow(x + 9, y + height/2 - 3.5, 7, 7, 3.5, 8, p.accentGlow());
    renderer.roundedRect(x + 9, y + height/2 - 3.5, 7, 7, 3.5, p.accent());
    // 3. title text "Lime" — Space Grotesk SB 13
    // 4. breadcrumb — Inter 11 muted, after title
    // 5. ⌘K pill right-aligned
    // 6. bottom divider (1px) — divider color
    renderer.quad(x + 16, y + height - 1, width - 32, 1, p.divider());
}
```

Use `LimeFonts.DISPLAY.get(SEMI_BOLD)` and `LimeFonts.UI.get(REGULAR)` to render text via the existing `TextRenderer` integration.

- [ ] **Step 2: Implement `WAuroraTopBar`** as the body version (no title bar, just an accent strip + nav buttons).

- [ ] **Step 3: Implement `WAuroraSection`** — collapsible section. Header row with chevron triangle and label, body below. Chevron rotates animated when toggled.

- [ ] **Step 4: Verify on debug screen** (add a window with title bar + sections).

- [ ] **Step 5: Commit**

---

### Task 15: `WAuroraToggle` — animated toggle switch

**Files:**
- Create: `widgets/pressable/WAuroraToggle.java`
- Modify: `AuroraGuiTheme.java` — wire as the rendering for `checkbox(boolean)`

- [ ] **Step 1: Test the toggle animation state machine**

```java
// WAuroraToggleTest.java
@Test void thumbAnimatesToTargetOnToggle() {
    WAuroraToggle t = new WAuroraToggle(false);
    assertEquals(0.0, t.thumbPosition(), 1e-4);
    t.setChecked(true);
    for (int i = 0; i < 30; i++) t.tick(0.010);   // 300ms total
    assertEquals(1.0, t.thumbPosition(), 1e-3);
}
```

- [ ] **Step 2: Implement**

The thumb's x-position is an `Animated` value tweened from 0 (off) to 1 (on). Render order:
1. Track: rounded rect (h*0.5 radius), color interpolated from `panelTop`→`accent` based on thumbPosition.
2. Glow under track when on, scaled by thumbPosition.
3. Thumb circle: white rounded rect, x = trackX + thumbPosition * (trackW - thumbH) - inset.

- [ ] **Step 3: Verify visually**

Add toggle samples (off, on, mid-anim) to debug screen.

- [ ] **Step 4: Commit**

---

### Task 16: `WAuroraSlider`

**Files:**
- Create: `widgets/input/WAuroraSlider.java`

Mirror `WMeteorSlider` structure. Render:
1. Track: `sliderTrack` rounded rect, 6px tall, full-width.
2. Fill: gradient `accent → accentLight`, rounded rect from x to thumb position.
3. Glow under fill (radius 8, accentGlow).
4. Thumb: white rounded circle (12px), 4px drop shadow.
5. Value label to the right: JetBrains Mono 11, accentLight color.

Interpolate the value display smoothly (Animated wrapping the displayed value, but actual setting writes are still per-frame).

- [ ] **Step 1-5:** Standard cycle. Commit `feat(aurora): slider widget with gradient fill + glow`.

---

### Task 17: `WAuroraDropdown` (pill style)

**Files:**
- Create: `widgets/input/WAuroraDropdown.java`

Pill rendering when closed: rounded rect 10px radius, `accent 0.1` bg, `accentLight` text, chevron icon ▾ on right. Opens into a floating menu (also a glass card) with hover rows and accent active row.

Menu open/close animates (height + opacity), uses an `Animated` for height.

- [ ] **Steps:** Standard. Commit.

---

### Task 18: `WAuroraTextBox`

**Files:**
- Create: `widgets/input/WAuroraTextBox.java`

Rounded rect 8px radius, panelBase bg with `divider` border. On focus: border becomes `accent`, soft glow appears. Cursor blinks (Animated opacity, 1.0s loop).

- [ ] **Steps:** Standard. Commit.

---

### Task 19: `WAuroraButton` and the small pressables (Plus, Minus, Triangle, Favorite, ConfirmedButton, ConfirmedMinus, Checkbox)

**Files:**
- Create one file per widget in `widgets/pressable/`

- [ ] **Step 1: WAuroraButton** — rounded pill, accent bg on primary, panelBase bg on secondary, hover glow.

- [ ] **Step 2: WAuroraPlus / WAuroraMinus** — 14px square rounded, icon glyph (use existing GuiRenderer icon textures), hover glow.

- [ ] **Step 3: WAuroraTriangle** — chevron, rotates on expand/collapse (Animated rotation, 180ms OUT_CUBIC).

- [ ] **Step 4: WAuroraFavorite** — star icon, when active fills with accent + glow.

- [ ] **Step 5: WAuroraConfirmedButton / WAuroraConfirmedMinus** — two-click confirm; first click changes label/color to a warning state (orange tone, derived but not from accent — use a hardcoded `WARN = (255, 140, 60)`).

- [ ] **Step 6: WAuroraCheckbox** — 14px square rounded rect; checked state shows a small accent fill + checkmark glyph.

- [ ] **Step 7:** Verify each on debug screen. Commit per widget or batch — your call. Frequent commits preferred.

---

### Task 20: `WAuroraLabel`, `WAuroraSeparator`, `WAuroraQuad`, `WAuroraTooltip`, `WAuroraMultiLabel`, `WAuroraHorizontalSeparator`, `WAuroraVerticalSeparator`

**Files:**
- Create one file per widget

- [ ] **Step 1: WAuroraLabel** — text rendering with optional title flag (DISPLAY font + SemiBold + 16px when title, UI + Regular + 11px otherwise).

- [ ] **Step 2: WAuroraSeparator** — 1px horizontal/vertical gradient line that fades at both ends, using `divider` color.

- [ ] **Step 3: WAuroraQuad** — basic colored quad with rounded corners (12px default).

- [ ] **Step 4: WAuroraTooltip** — floating glass card, fades in 120ms after hover, fades out 80ms. Body text Inter Regular 11.

- [ ] **Step 5: Multi-label, separators** — straightforward variants.

- [ ] **Step 6:** Verify each on debug screen. Commit.

---

### Task 21: New layout widgets — `WAuroraSidebar`, `WAuroraBreadcrumb`, `WAuroraStatusBar`

**Files:**
- Create: each as `widgets/`-level (no inherited Meteor counterpart since they're new)

- [ ] **Step 1: `WAuroraSidebar`** — vertical container with two regions: nav items list (top) and status block (bottom). Each nav item is its own subwidget with: dot leading icon, label text. Active item gets:
  - Background: `linear(accent 0.12 → transparent)` from left
  - Left border: 2px solid accent (Animated y-offset to active item — slides on selection change)
  - Text: `textPrimary`

- [ ] **Step 2: `WAuroraBreadcrumb`** — horizontal text widget with segments separated by ` / `. Segments are clickable to navigate up.

- [ ] **Step 3: `WAuroraStatusBar`** — bottom-of-window strip with stat items. Each item: label (`textMuted`) + value (`textAccent`, JetBrains Mono). Right-aligned status text on the far right.

- [ ] **Step 4:** Verify on debug screen by composing a faux ModulesScreen. Commit.

---

### Task 22: `WAuroraModule`

**Files:**
- Create: `widgets/WAuroraModule.java`

The module card in the browser list. Render:
- Rounded rect 10px radius, bg = `hover` when hovered, transparent otherwise
- Module name in UI Medium 11
- Active indicator: small accent pip (3px) on the left with pulsing glow (`Animated` opacity loop, 2.4s sine)
- Favorite star on the right if favorited
- Subtle right-chevron on hover to indicate "click to open"

- [ ] **Steps:** Standard. Commit.

---

# Phase 3 · Screen layouts

### Task 23: Redesigned `ModulesScreen` with sidebar layout

**Files:**
- Modify: `src/main/java/meteordevelopment/meteorclient/gui/screens/ModulesScreen.java`

The existing ModulesScreen builds a horizontal-tab + grid layout. Replace with a sidebar+content layout when the active theme is Aurora.

- [ ] **Step 1: Read the existing screen** to understand widget composition and event flow.

- [ ] **Step 2: Add a layout branch**

```java
@Override
public void initWidgets() {
    GuiTheme t = GuiThemes.get();
    if (t instanceof AuroraGuiTheme) {
        buildAuroraLayout();
    } else {
        buildLegacyLayout();   // wrap the existing build code
    }
}

private void buildAuroraLayout() {
    // title bar (accent dot + "Lime" + breadcrumb)
    // sidebar with categories (Combat, Movement, Render, Player, World, Misc)
    //   below: status block (active count, idle count)
    // content area: module title + hint + settings rows
    // status bar at bottom: FPS, TPS, active count
}
```

- [ ] **Step 3: Wire category click to swap content**

Selection changes the current category. Animated indicator slides between items (handled by `WAuroraSidebar` internally).

- [ ] **Step 4: Verify**

Run client. Switch theme to Lime. Open module GUI (default keybind `right-shift`). Confirm layout matches mockup. Click each category, confirm content swaps.

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(aurora): redesigned ModulesScreen with sidebar"
```

---

### Task 24: Command palette (⌘K / Ctrl+K search)

**Files:**
- Create: `src/main/java/meteordevelopment/meteorclient/gui/screens/CommandPaletteScreen.java`
- Modify: `ModulesScreen` — bind Ctrl+K keypress to open palette

- [ ] **Step 1: Build the palette screen**

Floating centered card (480px × 360px max). Single text input at top (focused immediately), fuzzy-filtered result list below. Each result: category breadcrumb (muted) + module name + 1-line description.

```java
public class CommandPaletteScreen extends WindowTabScreen {
    private WAuroraTextBox query;
    private final List<Module> results = new ArrayList<>();

    // initWidgets builds: title bar "Search" + query + results container
    // queryChanged listener: fuzzy match against Modules.get().getAll(), update results
    // arrow-up/down navigates results, enter opens selected module
}
```

Fuzzy match: subsequence match scored by gap penalty. Don't pull in a fuzzy library — 30 lines is enough.

- [ ] **Step 2: Bind keypress**

In `ModulesScreen.keyPressed` (or wherever key events route), intercept Ctrl+K and open `CommandPaletteScreen`.

- [ ] **Step 3: Verify**

Run, open module GUI, press Ctrl+K, type "killaura", press Enter — should open the Killaura module settings.

- [ ] **Step 4: Commit**

---

### Task 25: Standalone module settings (floating window)

**Files:**
- Modify: `src/main/java/meteordevelopment/meteorclient/gui/screens/ModuleScreen.java`

When a module is opened standalone (not from the browser), present it as a smaller floating Aurora card.

- [ ] **Step 1: Switch screen background to backdrop-blurred game scene** (it should already be a transparent Screen — just ensure no opaque background fill).

- [ ] **Step 2: Wrap content in a centered `WAuroraWindow`** sized to fit content.

- [ ] **Step 3: Verify with `right-click on module entry → settings`.**

- [ ] **Step 4: Commit.**

---

### Task 26: Tab screens audit

**Files:**
- Read all files under: `src/main/java/meteordevelopment/meteorclient/gui/tabs/builtin/`
- Read: `src/main/java/meteordevelopment/meteorclient/gui/screens/accounts/`, `screens/settings/`, `screens/EditSystemScreen.java`, `screens/MarkerScreen.java`, `screens/NotebotSongsScreen.java`, `screens/ProxiesScreen.java`

- [ ] **Step 1: For each tab screen, run it in client and screenshot** with Aurora active.

- [ ] **Step 2: For any screen that has hardcoded references to `theme.color*()` settings that no longer exist on Aurora**, replace with palette tokens.

```java
// Before (using removed Meteor setting):
renderer.quad(x, y, w, h, theme.backgroundColor.get());
// After:
renderer.quad(x, y, w, h, ((AuroraGuiTheme) theme).palette().panelBase());
```

- [ ] **Step 3: Verify each tab screen opens, renders correctly, no log spam.**

- [ ] **Step 4: Commit per screen or batch.**

---

### Task 27: HUD editor styling

**Files:**
- Modify: `src/main/java/meteordevelopment/meteorclient/gui/screens/HudEditorScreen.java` (find exact filename via `grep -r "class.*HudEditor" src/`)
- Modify: any HUD-editor specific widgets

- [ ] **Step 1: Toolbar bar at top** — use Aurora title-bar treatment with Add / Lock / Reset / Done buttons (`WAuroraButton` set).

- [ ] **Step 2: Selection outline** — when an element is selected, draw a 1.5px dashed outline animated marching ants. Use `Animated` for dash offset (loops 0→1 over 1.2s linear).

- [ ] **Step 3: Snap guides** — 1px accent lines with 8px glow when alignment to other elements is detected.

- [ ] **Step 4: Right-click context menu** — small Aurora popup with options (Edit, Reset, Remove).

- [ ] **Step 5: Verify, commit.**

---

### Task 28: In-game HUD chrome

**Files:**
- Modify: `src/main/java/meteordevelopment/meteorclient/utils/player/ChatUtils.java` — already changed prefix text to "Lime"; now style it with custom font + glow
- Modify: HUD info-display rendering (find: `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/`)

- [ ] **Step 1: Chat prefix styling**

`ChatUtils.init()` currently builds a literal "Lime" component. Add font + glow:
- Render in UI SemiBold 11 if `LimeFonts.UI` available
- Bracket characters get an accent halo (drawn separately via HUD overlay rendering — actually, since chat messages are vanilla MC text, we can't apply a glow shader to them. Cut this — just leave the colored bracket. Update spec.)

> **Spec patch:** chat-prefix glow not feasible inside vanilla chat rendering. Drop it. Color tint only.

- [ ] **Step 2: HUD info-display panels (the side text overlays)**

Find the element renderer (look for `TextHudElement` or similar). Wrap text in an Aurora glass card: `roundedRectStroke` background, `accent` left-border strip (2px wide), text padding 8px.

- [ ] **Step 3: Active-module list**

Each module name as a small pill (rounded rect, accent 0.15 bg, accentLight text, 6px radius, 4px horizontal padding).

- [ ] **Step 4: Verify in-game with various HUD elements added.**

- [ ] **Step 5: Commit.**

---

# Phase 4 · Switch + cleanup

### Task 29: Make Aurora the default theme + one-time accent migration

**Files:**
- Modify: `GuiThemes.java`

- [ ] **Step 1: Change default theme**

```java
if (theme == null) select("Lime");
```

(Already changed during rebrand — re-verify.)

- [ ] **Step 2: One-time migration of user's old accent**

Add a migration check at theme init: if `themes/aurora.nbt` doesn't exist but `themes/meteor.nbt` does, read the old Meteor "accentColor" setting and use its hue as Aurora's accent default.

- [ ] **Step 3: Verify a fresh user gets Lime by default; an existing user keeps their old accent hue.**

- [ ] **Step 4: Commit.**

---

### Task 30: Delete the old Meteor theme

**Files:**
- Delete: `src/main/java/meteordevelopment/meteorclient/gui/themes/meteor/` (entire directory)
- Modify: `GuiThemes.java` — remove the registration line

- [ ] **Step 1: Verify no references**

```bash
grep -rn "MeteorGuiTheme\|themes\.meteor" src/main 2>&1 | grep -v "^Binary"
```

If anything remains, fix it.

- [ ] **Step 2: Delete the directory**

```bash
git rm -r src/main/java/meteordevelopment/meteorclient/gui/themes/meteor/
```

- [ ] **Step 3: Build and run client**

Run: `./gradlew runClient`. Open every GUI surface. No crashes.

- [ ] **Step 4: Commit**

```bash
git commit -am "chore(aurora): remove legacy Meteor theme — Aurora is now the only theme"
```

---

### Task 31: Final smoke test + push

- [ ] **Step 1: End-to-end smoke**

In a clean Minecraft session, verify each surface:
- [ ] Module browser opens with sidebar layout, backdrop-blurred game scene visible
- [ ] Click each category — animated underline slides
- [ ] Open Killaura → all settings render correctly
- [ ] Toggle Enabled — toggle animates
- [ ] Drag Range slider — value animates smoothly
- [ ] Open Mode dropdown — opens animated
- [ ] Ctrl+K — palette opens, search filters
- [ ] HUD editor: add element, drag, snap guide appears
- [ ] In-game: chat prefix is "Lime" in accent color; info-display panels show in Aurora glass cards
- [ ] Settings → Lime theme → change accent color → entire UI updates in real time
- [ ] Settings → reduced-motion = true → animations disabled
- [ ] Settings → backdrop-blur = false → falls back to solid panel base

- [ ] **Step 2: Performance check**

In F3 debug overlay, confirm ms/frame impact of Aurora ≤ 2ms over baseline on a GTX 1060-class GPU with backdrop blur on.

- [ ] **Step 3: Push to fork**

```bash
git push origin master
```

- [ ] **Step 4: Final commit (if any tweaks during smoke)**

```bash
git commit -am "polish(aurora): smoke-test fixes"
git push
```

---

## Self-Review Summary

**Spec coverage:** Every spec section has tasks:
- Color tokens → Task 4
- Typography → Task 5
- Spacing scale → enforced inline in widget tasks (no dedicated task; constants live on `AuroraPalette` / inline)
- Motion → Task 2 + 3 + per-widget animation in Tasks 13–22
- File layout → Tasks 12–22
- Theme class + 4 settings → Task 12
- BackdropBlur → Task 9
- RoundedRect → Task 6
- Glow → Task 7
- Gradient → Task 8
- Tween & Animated → Tasks 2 + 3
- Module browser → Task 23
- Module settings → Task 25
- HUD editor → Task 27
- Tab screens → Task 26
- In-game HUD → Task 28
- 3-phase implementation strategy → Phases 1, 2, 3, 4 in this plan
- Risks (blur GPU cost, JAR bloat, color migration, animation cost, hardcoded refs) → handled in Tasks 9, 5, 29, 12, 26

**Spec patch noted in plan:** Chat-prefix bracket glow (line in spec under "In-Game HUD") isn't feasible inside vanilla chat rendering — Task 28 Step 1 calls this out and drops the glow.

**Open implementation questions for the engineer:**
1. Exact `Color` constructor signatures — adapt Task 4 if `Color(r,g,b,a)` differs.
2. `FontInfo.Type` enum may not have `SEMI_BOLD` — add if missing.
3. Existing shader/framebuffer APIs in `renderer/Shader.java`, `renderer/Framebuffer.java` — Tasks 6, 7, 8, 9 reference patterns from these without showing full code. Engineer reads them before writing primitive Java glue.
4. `WWidget.getTheme()` may not exist — if not, route theme access through `GuiRenderer.theme` inside `onRender` per the existing Meteor pattern.

These are read-the-existing-code questions, not design decisions — the engineer resolves them at task time.
