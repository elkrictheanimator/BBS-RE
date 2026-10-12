package mchorse.bbs_mod.ui.dashboard.panels.landing;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
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

    public UIBannerEditorPanel()
    {
        super(IKey.constant("Chỉnh banner"));

        this.style = new UICirculate((b) -> BBSSettings.bannerStyle.set(b.getValue()));
        this.style.addLabel(IKey.constant("Kiểu: Ảnh"));
        this.style.addLabel(IKey.constant("Kiểu: Gradient dọc"));
        this.style.addLabel(IKey.constant("Kiểu: Gradient ngang"));
        this.style.addLabel(IKey.constant("Kiểu: Ảnh của bạn"));
        this.style.setValue(BBSSettings.bannerStyle.get());

        this.start = new UIColor((c) -> BBSSettings.bannerColorStart.set(c & Colors.RGB));
        this.start.setColor(BBSSettings.bannerColorStart.get() & Colors.RGB);

        this.end = new UIColor((c) -> BBSSettings.bannerColorEnd.set(c & Colors.RGB));
        this.end.setColor(BBSSettings.bannerColorEnd.get() & Colors.RGB);

        UILabel startLabel = UI.label(IKey.constant("Màu đầu"));
        UILabel endLabel = UI.label(IKey.constant("Màu cuối"));
        UILabel hint = UI.label(IKey.constant("Gradient chỉ hiện khi chọn kiểu Gradient."));

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

        UIButton removeImage = new UIButton(IKey.constant("Bỏ ảnh banner"), (b) ->
        {
            BBSSettings.bannerImage.set(null);

            if (BBSSettings.bannerStyle.get() == 3)
            {
                BBSSettings.bannerStyle.set(0);
                this.style.setValue(0);
            }
        });

        /* The landing screen's own background takes the same two colours */
        UIToggle backdrop = new UIToggle(IKey.constant("Nền màn hình chính theo gradient"), BBSSettings.backdropGradient.get(), (b) -> BBSSettings.backdropGradient.set(b.getValue()));

        /* Framing of the picture: how far it is zoomed, and which part of it the banner shows */
        UITrackpad zoom = new UITrackpad((v) -> BBSSettings.bannerZoom.set(v.floatValue()));
        UITrackpad focusX = new UITrackpad((v) -> BBSSettings.bannerFocusX.set(v.floatValue()));
        UITrackpad focusY = new UITrackpad((v) -> BBSSettings.bannerFocusY.set(v.floatValue()));

        zoom.limit(1, 4).setValue(BBSSettings.bannerZoom.get());
        focusX.limit(0, 1).setValue(BBSSettings.bannerFocusX.get());
        focusY.limit(0, 1).setValue(BBSSettings.bannerFocusY.get());

        UIButton reframe = new UIButton(IKey.constant("Đặt lại vị trí ảnh"), (b) ->
        {
            BBSSettings.bannerZoom.set(1F);
            BBSSettings.bannerFocusX.set(0.5F);
            BBSSettings.bannerFocusY.set(0.5F);
            zoom.setValue(1);
            focusX.setValue(0.5);
            focusY.setValue(0.5);
        });

        UIElement column = UI.column(5, 10, this.style, image, removeImage,
            UI.label(IKey.constant("Phóng to ảnh")), zoom,
            UI.label(IKey.constant("Vị trí ngang (0 trái - 1 phải)")), focusX,
            UI.label(IKey.constant("Vị trí dọc (0 trên - 1 dưới)")), focusY, reframe,
            startLabel, this.start, endLabel, this.end, backdrop, hint);

        column.relative(this.content).xy(0, 0).w(1F).h(1F);
        this.content.add(column);

        /* The pickers live next to the overlay, not inside it, so they would outlive the panel */
        this.onClose((e) ->
        {
            this.start.picker.removeFromParent();
            this.end.picker.removeFromParent();
        });
    }
}
