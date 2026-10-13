package mchorse.bbs_mod.ui.dashboard.panels.landing;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * A small copy of the banner to frame the picture on by hand: drag it with the left mouse button
 * to slide the picture, and turn the wheel to zoom in or out.
 */
public class UIBannerFramer extends UIElement
{
    private static final float MAX_ZOOM = 4F;

    private final float aspect;

    private boolean dragging;
    private int lastX;
    private int lastY;

    public UIBannerFramer(float aspect)
    {
        super();

        this.aspect = aspect;
    }

    private Texture texture()
    {
        Link link = BBSSettings.bannerImage.get();

        return link == null ? null : BBSModClient.getTextures().getTexture(link);
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context) && context.mouseButton == 0)
        {
            this.dragging = true;
            this.lastX = context.mouseX;
            this.lastY = context.mouseY;

            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        boolean was = this.dragging;

        this.dragging = false;

        return was;
    }

    @Override
    public boolean subMouseScrolled(UIContext context)
    {
        if (!this.area.isInside(context) || this.texture() == null)
        {
            return super.subMouseScrolled(context);
        }

        float zoom = BBSSettings.bannerZoom.get();

        /* Each notch is a tenth, so the zoom feels the same near 1x and near 4x */
        zoom *= (float) Math.pow(1.1, context.mouseWheel);

        BBSSettings.bannerZoom.set(Math.max(1F, Math.min(MAX_ZOOM, zoom)));

        return true;
    }

    private void drag(UIContext context, Texture texture)
    {
        float[] crop = UILandingScreen.bannerCrop(texture, this.aspect);
        float cropW = crop[2] - crop[0];
        float cropH = crop[3] - crop[1];
        float slackW = texture.width - cropW;
        float slackH = texture.height - cropH;
        int dx = context.mouseX - this.lastX;
        int dy = context.mouseY - this.lastY;

        /* The picture follows the hand: a pixel of the preview is cropW / width pixels of picture */
        if (slackW > 0.5F && dx != 0)
        {
            float shift = -dx * cropW / Math.max(1, this.area.w);

            BBSSettings.bannerFocusX.set(Math.max(0F, Math.min(1F, BBSSettings.bannerFocusX.get() + shift / slackW)));
        }

        if (slackH > 0.5F && dy != 0)
        {
            float shift = -dy * cropH / Math.max(1, this.area.h);

            BBSSettings.bannerFocusY.set(Math.max(0F, Math.min(1F, BBSSettings.bannerFocusY.get() + shift / slackH)));
        }

        this.lastX = context.mouseX;
        this.lastY = context.mouseY;
    }

    @Override
    public void render(UIContext context)
    {
        Texture texture = this.texture();
        FontRenderer font = context.batcher.getFont();

        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xFF101014);

        if (texture == null)
        {
            String hint = "Chọn ảnh banner trước";

            context.batcher.text(hint, this.area.mx(font.getWidth(hint)), this.area.my(font.getHeight()), 0xFFAAAAAA, false);
        }
        else
        {
            if (this.dragging)
            {
                this.drag(context, texture);
            }

            float[] crop = UILandingScreen.bannerCrop(texture, this.aspect);

            context.batcher.texturedBox(texture, Colors.WHITE, this.area.x, this.area.y, this.area.w, this.area.h, crop[0], crop[1], crop[2], crop[3], texture.width, texture.height);

            String hint = "Kéo: dời ảnh   Lăn chuột: phóng to";

            context.batcher.textCard(hint, this.area.x + 4, this.area.ey() - font.getHeight() - 6, Colors.WHITE, Colors.A50, 2);
        }

        context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), this.area.isInside(context) ? BBSSettings.primaryColor(Colors.A100) : 0x44FFFFFF);

        super.render(context);
    }
}
