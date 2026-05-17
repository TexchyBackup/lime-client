/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;

public class WAuroraView extends WView implements AuroraWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (canScroll && hasScrollBar) {
            // Draw scrollbar handle using Aurora hover color
            renderer.roundedRect(handleX(), handleY(), handleWidth(), handleHeight(),
                handleWidth() / 2, theme().palette().hover());
        }
    }
}
