package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The "add clip" popup of the camera timeline: a strip of tabs on top, and under it the clips of the
 * open tab laid out as a grid of tiles, grouped under small headings.
 *
 * <p>It replaces a long numbered list with "Media >" rows and a Back button. Everything is on one
 * screen: the tabs say <em>what kind</em> of clip, the headings say <em>what it is for</em>, and every
 * tile has one letter — underlined in its name — that picks it from the keyboard. Which letter it is
 * is the first one of the name wherever that is free, so the keys can be guessed rather than learned.</p>
 *
 * <p>Keys: the underlined letter picks, {@code 1}-{@code 4} or Tab / Shift+Tab / the arrows change the
 * tab, Backspace returns to the menu this one was opened from, Esc closes.</p>
 */
public class UIClipPickerMenu extends UIContextMenu
{
    private static final int COLUMNS_MIN = 3;
    private static final int COLUMNS_MAX = 5;

    private static final int PAD = 6;
    private static final int GAP = 2;
    private static final int TAB_H = 22;
    private static final int HEAD_H = 12;
    private static final int GROUP_GAP = 4;
    private static final int TILE_H = 20;
    private static final int FOOT_H = 16;
    private static final int MIN_TILE_W = 78;

    /** Where a tile's name starts: after the icon. */
    private static final int TEXT_X = 24;

    /** One clip type, as a tile. */
    public static class Entry
    {
        public final Link type;
        public final Icon icon;
        public final int color;
        public final String label;

        /** The access letter as an index into 0-25 (A-Z), or -1 when none is left. */
        private int key = -1;
        /** Where that letter is in {@link #label}, or -1 when it is not one of its letters. */
        private int keyIndex = -1;

        private int x;
        private int y;
        private int w;

        public Entry(Link type, Icon icon, int color, String label)
        {
            this.type = type;
            this.icon = icon;
            this.color = color & Colors.RGB;
            this.label = label;
        }
    }

    /** A run of tiles under a heading; a group without a title is just tiles. */
    public static class Group
    {
        public final IKey title;
        public final List<Entry> entries = new ArrayList<>();

        private int y;

        public Group(IKey title)
        {
            this.title = title;
        }

        public Group add(Entry entry)
        {
            this.entries.add(entry);

            return this;
        }
    }

    /** One tab. */
    public static class Page
    {
        public final Icon icon;
        public final IKey title;
        public final List<Group> groups = new ArrayList<>();

        public Page(Icon icon, IKey title)
        {
            this.icon = icon;
            this.title = title;
        }

        public Group group(IKey title)
        {
            Group group = new Group(title);

            this.groups.add(group);

            return group;
        }

        public boolean isEmpty()
        {
            for (Group group : this.groups)
            {
                if (!group.entries.isEmpty())
                {
                    return false;
                }
            }

            return true;
        }
    }

    private final List<Page> pages;
    private final Consumer<Link> pick;

    private int page;
    /** The tile the mouse went down on: it is picked when the mouse comes up on it too. */
    private Entry pressed;

    private int width;
    private int height;
    private int columns;
    private int tileW;
    private boolean dragging;
    private int dragX;
    private int dragY;

    private int[] tabX;
    private int[] tabW;
    private boolean hasBack;

    public UIClipPickerMenu(List<Page> pages, Consumer<Link> pick)
    {
        super();

        this.pages = new ArrayList<>();
        this.pick = pick;

        for (Page page : pages)
        {
            if (!page.isEmpty())
            {
                this.pages.add(page);
                this.assignKeys(page);
            }
        }
    }

    @Override
    public boolean isEmpty()
    {
        return this.pages.isEmpty();
    }

    /* Access letters */

    private static int letter(String label, int index)
    {
        if (index < 0 || index >= label.length())
        {
            return -1;
        }

        char c = Character.toLowerCase(label.charAt(index));

        return c >= 'a' && c <= 'z' ? c - 'a' : -1;
    }

    /**
     * The first letter of a name where it is free; the ones that lost it take the first letter of
     * another word, then any letter of their name, and last of all whatever is left over (shown
     * as a badge, for names in a script the keys do not spell).
     */
    private void assignKeys(Page page)
    {
        List<Entry> all = new ArrayList<>();
        boolean[] used = new boolean[26];

        for (Group group : page.groups)
        {
            all.addAll(group.entries);
        }

        for (Entry entry : all)
        {
            int c = letter(entry.label, 0);

            if (c >= 0 && !used[c])
            {
                used[c] = true;
                entry.key = c;
                entry.keyIndex = 0;
            }
        }

        for (Entry entry : all)
        {
            if (entry.key >= 0)
            {
                continue;
            }

            for (int pass = 0; pass < 2 && entry.key < 0; pass++)
            {
                for (int i = 0; i < entry.label.length() && entry.key < 0; i++)
                {
                    int c = letter(entry.label, i);
                    boolean initial = i == 0 || letter(entry.label, i - 1) < 0;

                    if (c >= 0 && !used[c] && (pass == 1 || initial))
                    {
                        used[c] = true;
                        entry.key = c;
                        entry.keyIndex = i;
                    }
                }
            }
        }

        for (Entry entry : all)
        {
            for (int c = 0; c < 26 && entry.key < 0; c++)
            {
                if (!used[c])
                {
                    used[c] = true;
                    entry.key = c;
                }
            }
        }
    }

    /* Layout */

    private void layout(FontRenderer font, boolean back, int maxHeight)
    {
        int labelW = 0;

        for (Page page : this.pages)
        {
            for (Group group : page.groups)
            {
                for (Entry entry : group.entries)
                {
                    labelW = Math.max(labelW, font.getWidth(entry.label));
                }
            }
        }

        int tabsW = 0;

        this.hasBack = back;
        this.tabX = new int[this.pages.size()];
        this.tabW = new int[this.pages.size()];

        for (int i = 0; i < this.pages.size(); i++)
        {
            this.tabW[i] = font.getWidth(this.pages.get(i).title.get()) + 33;
            this.tabX[i] = tabsW;
            tabsW += this.tabW[i];
        }

        if (back)
        {
            tabsW += TAB_H;
        }

        /* Three columns when they fit; more columns, and so a flatter popup, when the screen is short. */
        for (int columns = COLUMNS_MIN; columns <= COLUMNS_MAX; columns++)
        {
            int natural = Math.max(MIN_TILE_W, TEXT_X + labelW + 8);
            int inner;

            this.columns = columns;
            this.width = Math.max(PAD * 2 + columns * natural + (columns - 1) * GAP, tabsW);
            inner = this.width - PAD * 2;
            this.tileW = (inner - (columns - 1) * GAP) / columns;

            int gridX = PAD + (inner - (columns * this.tileW + (columns - 1) * GAP)) / 2;
            int content = 0;

            for (Page page : this.pages)
            {
                content = Math.max(content, this.place(page, gridX));
            }

            this.height = content + FOOT_H;

            if (this.height <= maxHeight)
            {
                break;
            }
        }
    }

    /** Position the tiles of a page; returns how far down it reaches. */
    private int place(Page page, int gridX)
    {
        int y = TAB_H + PAD - 2;
        boolean first = true;

        for (Group group : page.groups)
        {
            if (group.entries.isEmpty())
            {
                continue;
            }

            if (!first)
            {
                y += GROUP_GAP;
            }

            first = false;

            if (group.title != null)
            {
                group.y = y;
                y += HEAD_H;
            }

            for (int i = 0; i < group.entries.size(); i++)
            {
                Entry entry = group.entries.get(i);

                entry.x = gridX + (i % this.columns) * (this.tileW + GAP);
                entry.y = y + (i / this.columns) * (TILE_H + GAP);
                entry.w = this.tileW;
            }

            y += ((group.entries.size() + this.columns - 1) / this.columns) * (TILE_H + GAP);
        }

        return y + PAD - GAP;
    }

    @Override
    public void setMouse(UIContext context)
    {
        this.layout(context.batcher.getFont(), context.canGoBack(), context.menu.height - 10);
        this.xy(context.mouseX(), context.mouseY()).wh(this.width, this.height).bounds(context.menu.overlay, 5);
    }

    /* Hit testing */

    private boolean inside(UIContext context, int x, int y, int w, int h)
    {
        return context.mouseX >= this.area.x + x && context.mouseX < this.area.x + x + w
            && context.mouseY >= this.area.y + y && context.mouseY < this.area.y + y + h;
    }

    private int tabAt(UIContext context)
    {
        for (int i = 0; i < this.pages.size(); i++)
        {
            if (this.inside(context, this.tabX[i], 0, this.tabW[i], TAB_H))
            {
                return i;
            }
        }

        return -1;
    }

    private Entry entryAt(UIContext context)
    {
        for (Group group : this.pages.get(this.page).groups)
        {
            for (Entry entry : group.entries)
            {
                if (this.inside(context, entry.x, entry.y, entry.w, TILE_H))
                {
                    return entry;
                }
            }
        }

        return null;
    }

    /* Actions */

    private void setPage(int page)
    {
        int count = this.pages.size();

        this.page = ((page % count) + count) % count;
        this.pressed = null;
    }

    private void choose(Entry entry)
    {
        this.pick.accept(entry.type);
        this.dismiss();
    }

    /* Input */

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return super.subMouseClicked(context);
        }

        if (context.mouseButton != 0)
        {
            return true;
        }

        if (this.hasBack && this.inside(context, this.width - TAB_H, 0, TAB_H, TAB_H))
        {
            context.backContextMenu();

            return true;
        }

        int tab = this.tabAt(context);

        if (tab >= 0)
        {
            this.setPage(tab);

            return true;
        }

        this.pressed = this.entryAt(context);

        /* The empty part of the tab strip is a handle: drag it to move the popup out of the way */
        if (this.pressed == null && tab < 0 && context.mouseY < this.area.y + TAB_H)
        {
            this.dragging = true;
            this.dragX = context.mouseX - this.area.x;
            this.dragY = context.mouseY - this.area.y;
        }

        return true;
    }

    /* Like every menu, a pick lands when the button comes back up, so that the release does not
     * fall through onto whatever the new clip puts under the cursor. */
    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (this.dragging)
        {
            this.dragging = false;

            return true;
        }

        if (this.pressed != null)
        {
            Entry entry = this.pressed;

            this.pressed = null;

            if (entry == this.entryAt(context))
            {
                this.choose(entry);
            }

            return true;
        }

        return super.subMouseReleased(context);
    }

    @Override
    public boolean subKeyPressed(UIContext context)
    {
        if (context.getKeyAction() != mchorse.bbs_mod.ui.utils.keys.KeyAction.PRESSED || Window.isCtrlPressed() || Window.isAltPressed())
        {
            return super.subKeyPressed(context);
        }

        int code = context.getKeyCode();

        if (code == GLFW.GLFW_KEY_TAB)
        {
            this.setPage(this.page + (Window.isShiftPressed() ? -1 : 1));

            return true;
        }
        else if (code == GLFW.GLFW_KEY_RIGHT)
        {
            this.setPage(this.page + 1);

            return true;
        }
        else if (code == GLFW.GLFW_KEY_LEFT)
        {
            this.setPage(this.page - 1);

            return true;
        }
        else if (code == GLFW.GLFW_KEY_BACKSPACE && this.hasBack)
        {
            context.backContextMenu();

            return true;
        }
        else if (code >= GLFW.GLFW_KEY_1 && code < GLFW.GLFW_KEY_1 + this.pages.size())
        {
            this.setPage(code - GLFW.GLFW_KEY_1);

            return true;
        }
        else if (code >= GLFW.GLFW_KEY_A && code <= GLFW.GLFW_KEY_Z && !Window.isShiftPressed())
        {
            for (Group group : this.pages.get(this.page).groups)
            {
                for (Entry entry : group.entries)
                {
                    if (entry.key == code - GLFW.GLFW_KEY_A)
                    {
                        this.choose(entry);

                        return true;
                    }
                }
            }
        }

        return super.subKeyPressed(context);
    }

    /* Rendering */

    @Override
    public void render(UIContext context)
    {
        if (this.dragging)
        {
            this.xy(context.mouseX - this.dragX, context.mouseY - this.dragY).bounds(context.menu.overlay, 5);
            this.resize();
        }

        super.render(context);

        Batcher2D batcher = context.batcher;
        FontRenderer font = batcher.getFont();
        Page page = this.pages.get(this.page);
        int ax = this.area.x;
        int ay = this.area.y;
        int textDy = (TAB_H - font.getHeight()) / 2 + 1;
        int divider = BBSSettings.dividerColor();

        /* Tabs */
        batcher.box(ax, ay, ax + this.width, ay + TAB_H, BBSSettings.deepSurface());
        batcher.box(ax, ay + TAB_H - 1, ax + this.width, ay + TAB_H, divider);

        for (int i = 0; i < this.pages.size(); i++)
        {
            Page tab = this.pages.get(i);
            int x = ax + this.tabX[i];
            boolean active = i == this.page;
            boolean hover = this.inside(context, this.tabX[i], 0, this.tabW[i], TAB_H);
            boolean lit = active || hover;

            if (active)
            {
                batcher.box(x, ay, x + this.tabW[i], ay + TAB_H, BBSSettings.raisedSurface());
                batcher.box(x, ay, x + this.tabW[i], ay + 2, BBSSettings.primaryColor(Colors.A100));
            }
            else if (hover)
            {
                RowStyle.hover(batcher, x, ay, this.tabW[i], TAB_H - 1, 0);
            }

            batcher.icon(tab.icon, RowStyle.iconColor(lit), x + 8, ay + TAB_H / 2, 0, 0.5F);
            batcher.text(tab.title.get(), x + 27, ay + textDy, RowStyle.textColor(lit), false);
        }

        if (this.hasBack)
        {
            int x = ax + this.width - TAB_H;
            boolean hover = this.inside(context, this.width - TAB_H, 0, TAB_H, TAB_H);

            if (hover)
            {
                RowStyle.hover(batcher, x, ay, TAB_H, TAB_H - 1, 0);
            }

            batcher.icon(Icons.ARROW_LEFT, RowStyle.iconColor(hover), x + TAB_H / 2, ay + TAB_H / 2, 0.5F, 0.5F);
        }

        /* Groups and tiles */
        int dim = Colors.setA(Colors.WHITE, 0.45F);

        for (Group group : page.groups)
        {
            if (group.entries.isEmpty())
            {
                continue;
            }

            if (group.title != null)
            {
                String title = group.title.get();
                int y = ay + group.y;
                int textW = font.getWidth(title);

                batcher.text(title, ax + PAD + 2, y + (HEAD_H - font.getHeight()) / 2 + 1, dim, false);
                batcher.box(ax + PAD + 2 + textW + 6, y + HEAD_H / 2, ax + this.width - PAD, y + HEAD_H / 2 + 1, divider);
            }

            for (Entry entry : group.entries)
            {
                this.renderEntry(context, entry, font);
            }
        }

        /* Footer */
        int footY = ay + this.height - FOOT_H;

        batcher.box(ax + PAD, footY, ax + this.width - PAD, footY + 1, divider);
        batcher.text(font.limitToWidth(UIKeys.CAMERA_PICKER_HINT.get(), "...", this.width - PAD * 2), ax + PAD + 2, footY + (FOOT_H - font.getHeight()) / 2 + 1, dim, false);
    }

    private void renderEntry(UIContext context, Entry entry, FontRenderer font)
    {
        Batcher2D batcher = context.batcher;
        int x = this.area.x + entry.x;
        int y = this.area.y + entry.y;
        boolean hover = this.inside(context, entry.x, entry.y, entry.w, TILE_H);
        int textY = y + (TILE_H - font.getHeight()) / 2 + 1;
        int color = RowStyle.textColor(hover);

        RowStyle.row(batcher, x, y, entry.w, TILE_H, entry.color, false, hover, this.pressed == entry);

        batcher.icon(entry.icon, RowStyle.iconColor(hover), x + 3, y + TILE_H / 2, 0, 0.5F);

        String shown = font.limitToWidth(entry.label, "...", entry.w - TEXT_X - 4);

        batcher.text(shown, x + TEXT_X, textY, color, false);

        if (entry.keyIndex >= 0 && entry.keyIndex < shown.length() && shown.length() == entry.label.length())
        {
            int lx = x + TEXT_X + font.getWidth(shown.substring(0, entry.keyIndex));
            int lw = font.getWidth(shown.substring(entry.keyIndex, entry.keyIndex + 1));

            batcher.box(lx, textY + font.getHeight(), lx + lw, textY + font.getHeight() + 1, color);
        }
        else if (entry.keyIndex < 0 && entry.key >= 0)
        {
            String badge = String.valueOf((char) ('A' + entry.key));

            batcher.text(badge, x + entry.w - font.getWidth(badge) - 4, textY, Colors.setA(Colors.WHITE, 0.4F), false);
        }
    }
}
