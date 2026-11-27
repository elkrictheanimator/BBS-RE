package mchorse.bbs_mod.ui.forms;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.categories.ModelFormCategory;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.cells.CellPainter;
import mchorse.bbs_mod.ui.utils.cells.CellState;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.KeyCodes;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * How a single form cell looks: the picture of the form on the shared {@link CellPainter
 * cell ground}, its name once the cell is wide enough, its hotkey, and the quick action bar
 * while hovered.
 */
public class FormCellRenderer
{
    /** Cell width from which a name strip is drawn along the bottom edge. */
    public static final int NAME_THRESHOLD = 80;

    public static boolean hasName(int cellWidth)
    {
        return cellWidth >= NAME_THRESHOLD;
    }

    /**
     * Whether the cell says the form's whole name — it has a strip and the name fits in it.
     * What the cell can't say, the grid says by the cursor instead.
     */
    public static boolean showsWholeName(UIContext context, Form form, int w)
    {
        return hasName(w) && CellPainter.captionFits(context, form.getDisplayName(), w);
    }

    public static void render(UIContext context, Form form, int x, int y, int w, int h, CellState state)
    {
        context.batcher.clip(x, y, w, h, context);

        int previewBottom = y + h - (hasName(w) ? CellPainter.CAPTION_HEIGHT : 0);

        CellPainter.marks(context, x, y, w, h, state);
        renderPreviewOrPlaceholder(form, context, x, y, x + w, previewBottom, state.hover || state.selected);
        CellPainter.dim(context, x, y, w, h, state);

        if (hasName(w))
        {
            CellPainter.caption(context, form.getDisplayName(), x, y, w, h, state.hover || state.selected);
        }

        renderHotkey(context, form, x, y, w);

        CellPainter.bar(context, x, y, w, h, state);

        context.batcher.unclip(context);
    }

    /**
     * Render a form's 3D preview when ready, or a lightweight placeholder card while gradual
     * background loading is enabled and the model's GPU VAO/textures are not yet uploaded.
     */
    public static void renderPreviewOrPlaceholder(Form form, UIContext context, int x1, int y1, int x2, int y2, boolean priority)
    {
        if (BBSSettings.isFormPickerFoldersEnabled() && form instanceof ModelForm modelForm)
        {
            String modelId = modelForm.model.get();
            ModelManager models = BBSModClient.getModels();

            if (models.isPreviewReady(modelId))
            {
                FormUtilsClient.renderPreview(form, context, x1, y1, x2, y2);
            }
            else
            {
                boolean failed = models.isModelFailed(modelId);

                if (!failed)
                {
                    models.requestPreview(modelId, priority);
                }

                renderModelPlaceholder(context, x1, y1, x2 - x1, y2 - y1, !failed && models.isLoadingOrQueued(modelId));
            }

            return;
        }

        FormUtilsClient.renderPreview(form, context, x1, y1, x2, y2);
    }

    private static void renderModelPlaceholder(UIContext context, int x, int y, int w, int h, boolean loading)
    {
        if (w <= 8 || h <= 8)
        {
            return;
        }

        int pad = Math.min(4, Math.min(w, h) / 6);
        int bx1 = x + pad;
        int by1 = y + pad;
        int bx2 = x + w - pad;
        int by2 = y + h - pad;

        context.batcher.box(bx1, by1, bx2, by2, Colors.A12 | Colors.WHITE);
        context.batcher.outline(bx1, by1, bx2, by2, Colors.A25 | Colors.GRAY, 1);

        int iconColor;

        if (loading)
        {
            float pulse = 0.45F + 0.35F * (float) Math.sin(System.currentTimeMillis() / 180.0D);

            iconColor = Colors.mulA(Colors.A100 | BBSSettings.primaryColor.get(), pulse);
        }
        else
        {
            iconColor = Colors.A50 | Colors.GRAY;
        }

        context.batcher.icon(Icons.POSE, iconColor, x + w / 2F, y + h / 2F, 0.5F, 0.5F);
    }

    /**
     * Render a lightweight model-folder card on the Home page: folder icon, model count badge,
     * and folder name strip — without touching {@link ModelManager} or rendering any 3D model.
     */
    public static void renderFolderCard(UIContext context, ModelFormCategory folder, int x, int y, int w, int h, CellState state)
    {
        context.batcher.clip(x, y, w, h, context);

        CellPainter.marks(context, x, y, w, h, state);

        int room = Math.max(16, h - CellPainter.CAPTION_HEIGHT);
        int size = Math.max(16, Math.min(w / 2, room - 8));
        int cy = y + room / 2;
        int iconColor = state.hover || state.selected ? Colors.WHITE : Colors.LIGHTEST_GRAY;

        context.batcher.scaledIcon(Icons.FOLDER, iconColor, x + (w - size) / 2, cy - size / 2, size);

        FontRenderer font = context.batcher.getFont();
        String count = String.valueOf(folder.getForms().size());
        String countLabel = font.limitToWidth(count, Math.max(8, w - 8));

        context.batcher.textCard(countLabel, x + w - font.getWidth(countLabel) - 4, y + 4, Colors.WHITE, Colors.A50, 2);

        CellPainter.caption(context, folder.getFolderDisplayName(), x, y, w, h, state.hover || state.selected);
        CellPainter.bar(context, x, y, w, h, state);

        context.batcher.unclip(context);
    }

    private static void renderHotkey(UIContext context, Form form, int x, int y, int w)
    {
        int keybind = form.hotkey.get();

        if (keybind <= 0)
        {
            return;
        }

        FontRenderer font = context.batcher.getFont();
        String key = font.limitToWidth(KeyCodes.getName(keybind), w - 8);

        context.batcher.textCard(key, x + w - font.getWidth(key) - 4, y + 4, Colors.WHITE, Colors.A50, 2);
    }
}
