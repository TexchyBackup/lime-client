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
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class WAuroraModule extends WPressable implements AuroraWidget {
    private static final Color INACTIVE_TEXT  = new Color(180, 195, 175, 220);
    private static final Color HOVER_FILL     = new Color(255, 255, 255, 16);

    private final Module module;
    private final String title;

    private double titleWidth;

    /** 0→1: smooth hover background opacity (120 ms OUT_CUBIC). */
    private final Animated hoverAnim = new Animated(0, 0.12, Easing.OUT_CUBIC);

    /** 0→1: active gradient / border / pip visibility. Mirrors WMeteorModule's animationProgress2. */
    private double activeAnim;

    /** Pulse phase in seconds (0–2π), advanced only while module is active. */
    private double pulsePhase;

    public WAuroraModule(Module module, String title) {
        this.module = module;
        this.title  = title;
        this.tooltip = module.description;

        // Initialise at end-state so first frame shows the correct visual.
        activeAnim = module.isActive() ? 1.0 : 0.0;
        hoverAnim.setInstant(0);
    }

    @Override
    public double pad() {
        return theme.scale(4);
    }

    @Override
    protected void onCalculateSize() {
        double padH = theme.scale(10);
        double padV = theme.scale(8);

        if (titleWidth == 0) titleWidth = theme.textWidth(title);

        // pip (4 px radius = 8 px diameter) + 4 px gap + title + trailing padding
        double pipSpace = theme.scale(8 + 4);
        width  = padH + pipSpace + titleWidth + padH;
        height = padV + theme.textHeight() + padV;
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == GLFW_MOUSE_BUTTON_RIGHT) mc.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme t  = theme();
        AuroraPalette   p  = t.palette();
        boolean active     = module.isActive();

        // --- Advance animations ---
        double animSpeed = t.reducedMotion.get() ? 1_000_000 : (t.motionScale.get() > 0 ? t.motionScale.get() : 1_000_000);
        activeAnim += delta * 6 * animSpeed * (active ? 1 : -1);
        activeAnim  = Mth.clamp(activeAnim, 0, 1);

        hoverAnim.set(mouseOver ? 1.0 : 0.0);
        hoverAnim.update(delta);

        if (active) pulsePhase = (pulsePhase + delta * (Math.PI * 2 / 2.4)) % (Math.PI * 2);

        double radius = theme.scale(8);

        // --- Background ---
        // Idle: very faint hover token
        if (AuroraPalette.HOVER.a > 0) {
            renderer.roundedRect(x, y, width, height, radius, AuroraPalette.HOVER);
        }

        // Hover overlay
        double hv = hoverAnim.get();
        if (hv > 0.001) {
            Color hc = new Color(255, 255, 255, (int)(16 * hv));
            renderer.roundedRect(x, y, width, height, radius, hc);
        }

        // Active: left-to-right gradient (accent 0.15 alpha → transparent) + glow
        if (activeAnim > 0.001) {
            Color accentBase = p.accent();
            Color gradLeft  = new Color(accentBase.r, accentBase.g, accentBase.b, (int)(38 * activeAnim));
            Color gradRight = new Color(accentBase.r, accentBase.g, accentBase.b, 0);
            renderer.gradientLinear(x, y, width, height, radius, gradLeft, gradRight, 0);

            // Soft glow halo
            Color glowCol = new Color(accentBase.r, accentBase.g, accentBase.b, (int)(20 * activeAnim));
            renderer.glow(x, y, width, height, radius, theme.scale(8), glowCol);

            // 2 px accent left border
            Color borderCol = new Color(accentBase.r, accentBase.g, accentBase.b, (int)(255 * activeAnim));
            renderer.quad(x, y + radius / 2, theme.scale(2), height - radius, borderCol);
        }

        // --- Pip dot ---
        double padH   = theme.scale(10);
        double pipR   = theme.scale(4);
        double pipCX  = x + padH + pipR;
        double pipCY  = y + height / 2.0;
        double pipSize = pipR * 2;

        if (activeAnim > 0.001) {
            double pulseOpacity = 0.8 + 0.2 * Math.sin(pulsePhase);
            Color pip = new Color(p.accent().r, p.accent().g, p.accent().b, (int)(255 * activeAnim * pulseOpacity));
            renderer.quad(pipCX - pipR, pipCY - pipR, pipSize, pipSize, pip);
        }

        // --- Label ---
        double textX = pipCX + pipR + theme.scale(4);
        double textY = y + (height - theme.textHeight()) / 2.0;
        Color textColor = active ? p.textPrimary() : INACTIVE_TEXT;
        renderer.text(title, textX, textY, textColor, false);

        // --- Trailing favorite star ---
        if (module.favorite) {
            double starSize = theme.textHeight();
            double starX = x + width - padH - starSize;
            double starY = y + (height - starSize) / 2.0;
            renderer.quad(starX, starY, starSize, starSize, GuiRenderer.FAVORITE_YES, p.accent());
        }
    }
}
