/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import com.mojang.blaze3d.platform.MacosUtil;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.widgets.WAuroraBreadcrumb;
import meteordevelopment.meteorclient.gui.themes.aurora.widgets.WAuroraSidebar;
import meteordevelopment.meteorclient.gui.themes.aurora.widgets.WAuroraStatusBar;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.mixin.MinecraftAccessor;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static org.lwjgl.glfw.GLFW.*;

public class ModulesScreen extends TabScreen {
    private WCategoryController controller;
    private WWindow searchWindow;
    private WTextBox searchTextBox;

    public ModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    @Override
    public void initWidgets() {
        if (theme instanceof AuroraGuiTheme) {
            initAuroraLayout();
        } else {
            initLegacyLayout();
        }
    }

    private void initLegacyLayout() {
        controller = add(new WCategoryController()).widget();

        // Help
        WVerticalList help = add(theme.verticalList()).pad(4).bottom().widget();
        help.add(theme.label("Left click - Toggle module"));
        help.add(theme.label("Right click - Open module settings"));
    }

    // --- Aurora layout ---

    private WAuroraBrowser auroraBrowser;

    private void initAuroraLayout() {
        auroraBrowser = add(new WAuroraBrowser()).center().widget();
    }

    @Override
    protected void init() {
        super.init();
        if (controller != null) controller.refresh();
    }

    // Category

    protected WWindow createCategory(WContainer c, Category category, List<Module> moduleList) {
        WWindow w = theme.window(category.name);
        w.id = category.name;
        w.padding = 0;
        w.spacing = 0;

        if (theme.categoryIcons()) {
            w.beforeHeaderInit = wContainer -> wContainer.add(theme.item(category.icon.get())).pad(2);
        }

        c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;
        w.view.spacing = 0;

        for (Module module : moduleList) {
            w.add(theme.module(module)).expandX();
        }

        return w;
    }

    // Search

    protected void createSearchW(WContainer w, String text) {
        if (!text.isEmpty()) {
            // Titles
            List<Tuple<Module, String>> modules = Modules.get().searchTitles(text);

            if (!modules.isEmpty()) {
                WSection section = w.add(theme.section("Modules")).expandX().widget();
                section.spacing = 0;

                int count = 0;
                for (Tuple<Module, String> p : modules) {
                    if (count >= Config.get().moduleSearchCount.get() || count >= modules.size()) break;
                    section.add(theme.module(p.getA(), p.getB())).expandX();
                    count++;
                }
            }

            // Settings
            Set<Module> settings = Modules.get().searchSettingTitles(text);

            if (!settings.isEmpty()) {
                WSection section = w.add(theme.section("Settings")).expandX().widget();
                section.spacing = 0;

                int count = 0;
                for (Module module : settings) {
                    if (count >= Config.get().moduleSearchCount.get() || count >= settings.size()) break;
                    section.add(theme.module(module)).expandX();
                    count++;
                }
            }
        }
    }

    protected WWindow createSearch(WContainer c) {
        WWindow w = theme.window("Search");
        w.id = "search";
        searchWindow = w;

        if (theme.categoryIcons()) {
            w.beforeHeaderInit = wContainer -> wContainer.add(theme.item(DisplayItemUtils.toStack(Items.COMPASS))).pad(2);
        }

        c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;
        w.view.maxHeight -= 20;

        WVerticalList l = theme.verticalList();

        WTextBox text = w.add(theme.textBox("")).minWidth(140).expandX().widget();
        text.setFocused(true);
        searchTextBox = text;
        text.action = () -> {
            l.clear();
            createSearchW(l, text.get());
        };

        w.add(l).expandX();
        createSearchW(l, text.get());

        return w;
    }

    @Override
    public boolean keyPressed(KeyEvent value) {
        if (locked) return false;

        boolean cntrl = MacosUtil.IS_MACOS ? value.modifiers() == GLFW_MOD_SUPER : value.modifiers() == GLFW_MOD_CONTROL;

        if (cntrl && value.key() == GLFW_KEY_F) {
            if (searchWindow != null) searchWindow.setExpanded(true);
            if (searchTextBox != null) {
                searchTextBox.setFocused(true);
                searchTextBox.setCursorMax();
            }

            return true;
        }

        return super.keyPressed(value);
    }

    // Favorites

    protected Cell<WWindow> createFavorites(WContainer c) {
        boolean hasFavorites = Modules.get().getAll().stream().anyMatch(module -> module.favorite);
        if (!hasFavorites) return null;

        WWindow w = theme.window("Favorites");
        w.id = "favorites";
        w.padding = 0;
        w.spacing = 0;

        if (theme.categoryIcons()) {
            w.beforeHeaderInit = wContainer -> wContainer.add(theme.item(DisplayItemUtils.toStack(Items.NETHER_STAR))).pad(2);
        }

        Cell<WWindow> cell = c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;
        w.view.spacing = 0;

        createFavoritesW(w);
        return cell;
    }

    protected boolean createFavoritesW(WWindow w) {
        List<Module> modules = new ArrayList<>();

        for (Module module : Modules.get().getAll()) {
            if (module.favorite) {
                modules.add(module);
            }
        }

        modules.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name));

        for (Module module : modules) {
            w.add(theme.module(module)).expandX();
        }

        return !modules.isEmpty();
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Modules.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Modules.get());
    }

    @Override
    public void reload() {
    }

    // Stuff

    protected class WCategoryController extends WContainer {
        public final List<WWindow> windows = new ArrayList<>();
        private Cell<WWindow> favorites;

        @Override
        public void init() {
            List<Module> moduleList = new ArrayList<>();
            for (Category category : Modules.loopCategories()) {
                for (Module module : Modules.get().getGroup(category)) {
                    if (!Config.get().hiddenModules.get().contains(module)) {
                        moduleList.add(module);
                    }
                }

                // Ensure empty categories are not shown
                if (!moduleList.isEmpty()) {
                    windows.add(createCategory(this, category, moduleList));
                    moduleList.clear();
                }
            }

            windows.add(createSearch(this));

            refresh();
        }

        protected void refresh() {
            if (favorites == null) {
                favorites = createFavorites(this);
                if (favorites != null) windows.add(favorites.widget());
            } else {
                favorites.widget().clear();

                if (!createFavoritesW(favorites.widget())) {
                    remove(favorites);
                    windows.remove(favorites.widget());
                    favorites = null;
                }
            }
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double pad = theme.scale(4);
            double h = theme.scale(40);

            double x = this.x + pad;
            double y = this.y;

            for (Cell<?> cell : cells) {
                double windowWidth = getWindowWidth();
                double windowHeight = getWindowHeight();

                if (x + cell.width > windowWidth) {
                    x = x + pad;
                    y += h;
                }

                if (x > windowWidth) {
                    x = windowWidth / 2.0 - cell.width / 2.0;
                    if (x < 0) x = 0;
                }
                if (y > windowHeight) {
                    y = windowHeight / 2.0 - cell.height / 2.0;
                    if (y < 0) y = 0;
                }

                cell.x = x;
                cell.y = y;

                cell.width = cell.widget().width;
                cell.height = cell.widget().height;

                cell.alignWidget();

                x += cell.width + pad;
            }
        }
    }

    /**
     * Aurora-mode single-window module browser. Sidebar on the left, scrollable module
     * list on the right. Window chrome (glass panel + glow + header + status bar) is painted
     * inside {@link #onRender}; layout is computed in {@link #onCalculateSize} /
     * {@link #onCalculateWidgetPositions} to fix the panel to ~80% of the viewport.
     */
    protected class WAuroraBrowser extends WContainer {
        private static final double HEADER_HEIGHT = 36;
        private static final double STATUS_HEIGHT = 22;
        private static final double RADIUS = 14;

        private final List<Category> categoryList = new ArrayList<>();
        private WAuroraSidebar sidebar;
        private WAuroraBreadcrumb headerCrumb;
        private WAuroraBreadcrumb contentCrumb;
        private WView contentScroll;
        private WAuroraStatusBar statusBar;
        private int selectedIndex = 0;

        @Override
        public void init() {
            // Collect non-empty categories
            for (Category category : Modules.loopCategories()) {
                List<Module> mods = Modules.get().getGroup(category);
                if (mods == null) continue;
                boolean any = false;
                for (Module m : mods) {
                    if (!Config.get().hiddenModules.get().contains(m)) { any = true; break; }
                }
                if (any) categoryList.add(category);
            }

            // Sidebar
            sidebar = new WAuroraSidebar();
            for (int i = 0; i < categoryList.size(); i++) {
                final int idx = i;
                sidebar.addItem(categoryList.get(i).name, () -> selectCategory(idx));
            }
            sidebar.setActive(0);
            add(sidebar);

            // Content scroll view (Aurora view via theme.view())
            contentScroll = theme.view();
            contentScroll.hasScrollBar = true;
            contentScroll.scrollOnlyWhenMouseOver = true;
            add(contentScroll);
            contentCrumb = new WAuroraBreadcrumb();
            contentScroll.add(contentCrumb).pad(4);
            rebuildContent();

            // Header breadcrumb
            headerCrumb = new WAuroraBreadcrumb();
            add(headerCrumb);

            // Status bar
            statusBar = new WAuroraStatusBar();
            statusBar.addStat("fps", () -> String.valueOf(MinecraftAccessor.meteor$getFps()));
            statusBar.addStat("active", () -> String.valueOf(Modules.get().getActive().size()));
            statusBar.setStatus("lime client");
            add(statusBar);

            updateBreadcrumbs();
            updateStats();
        }

        private void selectCategory(int idx) {
            if (idx < 0 || idx >= categoryList.size() || idx == selectedIndex) return;
            selectedIndex = idx;
            rebuildContent();
            updateBreadcrumbs();
            invalidate();
        }

        private void rebuildContent() {
            contentScroll.clear();
            if (categoryList.isEmpty()) return;
            Category cat = categoryList.get(selectedIndex);
            List<Module> mods = new ArrayList<>();
            for (Module m : Modules.get().getGroup(cat)) {
                if (!Config.get().hiddenModules.get().contains(m)) mods.add(m);
            }
            contentCrumb = new WAuroraBreadcrumb(cat.name + " . " + mods.size() + " modules");
            contentScroll.add(contentCrumb).pad(4);
            for (Module m : mods) {
                contentScroll.add(theme.module(m)).expandX();
            }
        }

        private void updateBreadcrumbs() {
            if (categoryList.isEmpty()) {
                headerCrumb.setSegments("Lime", "Modules");
            } else {
                headerCrumb.setSegments("Lime", "Modules", categoryList.get(selectedIndex).name);
            }
        }

        private void updateStats() {
            int total = 0;
            for (Category c : categoryList) total += Modules.get().getGroup(c).size();
            int active = Modules.get().getActive().size();
            sidebar.setStats(active, Math.max(0, total - active));
        }

        @Override
        protected void onCalculateSize() {
            // Size to ~80% of viewport, clamped [800x500, 1200x700]
            double vw = meteordevelopment.meteorclient.utils.Utils.getWindowWidth();
            double vh = meteordevelopment.meteorclient.utils.Utils.getWindowHeight();
            width = Math.max(theme.scale(800), Math.min(theme.scale(1200), vw * 0.8));
            height = Math.max(theme.scale(500), Math.min(theme.scale(700), vh * 0.8));

            // Child sizes
            for (Cell<?> cell : cells) cell.widget().calculateSize();
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double headerH = theme.scale(HEADER_HEIGHT);
            double statusH = theme.scale(STATUS_HEIGHT);
            double pad = theme.scale(8);

            // Sidebar on the left, full body height
            double bodyTop = y + headerH;
            double bodyBottom = y + height - statusH;
            double bodyH = bodyBottom - bodyTop;

            // Sidebar — propagate theme defensively (init() ordering can leave it null)
            sidebar.theme = theme;
            sidebar.x = x + pad;
            sidebar.y = bodyTop + pad;
            sidebar.calculateSize(); // ensures width=110 scaled
            sidebar.height = bodyH - pad * 2;

            // Content view
            double contentX = sidebar.x + sidebar.width + pad;
            double contentW = (x + width - pad) - contentX;
            contentScroll.theme = theme;
            contentScroll.x = contentX;
            contentScroll.y = bodyTop + pad;
            contentScroll.maxHeight = bodyH - pad * 2;
            // Reset width/height for layout pass, then let WView calculate
            contentScroll.width = contentW;
            contentScroll.calculateSize();
            // Force final dimensions after auto-size
            contentScroll.width = contentW;
            contentScroll.height = Math.min(contentScroll.height, bodyH - pad * 2);
            contentScroll.calculateWidgetPositions();

            // Header breadcrumb — center area of header bar
            headerCrumb.theme = theme;
            headerCrumb.calculateSize();
            headerCrumb.x = x + theme.scale(120);
            headerCrumb.y = y + (headerH - headerCrumb.height) / 2;

            // Status bar
            statusBar.theme = theme;
            statusBar.x = x;
            statusBar.y = y + height - statusH;
            statusBar.width = width;
            statusBar.height = statusH;
            statusBar.calculateWidgetPositions();

            // Update cell mirrors so events route correctly
            for (Cell<?> cell : cells) {
                cell.x = cell.widget().x;
                cell.y = cell.widget().y;
                cell.width = cell.widget().width;
                cell.height = cell.widget().height;
            }
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (!(theme instanceof AuroraGuiTheme)) return;
            AuroraPalette p = ((AuroraGuiTheme) theme).palette();

            // Refresh dynamic state per frame
            updateStats();

            // Outer glow
            renderer.glow(x, y, width, height, RADIUS, 24, p.panelGlow());

            // Glass body
            renderer.roundedRectStroke(x, y, width, height, RADIUS, p.panelBase(), p.panelBorder(), 1);

            // Header bar
            double headerH = theme.scale(HEADER_HEIGHT);
            renderer.quad(x, y + headerH - 1, width, 1, p.dividerStrong());

            // Wordmark "Lime" + accent dot on the left of header
            double dot = theme.scale(7);
            double dotX = x + theme.scale(12);
            double dotY = y + headerH / 2 - dot / 2;
            renderer.glow(dotX, dotY, dot, dot, dot / 2, 6, p.accentGlow());
            renderer.roundedRect(dotX, dotY, dot, dot, dot / 2, p.accent());
            String wordmark = "Lime";
            renderer.text(wordmark, dotX + dot + theme.scale(8), y + (headerH - theme.textHeight()) / 2, p.textPrimary(), true);

            // CMD+K hint pill on the right
            String hint = "Ctrl+K";
            double hintW = theme.textWidth(hint) + theme.scale(16);
            double hintH = theme.textHeight() + theme.scale(6);
            double hintX = x + width - hintW - theme.scale(12);
            double hintY = y + (headerH - hintH) / 2;
            renderer.roundedRectStroke(hintX, hintY, hintW, hintH, 6, p.hover(), p.divider(), 1);
            renderer.text(hint, hintX + theme.scale(8), hintY + (hintH - theme.textHeight()) / 2, p.textMuted(), false);
        }
    }
}
