/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WAuroraMinus extends WMinus implements AuroraWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme t = theme();
        AuroraPalette p = t.palette();
        double pad = pad();
        double radius = 4;
        double s = t.scale(3);

        // Transparent bg with hover tint
        Color bg = mouseOver ? p.hover() : new Color(0, 0, 0, 0);
        renderer.roundedRect(x, y, width, height, radius, bg);

        // Minus icon — use a warm red-ish tint or just use textSecondary
        renderer.quad(x + pad, y + height / 2 - s / 2, width - pad * 2, s, new Color(255, 80, 80));
    }
}
