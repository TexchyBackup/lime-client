/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.anim.Animated;
import meteordevelopment.meteorclient.gui.renderer.anim.Easing;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.themes.aurora.widgets.pressable.WAuroraButton;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.input.MouseButtonEvent;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

/**
 * Aurora-style sliding settings detail pane. Anchored to the right edge of its parent
 * container; the parent is responsible for placing/resizing this widget every layout pass.
 *
 * <p>The pane manages its own slide animation. The parent reads {@link #slideValue()} to
 * shrink/expand the surrounding content area, and the pane itself uses the same value to
 * fade its contents and offset its on-screen X position.
 */
public class WAuroraDetailPane extends WContainer implements AuroraWidget {
    public static final double PANE_WIDTH = 360;
    private static final double HEADER_HEIGHT = 64;
    private static final double FOOTER_HEIGHT = 56;
    private static final double RADIUS = 14;
    private static final double SIDE_PAD = 14;

    private final AuroraGuiTheme auroraTheme;
    private final Animated slideAnim = new Animated(0, 0.22, Easing.OUT_CUBIC);
    private final Animated toggleAnim = new Animated(0, 0.18, Easing.OUT_CUBIC);
    private double pulsePhase;

    private Module module;
    private boolean targetOpen;

    // Inner widgets
    private WView contentScroll;
    private WContainer settingsHost;
    private WHorizontalList footerRow;
    private WKeybind bindWidget;

    // Close-button hit/visual region (set during render, read on click)
    private double closeX, closeY, closeSize;
    private boolean closeHover;

    // Toggle-switch hit/visual region (header)
    private double toggleX, toggleY, toggleW, toggleH;
    private boolean toggleHover;

    // Local mouse cache for hover queries from onRender (we get them in onMouseMoved too)
    private double lastMouseX, lastMouseY;

    /** Optional callback fired when the user closes the pane via the × button. */
    public Runnable onClose;

    public WAuroraDetailPane(AuroraGuiTheme theme) {
        this.auroraTheme = theme;
        this.theme = theme;
        this.visible = false; // hidden until shown
    }

    // --- Public API ---

    public void show(Module module) {
        this.module = module;
        this.targetOpen = true;
        this.visible = true;
        slideAnim.set(1);
        toggleAnim.setInstant(module.isActive() ? 1 : 0);
        rebuildContent();
        invalidate();
    }

    public void hide() {
        this.targetOpen = false;
        slideAnim.set(0);
        invalidate();
    }

    public boolean isOpen() {
        return targetOpen;
    }

    public Module currentModule() {
        return module;
    }

    /** Called from the screen's tick() so the settings widget tree can refresh when setting
     *  visibility changes. */
    public void tickSettings() {
        if (module == null || settingsHost == null) return;
        module.settings.tick(settingsHost, theme);
    }

    /** 0 = fully closed, 1 = fully open. */
    public double slideValue() {
        return slideAnim.get();
    }

    // --- Layout ---

    @Override
    protected void onCalculateSize() {
        width = theme.scale(PANE_WIDTH);
        // height is set externally by the parent (the browser); fall back to whatever was assigned
        if (height <= 0) {
            for (Cell<?> c : cells) c.widget().calculateSize();
        }
    }

    @Override
    protected void onCalculateWidgetPositions() {
        if (module == null) return;

        double pad = theme.scale(SIDE_PAD);
        double headerH = theme.scale(HEADER_HEIGHT);
        double footerH = theme.scale(FOOTER_HEIGHT);

        double bodyTop = y + headerH + theme.scale(8);
        double bodyBottom = y + height - footerH - theme.scale(8);
        double bodyH = Math.max(40, bodyBottom - bodyTop);

        if (contentScroll != null) {
            contentScroll.theme = theme;
            contentScroll.x = x + pad;
            contentScroll.y = bodyTop;
            contentScroll.maxHeight = bodyH;
            contentScroll.width = width - pad * 2;
            contentScroll.calculateSize();
            contentScroll.width = width - pad * 2;
            contentScroll.height = Math.min(contentScroll.height, bodyH);
            contentScroll.calculateWidgetPositions();
        }

        if (footerRow != null) {
            footerRow.theme = theme;
            footerRow.x = x + pad;
            footerRow.y = y + height - footerH + theme.scale(8);
            footerRow.width = width - pad * 2;
            footerRow.calculateSize();
            footerRow.height = footerH - theme.scale(16);
            footerRow.calculateWidgetPositions();
        }

        // Sync cell mirrors so event routing works
        for (Cell<?> cell : cells) {
            cell.x = cell.widget().x;
            cell.y = cell.widget().y;
            cell.width = cell.widget().width;
            cell.height = cell.widget().height;
        }
    }

    // --- Internal: (re)build the widget tree for the currently-shown module ---

    private void rebuildContent() {
        cells.clear();

        if (module == null) return;

        // Settings host = scroll view + a single child (the settings table from the factory)
        contentScroll = theme.view();
        contentScroll.hasScrollBar = true;
        contentScroll.scrollOnlyWhenMouseOver = true;
        add(contentScroll);

        // Wrap the existing settings widget pipeline so it lays out inside the pane.
        // Per task spec: if it returns a WTable of name|control pairs, use as-is.
        WWidget settingsWidget = theme.settings(module.settings);
        contentScroll.add(settingsWidget).expandX();
        if (settingsWidget instanceof WContainer wc) settingsHost = wc;

        // Footer with "Reset defaults" + "Bind"
        footerRow = (WHorizontalList) theme.horizontalList();
        add(footerRow);

        WButton reset = footerRow.add(theme.button("Reset")).widget();
        reset.tooltip = "Reset all settings to default";
        reset.action = () -> {
            if (module == null) return;
            for (var group : module.settings.groups) {
                for (var s : group) s.reset();
            }
            rebuildContent();
            invalidate();
        };

        footerRow.add(theme.label("Bind:"));
        bindWidget = footerRow.add(theme.keybind(module.keybind)).expandX().widget();
        bindWidget.actionOnSet = () -> Modules.get().setModuleToBind(module);
    }

    // --- Events ---

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
        if (slideAnim.get() < 0.5 || module == null) return false;
        if (click.button() != GLFW_MOUSE_BUTTON_LEFT) return false;

        double mx = lastMouseX;
        double my = lastMouseY;

        // Close (×) button
        if (mx >= closeX && mx <= closeX + closeSize && my >= closeY && my <= closeY + closeSize) {
            hide();
            if (onClose != null) onClose.run();
            return true;
        }

        // Big toggle switch in header
        if (mx >= toggleX && mx <= toggleX + toggleW && my >= toggleY && my <= toggleY + toggleH) {
            module.toggle();
            return true;
        }

        return false;
    }

    @Override
    public void onMouseMoved(double mouseX, double mouseY, double lastX, double lastY) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        closeHover = (mouseX >= closeX && mouseX <= closeX + closeSize && mouseY >= closeY && mouseY <= closeY + closeSize);
        toggleHover = (mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= toggleY && mouseY <= toggleY + toggleH);
    }

    @Override
    protected boolean propagateEvents(WWidget widget) {
        // Don't propagate interaction to children while closed/closing
        return slideAnim.get() > 0.5;
    }

    @Override
    public boolean isOver(double x, double y) {
        if (slideAnim.get() < 0.5) return false;
        return super.isOver(x, y);
    }

    // --- Rendering ---

    @Override
    public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!visible) return false;

        // Advance animations
        slideAnim.update(delta);
        if (module != null) {
            toggleAnim.set(module.isActive() ? 1 : 0);
            toggleAnim.update(delta);
            if (module.isActive()) pulsePhase = (pulsePhase + delta * (Math.PI * 2 / 2.4)) % (Math.PI * 2);
        }

        double s = slideAnim.get();
        if (s < 0.001 && !targetOpen) {
            visible = false;
            return false;
        }

        // Render with global alpha multiplier tied to slide progress
        double prevAlpha = 1.0; // GuiRenderer has setAlpha but no getter; we just restore to 1.
        renderer.setAlpha(s);
        try {
            onRender(renderer, mouseX, mouseY, delta);
            // Render children (settings list + footer) inside the pane
            for (Cell<?> cell : cells) {
                WWidget w = cell.widget();
                if (!w.visible) continue;
                w.render(renderer, mouseX, mouseY, delta);
            }
        } finally {
            renderer.setAlpha(prevAlpha);
        }

        return false;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (module == null) return;
        AuroraPalette p = auroraTheme.palette();
        double s = slideAnim.get();
        if (s < 0.001) return;

        double radius = theme.scale(RADIUS);

        // 1. Soft outer glow on the pane
        renderer.glow(x, y, width, height, radius, theme.scale(20), p.panelGlow());

        // 2. Glass body — solid panel base (the pane sits over the same browser glass)
        Color body = new Color(p.panelBase().r, p.panelBase().g, p.panelBase().b, 245);
        renderer.roundedRectStroke(x, y, width, height, radius, body, p.panelBorder(), 1);

        // 3. Glowing left seam — 1px gradient (top-transparent → accent 30% → bottom-transparent)
        Color seamTransparent = new Color(p.accent().r, p.accent().g, p.accent().b, 0);
        Color seamMid = new Color(p.accent().r, p.accent().g, p.accent().b, 80);
        // top half
        renderer.gradientLinear(x, y + radius, 1, height / 2 - radius, seamTransparent, seamMid, 90);
        // bottom half
        renderer.gradientLinear(x, y + height / 2, 1, height / 2 - radius, seamMid, seamTransparent, 90);

        // 4. Header background gradient (panelBase → slightly darker)
        double headerH = theme.scale(HEADER_HEIGHT);
        Color headerTop = p.panelBase();
        Color headerBot = new Color(
            Math.max(0, p.panelBase().r - 6),
            Math.max(0, p.panelBase().g - 6),
            Math.max(0, p.panelBase().b - 6),
            p.panelBase().a
        );
        renderer.gradientLinear(x + 1, y + 1, width - 2, headerH - 1, radius, headerTop, headerBot, 90);
        // Header bottom divider
        renderer.quad(x + theme.scale(8), y + headerH, width - theme.scale(16), 1, p.dividerStrong());

        // 5. Title + status pill (left side of header)
        double pad = theme.scale(SIDE_PAD);
        double titleY = y + theme.scale(10);
        renderer.text(module.title, x + pad, titleY, p.textPrimary(), true);

        boolean active = module.isActive();
        String pillText = active ? "active" : "inactive";
        double pillW = theme.textWidth(pillText) + theme.scale(12);
        double pillH = theme.textHeight() + theme.scale(4);
        double pillX = x + pad;
        double pillY = titleY + theme.textHeight(true) + theme.scale(4);
        Color pillFill = active
            ? new Color(p.accent().r, p.accent().g, p.accent().b, 50)
            : new Color(255, 255, 255, 16);
        Color pillBorder = active
            ? new Color(p.accent().r, p.accent().g, p.accent().b, 140)
            : p.divider();
        renderer.roundedRectStroke(pillX, pillY, pillW, pillH, pillH / 2, pillFill, pillBorder, 1);
        renderer.text(pillText, pillX + theme.scale(6), pillY + (pillH - theme.textHeight()) / 2,
            active ? p.textAccent() : p.textSecondary(), false);

        // 6. Big toggle switch (right side of header, ~50% larger than card)
        double trackW = theme.scale(50);
        double trackH = theme.scale(22);
        toggleX = x + width - pad - trackW - theme.scale(28); // leaves room for close ×
        toggleY = y + (headerH - trackH) / 2;
        toggleW = trackW;
        toggleH = trackH;
        double ta = toggleAnim.get();
        Color trackOff = new Color(70, 85, 72, 255);
        Color trackOn = p.accent();
        Color trackColor = new Color(
            (int)(trackOff.r + (trackOn.r - trackOff.r) * ta),
            (int)(trackOff.g + (trackOn.g - trackOff.g) * ta),
            (int)(trackOff.b + (trackOn.b - trackOff.b) * ta),
            255
        );
        renderer.roundedRect(toggleX, toggleY, trackW, trackH, trackH / 2, trackColor);
        if (ta > 0.05) {
            double pulse = 0.85 + 0.15 * Math.sin(pulsePhase);
            Color halo = new Color(trackOn.r, trackOn.g, trackOn.b, (int)(80 * ta * pulse));
            renderer.glow(toggleX, toggleY, trackW, trackH, trackH / 2, theme.scale(10), halo);
        }
        double thumbSize = trackH - theme.scale(4);
        double thumbX = toggleX + theme.scale(2) + (trackW - thumbSize - theme.scale(4)) * ta;
        double thumbY = toggleY + theme.scale(2);
        renderer.roundedRect(thumbX, thumbY, thumbSize, thumbSize, thumbSize / 2, new Color(245, 248, 240));

        // 7. Close × button (top-right corner)
        closeSize = theme.scale(18);
        closeX = x + width - closeSize - theme.scale(8);
        closeY = y + theme.scale(8);
        Color closeFill = closeHover ? new Color(p.accent().r, p.accent().g, p.accent().b, 60) : new Color(255, 255, 255, 12);
        Color closeBorder = closeHover ? p.panelBorder() : p.divider();
        renderer.roundedRectStroke(closeX, closeY, closeSize, closeSize, closeSize / 2, closeFill, closeBorder, 1);
        if (closeHover) {
            renderer.glow(closeX, closeY, closeSize, closeSize, closeSize / 2, theme.scale(6), p.accentGlow());
        }
        // × glyph
        String xs = "×";
        double xsW = theme.textWidth(xs);
        renderer.text(xs, closeX + (closeSize - xsW) / 2, closeY + (closeSize - theme.textHeight()) / 2,
            closeHover ? p.textPrimary() : p.textSecondary(), false);

        // 8. Description block (below header)
        double descY = y + headerH + theme.scale(10);
        String desc = module.description != null ? module.description : "";
        if (!desc.isEmpty()) {
            // Simple word wrap
            double maxW = width - pad * 2;
            int start = 0;
            int len = desc.length();
            double lineY = descY;
            double lineH = theme.textHeight() + theme.scale(2);
            while (start < len) {
                int end = len;
                while (end > start && theme.textWidth(desc.substring(start, end)) > maxW) end--;
                if (end <= start) end = Math.min(len, start + 1);
                // Try to break on space
                if (end < len) {
                    int sp = desc.lastIndexOf(' ', end);
                    if (sp > start) end = sp;
                }
                renderer.text(desc.substring(start, end), x + pad, lineY, p.textSecondary(), false);
                lineY += lineH;
                start = end;
                while (start < len && desc.charAt(start) == ' ') start++;
                if (lineY - descY > theme.scale(40)) break; // cap to ~3 lines
            }
        }

        // 9. Footer divider above bind row
        double footerTopY = y + height - theme.scale(FOOTER_HEIGHT);
        renderer.quad(x + theme.scale(8), footerTopY, width - theme.scale(16), 1, p.divider());
    }
}
