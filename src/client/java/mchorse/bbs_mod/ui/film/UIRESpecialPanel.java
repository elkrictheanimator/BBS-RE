package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.dashboard.panels.landing.UIBannerEditorPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * The "RE" workspace tab: one place for the features that are specific to this build, so they
 * don't have to be hunted down across the settings and the editors.
 */
public class UIRESpecialPanel extends UIElement
{
    public UIRESpecialPanel()
    {
        super();

        /* Timeline */
        UIToggle classicOrder = new UIToggle(IKey.constant("Sắp xếp track kiểu BBS 2.5.2"), BBSSettings.classicTrackOrder.get(), (b) -> BBSSettings.classicTrackOrder.set(b.getValue()));

        classicOrder.tooltip(IKey.constant("Bật: các track của form xếp phẳng như bản 2.5.2. Tắt: gom theo nhóm pose."));

        UIElement trackHint = UI.label(IKey.constant("Màu từng bộ phận: Replay -> nút lọc track -> ô màu bên phải mỗi dòng."));

        /* Animation */
        UITrackpad armSwing = new UITrackpad((v) -> BBSSettings.armSwing.set(v.floatValue()));

        armSwing.limit(0, 1).setValue(BBSSettings.armSwing.get());
        armSwing.tooltip(IKey.constant("0 = tay đứng yên khi đi, 1 = vung tay như mặc định."));

        /* Look */
        UIToggle backdrop = new UIToggle(IKey.constant("Nền màn hình chính theo gradient"), BBSSettings.backdropGradient.get(), (b) -> BBSSettings.backdropGradient.set(b.getValue()));
        UIButton banner = new UIButton(IKey.constant("Chỉnh banner..."), (b) -> UIOverlay.addOverlay(this.getContext(), new UIBannerEditorPanel(), 280, 380));

        UIScrollView view = UI.scrollView(5, 10,
            this.header("Timeline"), classicOrder, trackHint,
            this.header("Hoạt họa"), UI.label(IKey.constant("Độ vung tay khi đi (0 - 1)")), armSwing,
            this.header("Giao diện"), banner, backdrop);

        view.relative(this).full(this);
        this.add(view);
    }

    private UIElement header(String text)
    {
        return UI.label(IKey.constant(text)).background(Colors.A25);
    }

    @Override
    public void render(UIContext context)
    {
        this.area.render(context.batcher, 0xFF1D1D1D);

        super.render(context);
    }
}
