/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets;

import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.utils.render.color.Color;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WAuroraTopBar extends WTopBar implements AuroraWidget {
    @Override
    protected Color getButtonColor(boolean pressed, boolean hovered) {
        // Active tab or pressed/hovered: use accent; idle: use hover overlay
        if (pressed) return theme().palette().accent();
        if (hovered) return theme().palette().hover();
        return new Color(0, 0, 0, 0); // transparent bg for idle
    }

    @Override
    protected Color getNameColor() {
        return theme().palette().textPrimary();
    }
}
