/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WAuroraTriangle extends WTriangle implements AuroraWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraPalette p = theme().palette();

        // Transparent bg with hover tint
        Color bg = mouseOver ? p.hover() : new Color(0, 0, 0, 0);
        renderer.roundedRect(x, y, width, height, 4, bg);

        Color iconColor = mouseOver ? p.textPrimary() : p.textSecondary();
        renderer.rotatedQuad(x, y, width, height, rotation, GuiRenderer.TRIANGLE, iconColor);
    }
}
