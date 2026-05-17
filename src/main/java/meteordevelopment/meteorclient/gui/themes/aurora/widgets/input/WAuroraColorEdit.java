/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.WQuad;
import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * Aurora-styled color swatch (the small clickable color box rendered next to the EDIT button
 * in the ColorSetting row). Renders as a rounded square with an accent-tinted border and a
 * subtle glow when hovered. Click behavior is unchanged — opening the color picker is handled
 * by the adjacent EDIT button, exactly as the Meteor variant.
 */
public class WAuroraColorEdit extends WQuad implements AuroraWidget {
    public WAuroraColorEdit(Color color) {
        super(color);
    }

    @Override
    protected void onCalculateSize() {
        // Slightly tighter than the default 32x32; matches the Aurora row height better
        double s = theme.scale(20);
        width = s;
        height = s;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme t = theme();
        AuroraPalette p = t.palette();
        double radius = 4;

        // Soft hover halo using the current swatch color
        if (mouseOver) {
            Color halo = new Color(color.r, color.g, color.b, 110);
            renderer.glow(x, y, width, height, radius, theme.scale(5), halo);
        }

        // Solid swatch (current color) + 1px accent-tinted border
        renderer.roundedRectStroke(x, y, width, height, radius, color, p.panelBorder(), 1);
    }
}
