package mchorse.bbs_mod.ui.forms.categories;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.forms.categories.ModelFormCategory;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.FormCellRenderer;
import mchorse.bbs_mod.ui.forms.FormGridLayout;
import mchorse.bbs_mod.ui.forms.UIFormList;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.input.items.UIItemGrid;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.cells.CellPainter;
import mchorse.bbs_mod.ui.utils.cells.CellState;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight Home-page grid that displays one card per model folder (folder name + model count)
 * without loading or rendering any 3D models. Clicking a card enters that folder.
 */
public class UIModelFolderHomeCategory extends UIItemGrid<UIModelFormCategory>
{
    public final UIFormList list;

    private final List<UIModelFormCategory> folders = new ArrayList<>();
    private final Area window = new Area();

    private boolean hoverHeader;
    private boolean pressedHeader;
    private UIModelFormCategory pressedFolder;
    private UIModelFormCategory contextFolder;

    public UIModelFolderHomeCategory(UIFormList list)
    {
        super(null, (a, b) -> a == b, new FormGridLayout());

        this.list = list;

        this.embedded();
        this.setCellSize(list.getCellSize());
        this.context(this::buildFolderContextMenu);
    }

    public void setFolders(List<UIModelFormCategory> folders)
    {
        this.folders.clear();
        this.folders.addAll(folders);
        this.pressedFolder = null;
        this.contextFolder = null;
        this.relayout();
    }

    private ValueBoolean visibilityPref()
    {
        return BBSModClient.getFormCategories().preferences.visible("models_home");
    }

    public void setExpanded(boolean expanded)
    {
        this.visibilityPref().set(expanded);
    }

    public void toggle()
    {
        ValueBoolean pref = this.visibilityPref();

        pref.set(!pref.get());
    }

    public boolean hasSelectedForm()
    {
        for (UIModelFormCategory folder : this.folders)
        {
            if (folder.selected != null)
            {
                return true;
            }
        }

        return false;
    }

    public int getFolderY(UIModelFormCategory folder)
    {
        int index = this.folders.indexOf(folder);

        if (index < 0)
        {
            return -1;
        }

        return this.isExpanded() ? this.layout.getY(index) : 0;
    }

    @Override
    public FormGridLayout getLayout()
    {
        return (FormGridLayout) this.layout;
    }

    @Override
    protected List<UIModelFormCategory> visible()
    {
        return this.folders;
    }

    @Override
    protected boolean isExpanded()
    {
        return this.visibilityPref().get();
    }

    @Override
    protected int contentSize()
    {
        return this.folders.isEmpty() ? 0 : super.contentSize();
    }

    @Override
    protected String caption(UIModelFormCategory item)
    {
        ModelFormCategory cat = item.getModelCategory();
        String countText = UIKeys.FORMS_LIST_FOLDER_COUNT.format(String.valueOf(cat.getForms().size())).get();

        return cat.getFolderDisplayName() + " (" + countText + ")";
    }

    @Override
    protected boolean showsCaption(UIContext context, UIModelFormCategory item, int cellWidth)
    {
        return CellPainter.captionFits(context, item.getModelCategory().getFolderDisplayName(), cellWidth);
    }

    @Override
    protected boolean onOpen(UIModelFormCategory item)
    {
        this.list.openModelFolder(item);

        return true;
    }

    private void buildFolderContextMenu(ContextMenuManager menu)
    {
        UIModelFormCategory target = this.contextFolder;

        if (target == null)
        {
            menu.action(Icons.FOLDER, UIKeys.FORMS_CATEGORIES_CONTEXT_OPEN_MODEL_FOLDER, () ->
            {
                UIUtils.openFolder(BBSMod.getAssetsPath(ModelManager.MODELS_PREFIX));
            });

            return;
        }

        ModelFormCategory cat = target.getModelCategory();
        String subPath = cat.folderKey.isEmpty() ? "" : cat.folderKey + "/";

        menu.action(Icons.ARROW_RIGHT, UIKeys.GENERAL_PICK, () -> this.list.openModelFolder(target));
        menu.action(Icons.FOLDER, UIKeys.FORMS_CATEGORIES_CONTEXT_OPEN_MODEL_FOLDER, () ->
        {
            File dir = BBSMod.getAssetsPath(ModelManager.MODELS_PREFIX + subPath);

            UIUtils.openFolder(dir.isDirectory() ? dir : BBSMod.getAssetsPath(ModelManager.MODELS_PREFIX));
        });
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (!this.area.isInside(context) || this.folders.isEmpty())
        {
            return false;
        }

        int x = this.contentX(context);
        int y = this.contentY(context);
        int button = context.mouseButton;

        if (this.layout.isHeader(y))
        {
            this.contextFolder = null;

            if (button != 0)
            {
                return false;
            }

            this.pressedHeader = true;
            this.pressedFolder = null;

            return true;
        }

        if (button == 1)
        {
            int index = this.indexAt(x, y);

            this.contextFolder = index == -1 ? null : this.visible().get(index);

            return false;
        }

        if (button == 0)
        {
            int index = this.indexAt(x, y);

            if (index >= 0 && index < this.visible().size())
            {
                UIModelFormCategory folder = this.visible().get(index);

                this.pressedFolder = folder;
                this.pressedHeader = false;
                this.cursor = index;
                this.selection.set(folder, this);

                return true;
            }

            this.pressedFolder = null;
            this.pressedHeader = false;
        }

        return false;
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        UIModelFormCategory folder = this.pressedFolder;
        boolean header = this.pressedHeader;

        this.pressedFolder = null;
        this.pressedHeader = false;

        if (header && this.area.isInside(context) && this.layout.isHeader(this.contentY(context)))
        {
            this.toggle();
        }
        else if (folder != null && this.area.isInside(context))
        {
            int index = this.indexAt(this.contentX(context), this.contentY(context));

            if (index >= 0 && index < this.visible().size() && this.visible().get(index) == folder)
            {
                this.list.openModelFolder(folder);
            }
        }

        return super.subMouseReleased(context);
    }

    @Override
    protected void scrollIntoView(int index)
    {
        if (index < 0 || index >= this.layout.getCount())
        {
            return;
        }

        UIScrollView forms = this.list.forms;
        int y = this.area.y - forms.area.y + this.layout.getY(index);

        forms.scroll.scrollIntoView(y, this.layout.getCellHeight() + this.layout.getGap(), this.layout.getGap());
    }

    @Override
    protected Area visibleWindow()
    {
        UIScrollView forms = this.list.forms;

        this.window.set(forms.area.x, forms.area.y + (int) forms.scroll.getScroll(), forms.area.w, forms.area.h);

        return this.window;
    }

    @Override
    protected void updateHover(UIContext context)
    {
        if (this.list.categoryDrag.isActive() || this.list.drag.isActive())
        {
            this.hoverIndex = -1;
            this.hoverHeader = false;

            return;
        }

        super.updateHover(context);

        boolean inside = this.area.isInside(context) && !context.hasContextMenu();

        this.hoverHeader = inside && this.layout.isHeader(this.contentY(context));
    }

    @Override
    public void render(UIContext context)
    {
        if (this.folders.isEmpty())
        {
            this.relayout();

            return;
        }

        if ((this.pressedFolder != null || this.pressedHeader) && !Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT))
        {
            this.pressedFolder = null;
            this.pressedHeader = false;
        }

        super.render(context);

        if (this.hoverHeader || this.hoverIndex >= 0)
        {
            context.requestCursor(GLFW.GLFW_HAND_CURSOR);
        }
    }

    @Override
    protected void renderContent(UIContext context)
    {
        this.updateHover(context);
        this.renderHeader(context);

        if (this.isExpanded())
        {
            this.renderCells(context);
        }
    }

    private void renderHeader(UIContext context)
    {
        Batcher2D batcher = context.batcher;
        FontRenderer font = batcher.getFont();
        int x = this.area.x;
        int y = this.area.y;
        int ex = this.area.ex();
        int ey = y + FormGridLayout.HEADER;
        boolean expanded = this.isExpanded();
        boolean hasPick = this.hasSelectedForm();

        batcher.box(x, y, ex, ey, BBSSettings.color(BBSSettings.chromeSurface(), Colors.A50));
        RowStyle.row(batcher, x, y, this.area.w, FormGridLayout.HEADER, 0, false, this.hoverHeader, hasPick);

        boolean lit = this.hoverHeader || hasPick;
        int textColor = RowStyle.textColor(lit);
        int my = y + FormGridLayout.HEADER / 2;
        int ix = x + UIList.ROW_PADDING;
        int textX = x + UIList.iconRowTextX(UIList.ROW_PADDING);

        UISection.renderArrow(context, ix + UIList.ARROW_SLOT / 2F, my, expanded, RowStyle.iconColor(lit));
        batcher.icon(Icons.POSE, RowStyle.iconColor(lit), ix + UIList.ARROW_SLOT + UIList.ICON_SLOT / 2F, my, 0.5F, 0.5F);

        String title = StringUtils.processColoredText(UIKeys.FORMS_CATEGORIES_MODELS.get());
        String count = String.valueOf(this.folders.size());
        int textY = y + (FormGridLayout.HEADER - font.getHeight()) / 2 + 1;

        batcher.textShadow(title, textX, textY, textColor);
        batcher.text(count, textX + font.getWidth(title) + 6, textY, Colors.GRAY);
    }

    @Override
    protected void renderCell(UIContext context, UIModelFormCategory item, int x, int y, int w, int h, CellState state)
    {
        state.selected = item.selected != null;

        FormCellRenderer.renderFolderCard(context, item.getModelCategory(), x, y, w, h, state);
    }
}
