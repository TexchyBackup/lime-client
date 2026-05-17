/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.anim.Animated;
import meteordevelopment.meteorclient.gui.renderer.anim.Easing;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WAuroraSlider extends WSlider implements AuroraWidget {
    private final Animated hoverAnim = new Animated(0, 0.12, Easing.OUT_CUBIC);

    public WAuroraSlider(double value, double min, double max) {
        super(value, min, max);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme t = theme();
        AuroraPalette p = t.palette();

        double handleSize = handleSize();
        double trackH = Math.max(theme.scale(6), handleSize * 0.45);
        double trackY = y + height / 2 - trackH / 2;
        double trackX = x + handleSize / 2;
        double trackW = width - handleSize;
        double radius = trackH / 2;

        double valueWidth = valueWidth();

        // Track (full background)
        renderer.roundedRect(trackX, trackY, trackW, trackH, radius, p.sliderTrack());

        // Fill glow halo under fill
        if (valueWidth > 1) {
            renderer.glow(trackX, trackY, valueWidth, trackH, radius, theme.scale(6),
                new Color(p.accent().r, p.accent().g, p.accent().b, 70));

            // Fill gradient (accent → accentLight, left → right)
            renderer.gradientLinear(trackX, trackY, valueWidth, trackH, radius,
                p.accent(), p.accentLight(), 0);
        }

        // Hover anim
        hoverAnim.setDuration(t.animDuration(0.12));
        hoverAnim.set((handleMouseOver || dragging) ? 1 : 0);
        hoverAnim.update(delta);
        double ha = hoverAnim.get();

        // Thumb: white circle with subtle shadow halo
        double thumbSize = handleSize - theme.scale(4);
        double thumbX = trackX + valueWidth - thumbSize / 2;
        double thumbY = y + height / 2 - thumbSize / 2;

        // Soft outer shadow / glow under thumb
        double shadowAlpha = 70 + (int)(80 * ha);
        renderer.glow(thumbX, thumbY, thumbSize, thumbSize, thumbSize / 2,
            theme.scale(5 + 3 * ha),
            new Color(p.accent().r, p.accent().g, p.accent().b, (int) shadowAlpha));

        // Thumb body
        Color thumbColor = new Color(245, 248, 240);
        renderer.roundedRectStroke(thumbX, thumbY, thumbSize, thumbSize, thumbSize / 2,
            thumbColor, p.panelBorder(), 1);
    }
}
