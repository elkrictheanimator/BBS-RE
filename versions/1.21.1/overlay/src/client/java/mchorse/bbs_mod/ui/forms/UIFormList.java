package mchorse.bbs_mod.ui.forms;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.forms.categories.ModelFormCategory;
import mchorse.bbs_mod.forms.categories.UserFormCategory;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.sections.UserFormSection;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.categories.UIFormCategory;
import mchorse.bbs_mod.ui.forms.categories.UIModelFolderHomeCategory;
import mchorse.bbs_mod.ui.forms.categories.UIModelFormCategory;
import mchorse.bbs_mod.ui.forms.categories.UIRecentFormCategory;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.items.ItemDrag;
import mchorse.bbs_mod.ui.framework.elements.input.items.Selection;
import mchorse.bbs_mod.ui.framework.elements.input.items.UIItemGrid;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import mchorse.bbs_mod.ui.morphing.UIMorphFormCategoryFilterOverlayPanel;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Marquee;
import mchorse.bbs_mod.ui.utils.Scroll;
import mchorse.bbs_mod.ui.utils.ScrollZoomAnchor;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIStrip;
import mchorse.bbs_mod.ui.utils.cells.DragGhost;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.KeyAction;
import mchorse.bbs_mod.ui.utils.keys.KeyCodes;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.DiffuseLighting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The list of form categories with the search bar under it.
 *
 * <p>Besides the categories themselves it owns everything that spans them: the cell size
 * (Ctrl + wheel zooms it), the {@link #selection multi-selection} and the {@link #drag} of
 * forms, which every category grid shares (so a pick runs across categories and a drag
 * started in one drops into another), the {@link #marquee band} stretched over several of
 * them, and the {@link #categoryDrag drag} of user categories among themselves.</p>
 */
public class UIFormList extends UIElement
{
    public static final int ZOOM_STEP = 8;
    public static final int BAR_HEIGHT = 20;
    public static final int STATUS_HEIGHT = 16;

    public IUIFormList palette;

    public UIScrollView forms;

    public UIStrip bar;
    public UIIcon back;
    public UITextbox search;
    public UIIcon edit;
    public UIIcon close;
    public UIIcon categoryFilter;
    public UIIcon collapseAll;
    public UIIcon expandAll;
    public UIElement folderNavBar;

    /* Forms are matched by identity: two equal-looking forms in different categories are different picks */
    public final Selection<Form> selection = new Selection<>((a, b) -> a == b);
    public final ItemDrag<Form> drag = new ItemDrag<>((a, b) -> a == b);

    /** A user category carried by its header to a new place among the others. */
    public final ItemDrag<UserFormCategory> categoryDrag = new ItemDrag<>();

    /** Shift + drag band, in the scroll view's content coordinates. */
    public final Marquee marquee = new Marquee();

    /* Where a Shift-press landed: a band that goes nowhere extends the pick to that form */
    private UIFormCategory marqueeCategory;
    private Form marqueeForm;

    /* What was picked before the band started; the band adds to it */
    private final List<Form> marqueeBase = new ArrayList<>();

    private UIFormCategory recent;
    /** Every enabled category (including unmounted model folders while on Home). */
    private final List<UIFormCategory> allCategories = new ArrayList<>();
    /** Enabled model folder categories for the Home folder cards view. */
    private final List<UIModelFormCategory> modelCategories = new ArrayList<>();
    /** Categories currently mounted inside {@link #forms}. */
    private List<UIFormCategory> categories = new ArrayList<>();

    private UIModelFolderHomeCategory folderHome;
    private UIModelFormCategory activeFolder;
    private String activeFolderKey;
    private int homeScroll = -1;
    private boolean lastFolderMode;
    private boolean wasShowingFolderHome;

    private long lastUpdate;
    private int lastScroll = -1;
    private boolean pendingScrollToSelected;

    /** A header pressed but not yet released: a release without a drag collapses it. */
    private UIFormCategory pressedHeader;

    public UIFormList(IUIFormList palette)
    {
        this.palette = palette;

        this.forms = UI.scrollView(0, 0);
        this.forms.scroll.cancelScrolling();
        this.bar = new UIStrip(BAR_HEIGHT);
        this.back = new UIIcon(Icons.ARROW_LEFT, (b) -> this.exitFolder());
        this.back.tooltip(UIKeys.GENERAL_BACK, Direction.TOP);
        this.back.w(20);
        this.back.setVisible(false);
        this.search = new UITextbox(100, this::onSearchQuery).placeholder(UIKeys.FORMS_LIST_SEARCH);
        this.edit = new UIIcon(Icons.EDIT, this::edit);
        this.edit.tooltip(UIKeys.FORMS_LIST_EDIT, Direction.TOP);
        this.close = new UIIcon(Icons.CLOSE, this::close);

        this.folderNavBar = new UIFolderNavBar();
        this.folderNavBar.setVisible(false);
        this.folderHome = new UIModelFolderHomeCategory(this);

        /* The bar sits along the top, as dark as the category headers, with a status line
         * about the chosen form under it; the list scrolls under both */
        this.bar.relative(this).xy(0, 0).w(1F).h(BAR_HEIGHT);
        this.folderNavBar.relative(this).xy(0, BAR_HEIGHT + STATUS_HEIGHT).w(1F).h(BAR_HEIGHT);
        this.forms.relative(this).xy(0, BAR_HEIGHT + STATUS_HEIGHT).w(1F).h(1F, -BAR_HEIGHT - STATUS_HEIGHT);
        this.close.w(20);

        this.categoryFilter = new UIIcon(Icons.FILTER, this::openMorphCategoryFilter);
        this.categoryFilter.tooltip(UIKeys.MORPHING_FILTER_CATEGORIES, Direction.TOP);
        this.categoryFilter.w(20);
        this.collapseAll = new UIIcon(Icons.COLLAPSE_ALL, (b) -> this.setAllExpanded(false));
        this.collapseAll.tooltip(UIKeys.FORMS_LIST_COLLAPSE_ALL, Direction.TOP);
        this.collapseAll.w(20);
        this.expandAll = new UIIcon(Icons.EXPAND_ALL, (b) -> this.setAllExpanded(true));
        this.expandAll.tooltip(UIKeys.FORMS_LIST_EXPAND_ALL, Direction.TOP);
        this.expandAll.w(20);
        this.bar.add(this.back, this.categoryFilter, this.collapseAll, this.expandAll, this.search, this.edit, this.close);

        this.add(this.forms, this.folderNavBar, this.bar);

        this.search.keys().register(Keys.FORMS_FOCUS, this::focusSearchInput);

        this.markContainer();
        this.setupForms(BBSModClient.getFormCategories());
    }

    public boolean isInFolder()
    {
        return this.activeFolder != null;
    }

    public boolean isActiveFolder(UIModelFormCategory category)
    {
        return category != null && this.activeFolder == category;
    }

    public boolean isShowingFolderHome()
    {
        String s = this.search.getText();
        boolean emptySearch = s == null || s.trim().isEmpty();

        return this.lastFolderMode && this.activeFolder == null && emptySearch && !this.modelCategories.isEmpty();
    }

    public void openModelFolder(UIModelFormCategory folder)
    {
        if (folder == null)
        {
            return;
        }

        this.homeScroll = (int) this.forms.scroll.getScroll();
        this.activeFolder = folder;
        this.activeFolderKey = folder.getModelCategory().folderKey;
        this.rebuildDisplayedCategories();
        this.forms.scroll.cancelScrolling();

        if (folder.selected != null)
        {
            this.scrollToSelected();
        }
        else
        {
            this.forms.scroll.setScroll(0);
        }
    }

    public void exitFolder()
    {
        if (this.activeFolder == null)
        {
            return;
        }

        BBSModClient.getModels().cancelPendingPreviews();

        this.activeFolder = null;
        this.activeFolderKey = null;

        int restoreScroll = this.homeScroll;

        this.homeScroll = -1;

        if (!this.search.getText().isEmpty())
        {
            this.search.setText("");

            for (UIFormCategory category : this.allCategories)
            {
                category.search("");
            }
        }

        this.rebuildDisplayedCategories();
        this.forms.scroll.cancelScrolling();

        if (restoreScroll >= 0)
        {
            this.lastScroll = restoreScroll;
        }
    }

    private void updateChromeLayout()
    {
        boolean inFolder = this.isInFolder();
        int topOffset = BAR_HEIGHT + STATUS_HEIGHT + (inFolder ? BAR_HEIGHT : 0);

        this.back.setVisible(inFolder);
        this.collapseAll.setVisible(!inFolder);
        this.expandAll.setVisible(!inFolder);
        this.folderNavBar.setVisible(inFolder);
        this.folderNavBar.relative(this).xy(0, BAR_HEIGHT + STATUS_HEIGHT).w(1F).h(BAR_HEIGHT);
        this.forms.relative(this).xy(0, topOffset).w(1F).h(1F, -topOffset);
    }

    private void openMorphCategoryFilter(UIIcon b)
    {
        Set<String> disabled = BBSSettings.disabledMorphFormCategories.get();
        FormCategories formCategories = BBSModClient.getFormCategories();
        UIMorphFormCategoryFilterOverlayPanel panel = new UIMorphFormCategoryFilterOverlayPanel(
            disabled,
            formCategories.getAllCategories()
        );

        UIOverlay.addOverlay(this.getContext(), panel, 240, 0.9F);

        panel.onClose(e ->
        {
            BBSSettings.disabledMorphFormCategories.set(disabled);
            Form selected = this.getSelected();
            this.setupForms(formCategories);
            this.setSelected(selected);
        });
    }

    public void focusSearchInput()
    {
        UIContext context = this.getContext();

        if (context != null)
        {
            this.search.clickItself(context);
        }
    }

    public int getCellSize()
    {
        return BBSSettings.formCellSize.get();
    }

    public void setupForms(FormCategories forms)
    {
        this.lastFolderMode = BBSSettings.isFormPickerFoldersEnabled();
        this.allCategories.clear();
        this.modelCategories.clear();
        this.categories.clear();
        this.forms.removeAll();
        this.recent = null;

        for (FormCategory category : forms.getAllCategories())
        {
            if (BBSSettings.disabledMorphFormCategories.get().contains(category.visible.getId()))
            {
                continue;
            }

            UIFormCategory uiCategory = category.createUI(this);

            this.allCategories.add(uiCategory);

            if (uiCategory instanceof UIModelFormCategory modelUi)
            {
                this.modelCategories.add(modelUi);
            }

            if (uiCategory instanceof UIRecentFormCategory)
            {
                this.recent = uiCategory;
            }
        }

        if (this.lastFolderMode && this.activeFolderKey != null)
        {
            UIModelFormCategory matched = null;

            for (UIModelFormCategory modelUi : this.modelCategories)
            {
                if (modelUi.getModelCategory().folderKey.equals(this.activeFolderKey))
                {
                    matched = modelUi;

                    break;
                }
            }

            this.activeFolder = matched;

            if (matched == null)
            {
                this.activeFolderKey = null;
            }
        }
        else
        {
            this.activeFolder = null;
            this.activeFolderKey = null;
        }

        this.folderHome.setFolders(this.modelCategories);

        String query = this.search.getText();
        String normalized = query == null ? "" : query.trim();

        for (UIFormCategory category : this.allCategories)
        {
            category.search(normalized);
        }

        this.lastUpdate = forms.getLastUpdate();
        this.rebuildDisplayedCategories();
        this.reconcile();
    }

    private void rebuildDisplayedCategories()
    {
        for (UIFormCategory category : this.allCategories)
        {
            category.marginBottom(0);
        }

        this.folderHome.marginBottom(0);
        this.categories.clear();
        this.forms.removeAll();

        UIElement lastAdded = null;
        boolean showHomeCards = this.isShowingFolderHome();

        this.wasShowingFolderHome = showHomeCards;

        if (this.activeFolder != null)
        {
            this.forms.add(this.activeFolder);
            this.categories.add(this.activeFolder);
            lastAdded = this.activeFolder;
        }
        else if (showHomeCards)
        {
            boolean addedHome = false;

            for (UIFormCategory uiCategory : this.allCategories)
            {
                if (uiCategory instanceof UIModelFormCategory)
                {
                    if (!addedHome)
                    {
                        this.forms.add(this.folderHome);
                        lastAdded = this.folderHome;
                        addedHome = true;
                    }
                }
                else
                {
                    this.forms.add(uiCategory);
                    this.categories.add(uiCategory);
                    lastAdded = uiCategory;
                }
            }
        }
        else
        {
            for (UIFormCategory uiCategory : this.allCategories)
            {
                this.forms.add(uiCategory);
                this.categories.add(uiCategory);
                lastAdded = uiCategory;
            }
        }

        if (lastAdded != null)
        {
            lastAdded.marginBottom(20);
        }

        this.updateChromeLayout();
        this.afterSearchLayout();
    }

    private void onSearchQuery(String search)
    {
        this.applySearchFilter(search);
    }

    private void applySearchFromTextbox()
    {
        this.applySearchFilter(this.search.getText());
    }

    private void applySearchFilter(String raw)
    {
        String s = raw == null ? "" : raw.trim();

        for (UIFormCategory category : this.allCategories)
        {
            category.search(s);
        }

        if (this.lastFolderMode && this.activeFolder == null && this.wasShowingFolderHome != this.isShowingFolderHome())
        {
            this.rebuildDisplayedCategories();
            this.forms.scroll.setScroll(0);

            return;
        }

        this.afterSearchLayout();
    }

    /** Lay every category out again — the cell size, the search or the width changed. */
    private void afterSearchLayout()
    {
        int size = this.getCellSize();

        this.folderHome.setCellSize(size);

        for (UIFormCategory category : this.allCategories)
        {
            category.setCellSize(size);
        }

        /* Two passes: a category can only measure itself once it has been placed (it needs its
         * real width), and it reports the new height as a layout to run later rather than
         * resizing this list from inside the pass. The second pass places them at those
         * heights, so whoever reads a category's area right after this — scrollToSelectedForm()
         * — reads final offsets. */
        this.resize();
        this.resize();
    }

    private void edit(UIIcon b)
    {
        this.palette.toggleEditor();
    }

    private void close(UIIcon b)
    {
        this.palette.exit();
    }

    /* Zoom */

    @Override
    protected IUIElement childrenMouseScrolled(UIContext context)
    {
        if (Window.isCtrlPressed() && context.mouseWheel != 0 && this.forms.area.isInside(context))
        {
            this.zoom(context, context.mouseWheel > 0 ? ZOOM_STEP : -ZOOM_STEP);

            return this;
        }

        return super.childrenMouseScrolled(context);
    }

    /**
     * Grow or shrink the cells, keeping whatever row is under the cursor where it is on
     * screen — zooming that jumps the list around is worse than no zoom at all.
     */
    private void zoom(UIContext context, int delta)
    {
        int old = this.getCellSize();
        int size = MathUtils.clamp(old + delta, FormGridLayout.MIN_CELL, FormGridLayout.MAX_CELL);

        if (size == old)
        {
            return;
        }

        ScrollZoomAnchor.keep(this.forms.scroll, context.mouseY - this.forms.area.y, this::rowAt, this::rowPlacement, () ->
        {
            BBSSettings.formCellSize.set(size);
            this.afterSearchLayout();
        });
    }

    /** A row of cells, or a category's header (row -1) — what the zoom keeps under the cursor. */
    private record Row(UIItemGrid<?> grid, int row)
    {}

    private Row rowAt(int contentY)
    {
        if (this.wasShowingFolderHome)
        {
            Row homeHit = this.rowInGrid(this.folderHome, contentY);

            if (homeHit != null)
            {
                return homeHit;
            }
        }

        for (UIFormCategory category : this.categories)
        {
            Row hit = this.rowInGrid(category, contentY);

            if (hit != null)
            {
                return hit;
            }
        }

        return null;
    }

    private Row rowInGrid(UIItemGrid<?> grid, int contentY)
    {
        int top = grid.area.y - this.forms.area.y;

        if (contentY < top || contentY >= top + grid.area.h)
        {
            return null;
        }

        FormGridLayout layout = (FormGridLayout) grid.getLayout();
        int y = contentY - top;

        if (y >= layout.getRowY(0) && layout.getRows() > 0)
        {
            return new Row(grid, Math.min(layout.getRows() - 1, (y - layout.getRowY(0)) / (layout.getCellHeight() + FormGridLayout.GAP)));
        }

        return new Row(grid, -1);
    }

    private ScrollZoomAnchor.Placement rowPlacement(Row row)
    {
        FormGridLayout layout = (FormGridLayout) row.grid().getLayout();
        int top = row.grid().area.y - this.forms.area.y;

        if (row.row() < 0)
        {
            return new ScrollZoomAnchor.Placement(top, FormGridLayout.HEADER);
        }

        return new ScrollZoomAnchor.Placement(top + layout.getRowY(row.row()), layout.getCellHeight());
    }

    /* Collapsing */

    private void setAllExpanded(boolean expanded)
    {
        if (this.wasShowingFolderHome)
        {
            this.folderHome.setExpanded(expanded);
        }

        for (UIFormCategory category : this.categories)
        {
            category.category.visible.set(expanded);
        }
    }

    /* Selection */

    public void selectCategory(UIFormCategory category, Form form, boolean notify)
    {
        this.deselect();

        category.selected = form;

        if (notify)
        {
            this.palette.accept(form);
        }
    }

    public void deselect()
    {
        for (UIFormCategory category : this.allCategories)
        {
            category.selected = null;
        }
    }

    public UIFormCategory getSelectedCategory()
    {
        for (UIFormCategory category : this.allCategories)
        {
            if (category.selected != null)
            {
                return category;
            }
        }

        return null;
    }

    public Form getSelected()
    {
        UIFormCategory category = this.getSelectedCategory();

        return category == null ? null : category.selected;
    }

    public void setSelected(Form form)
    {
        boolean found = false;

        this.deselect();

        for (UIFormCategory category : this.allCategories)
        {
            int index = category.category.getForms().indexOf(form);

            if (index == -1)
            {
                category.selected = null;
            }
            else
            {
                found = true;

                category.select(category.category.getForms().get(index), false);
            }
        }

        if (!found && form != null && this.recent != null)
        {
            Form copy = FormUtils.copy(form);

            this.recent.category.addForm(copy);
            this.recent.select(copy, false);
        }
    }

    /** The category that holds a form, or null once it's been removed. */
    public FormCategory categoryOf(Form form)
    {
        for (UIFormCategory category : this.allCategories)
        {
            if (this.selection.indexOf(category.category.getForms(), form) != -1)
            {
                return category.category;
            }
        }

        return null;
    }

    /** Whether every picked form lives in a category the user may take it out of. */
    public boolean canModifySelection()
    {
        for (Form form : this.selection.getItems())
        {
            FormCategory category = this.categoryOf(form);

            if (category == null || !category.canModify(form))
            {
                return false;
            }
        }

        return !this.selection.isEmpty();
    }

    public void copySelectionTo(FormCategory to, boolean move)
    {
        for (Form form : new ArrayList<>(this.selection.getItems()))
        {
            FormCategory from = this.categoryOf(form);

            if (move && from != null && from.canModify(form) && from != to)
            {
                from.removeForm(form);
                to.addForm(form);
            }
            else if (!move)
            {
                to.addForm(FormUtils.copy(form));
            }
        }

        this.reconcile();
    }

    public void removeSelection()
    {
        for (Form form : new ArrayList<>(this.selection.getItems()))
        {
            FormCategory from = this.categoryOf(form);

            if (from != null && from.canModify(form))
            {
                from.removeForm(form);
            }
        }

        this.reconcile();
    }

    /**
     * Drop marks that point at forms no longer in any category — after a removal, a move,
     * or a rebuild of the categories.
     */
    public void reconcile()
    {
        for (UIFormCategory category : this.allCategories)
        {
            if (category.selected != null && this.selection.indexOf(category.category.getForms(), category.selected) == -1)
            {
                category.selected = null;
            }
        }

        this.selection.retain((form) -> this.categoryOf(form) != null);
    }

    /* Pointer, reported by categories */

    /** Shift went down in a category: arm a band from the cursor, in the scroll view's content space. */
    public void pressMarquee(UIFormCategory category, Form form, UIContext context)
    {
        this.marqueeCategory = category;
        this.marqueeForm = form;
        this.marqueeBase.clear();
        this.marqueeBase.addAll(this.selection.getItems());
        this.marquee.press(context.mouseX - this.forms.area.x, context.mouseY - this.forms.area.y);
    }

    /**
     * While the band is stretched, the pick follows it live — what was picked before the
     * press stays, every form the band covers, in any category, joins.
     */
    private void applyMarquee()
    {
        if (!this.marquee.isActive())
        {
            return;
        }

        this.selection.clear();

        for (Form form : this.marqueeBase)
        {
            this.selection.add(form, this.categoryOf(form));
        }

        Area band = this.marquee.getArea();
        Area local = new Area();

        for (UIFormCategory category : this.categories)
        {
            local.copy(band);
            local.x -= category.area.x - this.forms.area.x;
            local.y -= category.area.y - this.forms.area.y;

            for (Form form : category.getFormsIn(local))
            {
                this.selection.add(form, category.category);
            }
        }
    }

    private void releaseMarquee()
    {
        /* An active band has already applied itself while stretching; a press that went
         * nowhere is a Shift-click, which extends the pick to that form */
        if (!this.marquee.isActive() && this.marqueeForm != null && this.marqueeCategory != null)
        {
            this.selection.range(this.marqueeForm, this.marqueeCategory.category, this.marqueeCategory.getForms());
        }

        this.marquee.reset();
        this.marqueeCategory = null;
        this.marqueeForm = null;
        this.marqueeBase.clear();
    }

    /**
     * A header went down: on release it collapses, unless the cursor travelled — then a user
     * category is carried. The gesture is tracked in global coordinates, as the list (not
     * the category, whose mouse is shifted by the scrolling) feeds it every frame.
     */
    public void pressHeader(UIFormCategory category, UIContext context)
    {
        if (this.activeFolder == category)
        {
            return;
        }

        this.pressedHeader = category;

        if (category.category instanceof UserFormCategory user)
        {
            this.categoryDrag.start(Collections.singletonList(user), context.globalX(context.mouseX), context.globalY(context.mouseY));
        }
    }

    public UIFormCategory getPressedHeader()
    {
        return this.pressedHeader;
    }

    /**
     * The button went up: finish what the list itself tracks — the band and a carried
     * category — and forget whatever was pressed. A drag of forms is the grids' own; the one
     * that sees the release resolves it through {@link #dropForms}.
     */
    private void release()
    {
        if (this.marquee.isPressed())
        {
            this.releaseMarquee();
        }

        if (this.categoryDrag.isActive())
        {
            this.dropCategory();
        }

        this.pressedHeader = null;
        this.categoryDrag.reset();
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.isInFolder() && context.mouseButton == GLFW.GLFW_MOUSE_BUTTON_4 && this.area.isInside(context))
        {
            this.exitFolder();

            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (this.isInFolder() && !context.isFocused() && context.getKeyAction() == KeyAction.PRESSED && context.getKeyCode() == GLFW.GLFW_KEY_BACKSPACE)
        {
            this.exitFolder();

            return true;
        }

        return super.subKeyPressed(context);
    }

    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        this.release();

        return false;
    }

    /* Drop */

    /**
     * Forms dropped into a category land before the form that was at the caret — the same
     * form for every one of them, so a group keeps its order. Within their own category they
     * are rearranged (when its order is manual); from another they move if that category can
     * let them go and copy otherwise. Ctrl always copies.
     */
    public void dropForms(UIFormCategory target, int insertion, List<Form> forms)
    {
        FormCategory into = target.category;

        if (!into.canModify(null) || insertion < 0)
        {
            return;
        }

        List<Form> view = target.getForms();
        Form before = null;

        for (int i = insertion; i < view.size(); i++)
        {
            if (this.selection.indexOf(forms, view.get(i)) == -1)
            {
                before = view.get(i);

                break;
            }
        }

        boolean copy = Window.isCtrlPressed();

        for (Form form : forms)
        {
            FormCategory from = this.categoryOf(form);
            int index = before == null ? into.getForms().size() : this.selection.indexOf(into.getForms(), before);

            if (from == into && !copy)
            {
                into.moveForm(form, index);

                continue;
            }

            if (!copy && from != null && from.canModify(form))
            {
                from.removeForm(form);
                into.insertForm(index, form);
            }
            else
            {
                into.insertForm(index, FormUtils.copy(form));
            }
        }

        this.reconcile();
    }

    /** The carried category lands above (insertion 0) or below (1) the category it was dropped on. */
    private void dropCategory()
    {
        if (!(this.categoryDrag.getTarget() instanceof UIFormCategory target) || !(target.category instanceof UserFormCategory user))
        {
            return;
        }

        UserFormSection section = BBSModClient.getFormCategories().getUserForms();

        section.moveUserCategory(this.categoryDrag.getItems().get(0), section.categories.indexOf(user) + this.categoryDrag.getInsertion());
    }

    /* Scrolling */

    /**
     * Request the list to scroll so that the currently selected form becomes
     * visible. The actual scrolling is deferred to {@link #render(UIContext)}
     * because it needs the list's real (resized) width to lay the categories
     * out at their final height.
     */
    public void scrollToSelected()
    {
        this.pendingScrollToSelected = true;
    }

    private void scrollToSelectedForm()
    {
        UIFormCategory category = this.getSelectedCategory();

        if (category == null)
        {
            return;
        }

        /* Categories only learn their real height once they render (until then
         * they keep the inflated, width-0 height from setupForms), so the bounds
         * of off-screen categories above the selection are stale. Lay them all
         * out at the real width first so the offsets below are final. */
        this.afterSearchLayout();

        if (this.wasShowingFolderHome && category instanceof UIModelFormCategory modelCat)
        {
            int contentY = this.folderHome.area.y - this.forms.area.y;
            int cardY = this.folderHome.getFolderY(modelCat);
            int itemHeight = FormGridLayout.HEADER;

            if (cardY > 0)
            {
                contentY += cardY;
                itemHeight = this.folderHome.getLayout().getCellHeight();
            }

            this.forms.scroll.setScroll(contentY - (this.forms.area.h - itemHeight) / 2);

            return;
        }

        if (!this.categories.contains(category))
        {
            return;
        }

        int contentY = category.area.y - this.forms.area.y;
        int formY = category.getFormY(category.selected);
        int itemHeight = FormGridLayout.HEADER;

        if (formY > 0)
        {
            contentY += formY;
            itemHeight = category.getLayout().getCellHeight();
        }

        /* Center the selected form (or the category header when it's collapsed)
         * within the visible area; scrollTo() clamps to the scroll bounds. */
        this.forms.scroll.setScroll(contentY - (this.forms.area.h - itemHeight) / 2);
    }

    private void autoScroll(UIContext context)
    {
        if (this.drag.isActive() || this.categoryDrag.isActive())
        {
            this.forms.scroll.autoScrollAt(context.mouseX, context.mouseY, Scroll.AUTO_SCROLL_EDGE, Scroll.AUTO_SCROLL_SPEED);
        }
    }

    /* Rendering */

    @Override
    public void render(UIContext context)
    {
        FormCategories categories = BBSModClient.getFormCategories();

        if (this.lastUpdate != categories.getLastUpdate() || this.lastFolderMode != BBSSettings.isFormPickerFoldersEnabled())
        {
            this.lastScroll = (int) this.forms.scroll.getScroll();

            Form selected = this.getSelected();

            this.setupForms(categories);
            this.setSelected(selected);
        }

        if (this.lastScroll >= 0)
        {
            this.forms.scroll.scrollTo(this.lastScroll);

            this.lastScroll = -1;
        }

        /* A release swallowed by another element (a button, an overlay) must not leave a
         * gesture hanging on the cursor; the grids guard their drag of forms the same way */
        if ((this.categoryDrag.isPressed() || this.pressedHeader != null || this.marquee.isPressed()) && !Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT))
        {
            this.release();
        }

        this.marquee.update(context.mouseX - this.forms.area.x, context.mouseY - this.forms.area.y + (int) this.forms.scroll.getScroll());
        this.applyMarquee();
        this.categoryDrag.update(context.mouseX, context.mouseY);
        this.categoryDrag.clearTarget();
        this.autoScroll(context);

        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + BAR_HEIGHT, BBSSettings.color(BBSSettings.chromeSurface(), Colors.A50));
        this.renderStatus(context);

        DiffuseLighting.method_34742();

        super.render(context);

        DiffuseLighting.disableGuiDepthLighting();

        if (this.pendingScrollToSelected && this.forms.area.w > 0)
        {
            this.scrollToSelectedForm();

            this.pendingScrollToSelected = false;
        }

        if (this.marquee.isActive())
        {
            context.batcher.clip(this.forms.area, context);
            this.marquee.render(context, this.forms.area.x, this.forms.area.y - (int) this.forms.scroll.getScroll());
            context.batcher.unclip(context);
        }

        if (this.drag.isActive() || this.categoryDrag.isActive())
        {
            this.renderGhost(context);
        }
    }

    /**
     * The line under the bar: the chosen form — its type icon, name, id and hotkey — or,
     * with several picked, how many.
     */
    private void renderStatus(UIContext context)
    {
        Batcher2D batcher = context.batcher;
        FontRenderer font = batcher.getFont();
        int y = this.area.y + BAR_HEIGHT;
        int textY = y + (STATUS_HEIGHT - font.getHeight()) / 2 + 1;
        int x = this.area.x + 4;

        batcher.box(this.area.x, y, this.area.ex(), y + STATUS_HEIGHT, BBSSettings.color(BBSSettings.chromeSurface(), Colors.A50));

        Form selected = this.getSelected();

        if (this.selection.isGroup())
        {
            batcher.textShadow(UIKeys.FORMS_LIST_STATUS_SELECTED.format(String.valueOf(this.selection.size())).get(), x, textY, Colors.LIGHTEST_GRAY);

            return;
        }

        if (selected == null)
        {
            return;
        }

        batcher.icon(selected.getIcon(), x, y + STATUS_HEIGHT / 2, 0F, 0.5F);
        x += 20;

        String name = selected.getDisplayName();

        batcher.textShadow(name, x, textY);
        x += font.getWidth(name) + 8;

        String id = selected.getFormId();

        batcher.text(id, x, textY, Colors.GRAY);
        x += font.getWidth(id) + 8;

        if (selected.hotkey.get() > 0)
        {
            batcher.textCard(KeyCodes.getName(selected.hotkey.get()), x + 2, textY, Colors.WHITE, Colors.A50, 2);
        }
    }

    /**
     * What's being carried, drawn beside the cursor: a small stack of the forms, or the
     * category's name. The grids sit inside the scroll view's clip, so the list draws it
     * for them, after everything else.
     */
    private void renderGhost(UIContext context)
    {
        Batcher2D batcher = context.batcher;
        int primary = BBSSettings.primaryColor.get();
        int x = context.mouseX + 10;
        int y = context.mouseY + 10;

        if (this.categoryDrag.isActive())
        {
            boolean landing = this.categoryDrag.getTarget() != null;

            batcher.textCard(this.categoryDrag.getItems().get(0).getProcessedTitle(), x + 4, y + 4, Colors.WHITE, landing ? Colors.A100 | primary : Colors.A75, 4);

            return;
        }

        List<Form> forms = this.drag.getItems();
        boolean landing = this.drag.getTarget() != null;
        int size = Math.min(this.getCellSize(), 48);
        int h = FormGridLayout.cellHeightFor(size);

        DragGhost.render(context, context.mouseX, context.mouseY, size, h, forms.size(), landing, (ctx, gx, gy, gw, gh) ->
        {
            FormCellRenderer.renderPreviewOrPlaceholder(forms.get(0), ctx, gx, gy, gx + gw, gy + gh, true);
        });

        if (landing && this.isCopyDrop())
        {
            batcher.textCard("+", x - 4, y - 4, Colors.WHITE, Colors.A100 | primary, 3);
        }
    }

    /** Whether the drop in progress would leave the originals where they are. */
    private boolean isCopyDrop()
    {
        if (Window.isCtrlPressed())
        {
            return true;
        }

        for (Form form : this.drag.getItems())
        {
            FormCategory from = this.categoryOf(form);

            if (from == null || !from.canModify(form))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Pinned navigation strip shown at the top of an opened model folder: a clickable
     * Back-to-Home button on the left, the folder's name and count in the middle, and
     * live gradual-loading progress on the right while visible models are still loading.
     */
    private class UIFolderNavBar extends UIElement
    {
        private int backWidth(FontRenderer font)
        {
            return 22 + font.getWidth(UIKeys.FORMS_LIST_BACK_TO_HOME.get()) + 10;
        }

        @Override
        public boolean subMouseClicked(UIContext context)
        {
            if (!this.isVisible() || !this.area.isInside(context) || context.mouseButton != 0)
            {
                return false;
            }

            int bw = this.backWidth(context.batcher.getFont());

            if (context.mouseX <= this.area.x + bw)
            {
                UIFormList.this.exitFolder();

                return true;
            }

            return true;
        }

        @Override
        public void render(UIContext context)
        {
            UIModelFormCategory folder = UIFormList.this.activeFolder;

            if (folder == null)
            {
                return;
            }

            Batcher2D batcher = context.batcher;
            FontRenderer font = batcher.getFont();
            int x = this.area.x;
            int y = this.area.y;
            int h = this.area.h;
            int ex = this.area.ex();
            int ey = y + h;
            int bw = this.backWidth(font);
            boolean hoverBack = this.area.isInside(context) && context.mouseX <= x + bw;

            batcher.box(x, y, ex, ey, BBSSettings.color(BBSSettings.chromeSurface(), Colors.A75));
            batcher.box(x, ey - 1, ex, ey, BBSSettings.dividerColor());

            RowStyle.row(batcher, x, y, bw, h, 0, false, hoverBack, false);

            if (hoverBack)
            {
                context.requestCursor(GLFW.GLFW_HAND_CURSOR);
            }

            int backColor = RowStyle.textColor(hoverBack);
            int textY = y + (h - font.getHeight()) / 2 + 1;

            batcher.icon(Icons.ARROW_LEFT, RowStyle.iconColor(hoverBack), x + 11, y + h / 2F, 0.5F, 0.5F);
            batcher.textShadow(UIKeys.FORMS_LIST_BACK_TO_HOME.get(), x + 22, textY, backColor);

            batcher.box(x + bw, y + 3, x + bw + 1, ey - 3, BBSSettings.dividerColor());

            ModelFormCategory cat = folder.getModelCategory();
            List<Form> shown = folder.getForms();
            int total = shown.size();
            int ready = 0;
            boolean anyLoading = false;
            ModelManager models = BBSModClient.getModels();

            for (Form form : shown)
            {
                if (form instanceof ModelForm modelForm)
                {
                    String id = modelForm.model.get();

                    if (models.isPreviewReady(id))
                    {
                        ready += 1;
                    }
                    else if (models.isLoadingOrQueued(id))
                    {
                        anyLoading = true;
                    }
                }
            }

            int fx = x + bw + 8;

            batcher.icon(Icons.FOLDER, Colors.LIGHTEST_GRAY, fx + 6, y + h / 2F, 0.5F, 0.5F);
            fx += 18;

            String progress = anyLoading ? UIKeys.FORMS_LIST_LOADING_MODELS.format(String.valueOf(ready), String.valueOf(total)).get() : null;
            int rightReserve = progress == null ? 8 : font.getWidth(progress) + 16;
            int maxTitleWidth = Math.max(20, ex - fx - rightReserve - 36);
            String folderTitle = font.limitToWidth(cat.getFolderDisplayName(), maxTitleWidth);

            batcher.textShadow(folderTitle, fx, textY, Colors.WHITE);
            batcher.text(String.valueOf(total), fx + font.getWidth(folderTitle) + 6, textY, Colors.GRAY);

            if (progress != null)
            {
                batcher.text(progress, ex - font.getWidth(progress) - 6, textY, Colors.LIGHTER_GRAY);
            }

            super.render(context);
        }
    }
}
