/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora;

import meteordevelopment.meteorclient.gui.utils.BaseWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

public interface AuroraWidget extends BaseWidget {
    default AuroraGuiTheme theme() {
        return (AuroraGuiTheme) ((WWidget) this).getTheme();
    }
}
