/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora;

import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.renderer.primitives.BackdropBlur;
import meteordevelopment.meteorclient.gui.themes.aurora.widgets.WAuroraModule;
import meteordevelopment.meteorclient.gui.themes.aurora.widgets.WAuroraSection;
import meteordevelopment.meteorclient.gui.themes.aurora.widgets.WAuroraTopBar;
import meteordevelopment.meteorclient.gui.themes.aurora.widgets.WAuroraView;
import meteordevelopment.meteorclient.gui.themes.aurora.widgets.WAuroraWindow;
import meteordevelopment.meteorclient.gui.themes.aurora.widgets.input.WAuroraDropdown;
import meteordevelopment.meteorclient.gui.themes.aurora.widgets.input.WAuroraSlider;
import meteordevelopment.meteorclient.gui.themes.aurora.widgets.pressable.*;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.pressable.*;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class AuroraGuiTheme extends MeteorGuiTheme {
    private final SettingGroup sgAurora = settings.createGroup("Aurora");

    public final Setting<SettingColor> accent = sgAurora.add(new ColorSetting.Builder()
        .name("accent")
        .description("Single accent color that drives every accent value in the UI.")
        .defaultValue(new SettingColor(136, 238, 85))
        .build()
    );

    public final Setting<Boolean> backdropBlur = sgAurora.add(new BoolSetting.Builder()
        .name("backdrop-blur")
        .description("Real gaussian blur of the game scene behind GUI panels.")
        .defaultValue(true)
        .onChanged(BackdropBlur::setEnabled)
        .build()
    );

    public final Setting<Double> motionScale = sgAurora.add(new DoubleSetting.Builder()
        .name("motion-scale")
        .description("Speed multiplier for UI animations. 0 = instant.")
        .defaultValue(1.0)
        .min(0.0)
        .max(1.5)
        .sliderRange(0.0, 1.5)
        .build()
    );

    public final Setting<Boolean> reducedMotion = sgAurora.add(new BoolSetting.Builder()
        .name("reduced-motion")
        .description("Disable all UI animations.")
        .defaultValue(false)
        .build()
    );

    private AuroraPalette cachedPalette;
    private int cachedAccentHash;

    public AuroraGuiTheme() {
        super("Lime");
        settingsFactory = new DefaultSettingsWidgetFactory(this);
    }

    /** Returns a cached AuroraPalette keyed on the accent color hash. */
    public AuroraPalette palette() {
        int hash = accent.get().hashCode();
        if (cachedPalette == null || hash != cachedAccentHash) {
            cachedPalette = new AuroraPalette(accent.get());
            cachedAccentHash = hash;
        }
        return cachedPalette;
    }

    /**
     * Returns 0 when reducedMotion is enabled, otherwise {@code base * motionScale}.
     */
    public double animDuration(double base) {
        if (reducedMotion.get()) return 0;
        return base * motionScale.get();
    }

    // --- Widget factory overrides ---

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WAuroraWindow(icon, title));
    }

    @Override
    public WView view() {
        return w(new WAuroraView());
    }

    @Override
    public WSection section(String title, boolean expanded, WWidget headerWidget) {
        return w(new WAuroraSection(title, expanded, headerWidget));
    }

    @Override
    public WTopBar topBar() {
        return w(new WAuroraTopBar());
    }

    @Override
    protected WButton button(String text, GuiTexture texture) {
        return w(new WAuroraButton(text, texture));
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new WAuroraCheckbox(checked));
    }

    @Override
    public WPlus plus() {
        return w(new WAuroraPlus());
    }

    @Override
    public WMinus minus() {
        return w(new WAuroraMinus());
    }

    @Override
    public WTriangle triangle() {
        return w(new WAuroraTriangle());
    }

    @Override
    public WFavorite favorite(boolean checked) {
        return w(new WAuroraFavorite(checked));
    }

    @Override
    public WWidget module(Module module, String title) {
        return w(new WAuroraModule(module, title));
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return w(new WAuroraSlider(value, min, max));
    }

    @Override
    public <T> WDropdown<T> dropdown(T[] values, T value) {
        return w(new WAuroraDropdown<>(values, value));
    }
}
