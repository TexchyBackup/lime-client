/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WAuroraButton extends WButton implements AuroraWidget {
    public WAuroraButton(String text, GuiTexture texture) {
        super(text, texture);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme t = theme();
        AuroraPalette p = t.palette();
        double pad = pad();
        double radius = 10;

        // Determine fill: pressed = accent, hovered = accent glow overlay, idle = panelBase
        Color fill;
        if (pressed) fill = p.accent();
        else if (mouseOver) fill = p.accentGlow();
        else fill = p.panelBase();

        // Hover glow halo
        if (mouseOver || pressed) {
            renderer.glow(x, y, width, height, radius, 8, p.accentGlow());
        }

        renderer.roundedRectStroke(x, y, width, height, radius, fill, p.panelBorder(), 1);

        Color textColor = (pressed || mouseOver) ? p.textPrimary() : p.textSecondary();

        if (text != null) {
            renderer.text(text, x + width / 2 - textWidth / 2, y + pad, textColor, false);
        } else {
            double ts = t.textHeight();
            renderer.quad(x + width / 2 - ts / 2, y + pad, ts, ts, texture, textColor);
        }
    }
}
