/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.anim.Animated;
import meteordevelopment.meteorclient.gui.renderer.anim.Easing;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;

public class WAuroraCheckbox extends WCheckbox implements AuroraWidget {
    private final Animated fillAnim;

    public WAuroraCheckbox(boolean checked) {
        super(checked);
        fillAnim = new Animated(checked ? 1 : 0, 0.12, Easing.OUT_CUBIC);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme t = theme();
        AuroraPalette p = t.palette();
        double radius = 4;

        fillAnim.setDuration(t.animDuration(0.12));
        fillAnim.set(checked ? 1.0 : 0.0);
        fillAnim.update(delta);
        double anim = fillAnim.get();

        if (anim > 0) {
            // Glow when (partially) checked
            renderer.glow(x, y, width, height, radius, 6, p.accentGlow());
            renderer.roundedRectStroke(x, y, width, height, radius, p.accent(), p.panelBorder(), 1);
            // Inner fill scales from center
            if (anim < 1) {
                double cs = (width - t.scale(2)) / 1.75 * anim;
                renderer.roundedRect(x + (width - cs) / 2, y + (height - cs) / 2, cs, cs, radius * anim, p.accent());
            }
        } else {
            // Unchecked: panelBase bg with divider border
            renderer.roundedRectStroke(x, y, width, height, radius, p.panelBase(), p.divider(), 1);
        }
    }
}
