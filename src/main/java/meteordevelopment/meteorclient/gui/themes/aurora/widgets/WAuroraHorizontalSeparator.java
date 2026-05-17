/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;

public class WAuroraHorizontalSeparator extends WHorizontalSeparator implements AuroraWidget {
    public WAuroraHorizontalSeparator(String text) {
        super(text);
    }

    @Override
    protected void onCalculateSize() {
        if (text != null) {
            textWidth = theme.textWidth(text);
            // Slightly taller label row to put breathing room around section titles
            width = 1;
            height = theme.textHeight() + theme.scale(4);
        } else {
            width = 1;
            height = theme.scale(2);
        }
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme t = theme();
        AuroraPalette p = t.palette();

        if (text == null) {
            // Bare divider — single 1px line, low alpha
            double lineY = y + Math.round(height / 2.0);
            renderer.quad(x, lineY, width, 1, p.divider());
            return;
        }

        // Section label: just the title text in the accent color. No horizontal "strikethrough"
        // lines — those overlap rows below in narrow panes and we explicitly don't want them.
        renderer.text(text, x, y + theme.scale(2), p.textAccent(), false);
    }
}
