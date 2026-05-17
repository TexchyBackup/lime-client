/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.anim.Animated;
import meteordevelopment.meteorclient.gui.renderer.anim.Easing;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * Module card: bigger size, stacked name + description, animated toggle switch on the right.
 * Left click toggles. Right click opens module settings.
 */
public class WAuroraModule extends WPressable implements AuroraWidget {
    private static final Color INACTIVE_NAME  = new Color(245, 252, 240, 255);
    private static final Color ACTIVE_NAME    = new Color(255, 255, 255, 255);
    private static final Color DESC_COLOR     = new Color(190, 205, 185, 255);
    private static final Color ROW_BASE       = new Color(14, 22, 16, 255);
    private static final Color ROW_HOVER      = new Color(28, 42, 30, 255);
    private static final Color ROW_BORDER     = new Color(255, 255, 255, 18);
    /** Off-state toggle track: distinctly lighter than the card so the switch shape reads. */
    private static final Color TOGGLE_OFF_BG  = new Color(70, 85, 72, 255);
    private static final Color TOGGLE_THUMB   = new Color(245, 248, 240, 255);

    private final Module module;
    private final String title;

    /** 0→1: smooth hover background opacity (120 ms OUT_CUBIC). */
    private final Animated hoverAnim = new Animated(0, 0.12, Easing.OUT_CUBIC);
    /** Thumb position 0 (off) → 1 (on). */
    private final Animated thumbAnim = new Animated(0, 0.18, Easing.OUT_CUBIC);

    /** Pulse phase in seconds for active glow. */
    private double pulsePhase;

    public WAuroraModule(Module module, String title) {
        this.module = module;
        this.title  = title;
        this.tooltip = null; // description shown inline on the card; no tooltip needed

        if (module.isActive()) thumbAnim.setInstant(1);
    }

    @Override
    public double pad() {
        return theme.scale(2);
    }

    @Override
    protected void onCalculateSize() {
        // Card size: title row + description row + padding
        // 52px tall is comfortable for two lines of text
        width  = theme.scale(220);   // min width; parent expandX makes it bigger
        height = theme.scale(52);
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
            // Try to find an ancestor WAuroraBrowser and slide its detail pane in.
            // If we're being rendered outside a browser (search results, etc.), fall back
            // to the legacy full-screen module screen.
            ModulesScreen.WAuroraBrowser browser = findAuroraBrowser();
            if (browser != null) {
                browser.showDetail(module);
            } else {
                mc.setScreen(theme.moduleScreen(module));
            }
        }
    }

    private ModulesScreen.WAuroraBrowser findAuroraBrowser() {
        WWidget p = this.parent;
        while (p != null) {
            if (p instanceof ModulesScreen.WAuroraBrowser b) return b;
            p = p.parent;
        }
        return null;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme t = theme();
        AuroraPalette  p = t.palette();
        boolean active   = module.isActive();

        // --- Animations ---
        hoverAnim.set(mouseOver ? 1.0 : 0.0);
        hoverAnim.update(delta);
        thumbAnim.set(active ? 1.0 : 0.0);
        thumbAnim.update(delta);
        if (active) pulsePhase = (pulsePhase + delta * (Math.PI * 2 / 2.4)) % (Math.PI * 2);

        double radius = theme.scale(8);

        // --- Card background (base → hover interpolation) ---
        double hv = hoverAnim.get();
        Color rowBg = new Color(
            (int) (ROW_BASE.r + (ROW_HOVER.r - ROW_BASE.r) * hv),
            (int) (ROW_BASE.g + (ROW_HOVER.g - ROW_BASE.g) * hv),
            (int) (ROW_BASE.b + (ROW_HOVER.b - ROW_BASE.b) * hv),
            (int) (ROW_BASE.a + (ROW_HOVER.a - ROW_BASE.a) * hv)
        );
        renderer.roundedRectStroke(x, y, width, height, radius, rowBg, ROW_BORDER, 1);

        // --- Active state: accent gradient + left border + glow halo ---
        double a = thumbAnim.get();
        if (a > 0.001) {
            Color accentBase = p.accent();
            // Gradient overlay
            Color gradLeft  = new Color(accentBase.r, accentBase.g, accentBase.b, (int)(28 * a));
            Color gradRight = new Color(accentBase.r, accentBase.g, accentBase.b, 0);
            renderer.gradientLinear(x, y, width, height, radius, gradLeft, gradRight, 0);
            // Glow
            double pulse = 0.85 + 0.15 * Math.sin(pulsePhase);
            Color glowCol = new Color(accentBase.r, accentBase.g, accentBase.b, (int)(28 * a * pulse));
            renderer.glow(x, y, width, height, radius, theme.scale(10), glowCol);
            // 3px accent left border
            Color borderCol = new Color(accentBase.r, accentBase.g, accentBase.b, (int)(255 * a));
            renderer.quad(x, y + radius / 2, theme.scale(3), height - radius, borderCol);
        }

        // --- Text: stacked name + description ---
        double padH = theme.scale(14);
        double textX = x + padH;
        // Title text uses theme's 1.25× title path; centerY relative to half height
        double titleH = theme.textHeight() * 1.25;
        double descH  = theme.textHeight();
        double totalH = titleH + theme.scale(3) + descH;
        double startY = y + (height - totalH) / 2;
        // Module name (title=true → renders bigger at 1.25× via the renderer's title pass)
        Color nameColor = active ? ACTIVE_NAME : INACTIVE_NAME;
        renderer.text(title, textX, startY, nameColor, true);
        // Description (only if present)
        String desc = module.description != null ? module.description : "";
        if (!desc.isEmpty()) {
            renderer.text(truncate(desc, width - padH * 2 - theme.scale(56)), textX, startY + titleH + theme.scale(3), DESC_COLOR, false);
        }

        // --- Toggle switch on right ---
        double trackW = theme.scale(34);
        double trackH = theme.scale(16);
        double trackX = x + width - padH - trackW;
        double trackY = y + (height - trackH) / 2;
        // Track color interpolates off-bg → accent based on thumb position
        Color accent = p.accent();
        Color trackColor = new Color(
            (int)(TOGGLE_OFF_BG.r + (accent.r - TOGGLE_OFF_BG.r) * a),
            (int)(TOGGLE_OFF_BG.g + (accent.g - TOGGLE_OFF_BG.g) * a),
            (int)(TOGGLE_OFF_BG.b + (accent.b - TOGGLE_OFF_BG.b) * a),
            255
        );
        renderer.roundedRect(trackX, trackY, trackW, trackH, trackH / 2, trackColor);
        // Subtle glow under the track when on
        if (a > 0.05) {
            Color tglow = new Color(accent.r, accent.g, accent.b, (int)(60 * a));
            renderer.glow(trackX, trackY, trackW, trackH, trackH / 2, theme.scale(6), tglow);
        }
        // Thumb (white circle, slides left↔right)
        double thumbSize = trackH - theme.scale(4);
        double thumbX = trackX + theme.scale(2) + (trackW - thumbSize - theme.scale(4)) * a;
        double thumbY = trackY + theme.scale(2);
        renderer.roundedRect(thumbX, thumbY, thumbSize, thumbSize, thumbSize / 2, TOGGLE_THUMB);
    }

    private String truncate(String s, double maxWidth) {
        if (theme.textWidth(s) <= maxWidth) return s;
        // Binary search the longest prefix that fits with "..."
        String ellipsis = "...";
        double ew = theme.textWidth(ellipsis);
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (theme.textWidth(s.substring(0, mid)) + ew <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, Math.max(0, lo)) + ellipsis;
    }
}
