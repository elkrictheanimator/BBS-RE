package mchorse.bbs_mod.ui.dashboard.panels.landing;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.film.clips.UIVideoClip;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Edits the landing screen banner in game: the artwork slideshow, or a two colour gradient.
 * Changes apply to the banner behind this panel right away and are kept in the settings.
 */
public class UIBannerEditorPanel extends UIOverlayPanel
{
    private final UICirculate style;
    private final UIColor start;
    private final UIColor end;

    private final float aspect;

    public UIBannerEditorPanel(float aspect)
    {
        super(IKey.constant("Chỉnh banner"));

        this.aspect = Math.max(0.5F, aspect);

        this.style = new UICirculate((b) -> BBSSettings.bannerStyle.set(b.getValue()));
        this.style.addLabel(IKey.constant("Kiểu: Ảnh"));
        this.style.addLabel(IKey.constant("Kiểu: Gradient dọc"));
        this.style.addLabel(IKey.constant("Kiểu: Gradient ngang"));
        this.style.addLabel(IKey.constant("Kiểu: Ảnh của bạn"));
        this.style.addLabel(IKey.constant("Kiểu: Video của bạn"));
        this.style.setValue(BBSSettings.bannerStyle.get());

        this.start = new UIColor((c) -> BBSSettings.bannerColorStart.set(c & Colors.RGB));
        this.start.setColor(BBSSettings.bannerColorStart.get() & Colors.RGB);

        this.end = new UIColor((c) -> BBSSettings.bannerColorEnd.set(c & Colors.RGB));
        this.end.setColor(BBSSettings.bannerColorEnd.get() & Colors.RGB);

        UILabel startLabel = UI.label(IKey.constant("Màu đầu"));
        UILabel endLabel = UI.label(IKey.constant("Màu cuối"));
        UILabel hint = UI.label(IKey.constant("Gradient hiện khi chọn kiểu Gradient (banner) hoặc Nền: Gradient."));

        /* Pick any texture as the banner picture; the style switches to it on its own */
        UIButton image = new UIButton(IKey.constant("Chọn ảnh banner..."), (b) ->
        {
            Link current = BBSSettings.bannerImage.get();

            UITexturePicker.open(this.getContext(), current, (link) ->
            {
                BBSSettings.bannerImage.set(link);
                BBSSettings.bannerStyle.set(3);
                this.style.setValue(3);
            });
        });

        UIButton video = new UIButton(IKey.constant("Chọn video banner..."), (b) -> pickBannerVideo(this.getContext(), this.style));

        UIButton removeImage = new UIButton(IKey.constant("Bỏ ảnh banner"), (b) ->
        {
            BBSSettings.bannerImage.set(null);

            if (BBSSettings.bannerStyle.get() == 3)
            {
                BBSSettings.bannerStyle.set(0);
                this.style.setValue(0);
            }
        });

        /* What the landing screen itself sits on */
        UICirculate backdrop = new UICirculate((b) -> BBSSettings.backdropStyle.set(b.getValue()));

        backdrop.addLabel(IKey.constant("Nền: Ánh sáng chuyển động"));
        backdrop.addLabel(IKey.constant("Nền: Gradient theo 2 màu"));
        backdrop.addLabel(IKey.constant("Nền: Kiểu 2.5.2"));
        backdrop.addLabel(IKey.constant("Nền: Ảnh / GIF của bạn"));
        backdrop.addLabel(IKey.constant("Nền: Video (wallpaper)"));
        backdrop.setValue(BBSSettings.backdropStyle.get());

        UIButton backdropImage = new UIButton(IKey.constant("Chọn ảnh nền (ảnh thường hoặc GIF)..."), (b) -> pickBackdropImage(this.getContext(), backdrop));
        UIButton backdropVideo = new UIButton(IKey.constant("Chọn video nền..."), (b) -> pickWallpaperVideo(this.getContext(), backdrop));

        /* Framing of the picture is done by hand on a small copy of the banner */
        UIBannerFramer framer = new UIBannerFramer(this.aspect);

        framer.h(Math.max(40, (int) (240 / this.aspect)));

        UIButton reframe = new UIButton(IKey.constant("Đặt lại vị trí ảnh"), (b) ->
        {
            BBSSettings.bannerZoom.set(1F);
            BBSSettings.bannerFocusX.set(0.5F);
            BBSSettings.bannerFocusY.set(0.5F);
        });

        UIButton bannerLink = new UIButton(IKey.constant("Dán link video banner (mp4 / YouTube)..."), (b) -> pasteBannerLink(this.getContext(), this.style));
        UIButton backdropLink = new UIButton(IKey.constant("Dán link video nền (mp4 / YouTube)..."), (b) -> pasteWallpaperLink(this.getContext(), backdrop));

        UILabel heightLabel = UI.label(IKey.constant("Chiều cao banner"));
        UITrackpad height = new UITrackpad((v) -> BBSSettings.bannerHeight.set(v.intValue()));

        height.limit(BBSSettings.bannerHeight).integer();
        height.setValue(BBSSettings.bannerHeight.get());

        UILabel volumeLabel = UI.label(IKey.constant("Âm lượng video banner (0 = tắt tiếng)"));
        UITrackpad volume = new UITrackpad((v) -> BBSSettings.bannerVolume.set(v.floatValue()));

        volume.limit(0, 1);
        volume.setValue(BBSSettings.bannerVolume.get());

        UIElement column = UI.column(5, 10, this.style, image, video, bannerLink, removeImage,
            heightLabel, height, volumeLabel, volume, framer, reframe,
            startLabel, this.start, endLabel, this.end, backdrop, backdropImage, backdropVideo, backdropLink, hint);

        column.relative(this.content).xy(0, 0).w(1F).h(1F);
        this.content.add(column);

        /* The pickers live next to the overlay, not inside it, so they would outlive the panel */
        this.onClose((e) ->
        {
            this.start.picker.removeFromParent();
            this.end.picker.removeFromParent();
        });
    }

    /** Pick the picture (or GIF) the landing screen sits on, and switch the background to it. */
    public static void pickBackdropImage(UIContext context, UICirculate style)
    {
        UITexturePicker.open(context, BBSSettings.backdropImage.get(), (link) ->
        {
            BBSSettings.backdropImage.set(link);
            BBSSettings.backdropStyle.set(3);

            if (style != null)
            {
                style.setValue(3);
            }
        });
    }

    /** Pick the video wallpaper (any video in the assets/video folder) and switch the background to it. */
    public static void pickWallpaperVideo(UIContext context, UICirculate style)
    {
        UIStringOverlayPanel panel = UIStringOverlayPanel.links(IKey.constant("Chọn video nền"), UIVideoClip.getVideoLinks(), (link) ->
        {
            BBSSettings.backgroundVideo.set(link);
            BBSSettings.backgroundVideoUrl.set("");
            BBSSettings.backdropStyle.set(4);

            if (style != null)
            {
                style.setValue(4);
            }
        });

        UIOverlay.addOverlay(context, panel.set(BBSSettings.backgroundVideo.get()));
    }

    /** Pick the video the banner plays (any video in the assets/video folder) and switch the banner to it. */
    public static void pickBannerVideo(UIContext context, UICirculate style)
    {
        UIStringOverlayPanel panel = UIStringOverlayPanel.links(IKey.constant("Chọn video banner"), UIVideoClip.getVideoLinks(), (link) ->
        {
            BBSSettings.bannerVideo.set(link);
            BBSSettings.bannerVideoUrl.set("");
            BBSSettings.bannerStyle.set(4);

            if (style != null)
            {
                style.setValue(4);
            }
        });

        UIOverlay.addOverlay(context, panel.set(BBSSettings.bannerVideo.get()));
    }

    /** Paste a web address (a direct video link, or YouTube if yt-dlp is installed) for the wallpaper. */
    public static void pasteWallpaperLink(UIContext context, UICirculate style)
    {
        pasteLink(context, "Link video nền", BBSSettings.backgroundVideoUrl, () ->
        {
            BBSSettings.backdropStyle.set(4);

            if (style != null)
            {
                style.setValue(4);
            }
        });
    }

    /** Paste a web address for the banner video. */
    public static void pasteBannerLink(UIContext context, UICirculate style)
    {
        pasteLink(context, "Link video banner", BBSSettings.bannerVideoUrl, () ->
        {
            BBSSettings.bannerStyle.set(4);

            if (style != null)
            {
                style.setValue(4);
            }
        });
    }

    private static void pasteLink(UIContext context, String title, mchorse.bbs_mod.settings.values.core.ValueString value, Runnable after)
    {
        mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel panel = new mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel(
            IKey.constant(title),
            IKey.constant("Dán link .mp4 / .webm, hoặc link YouTube (cần cài yt-dlp). Để trống để bỏ link."),
            (text) ->
            {
                value.set(text == null ? "" : text.trim());

                if (!value.get().isEmpty())
                {
                    after.run();
                }
            });

        panel.text.setText(value.get());
        UIOverlay.addOverlay(context, panel);
    }
}
