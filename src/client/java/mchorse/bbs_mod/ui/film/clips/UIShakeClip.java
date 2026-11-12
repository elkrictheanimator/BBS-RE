package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.modifiers.ShakeClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.widgets.UIBitToggle;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Direction;

public class UIShakeClip extends UIClip<ShakeClip>
{
    public UITrackpad shake;
    public UITrackpad shakeAmount;
    public UIToggle noise;
    public UIBitToggle active;
    public UIButton presets;

    public UIShakeClip(ShakeClip modifier, IUIClipsDelegate editor)
    {
        super(modifier, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.shake = this.trackpad(this.clip.shake);
        this.shake.tooltip(UIKeys.CAMERA_PANELS_SHAKE, Direction.BOTTOM);

        this.shakeAmount = this.trackpad(this.clip.shakeAmount);
        this.shakeAmount.tooltip(UIKeys.CAMERA_PANELS_SHAKE_AMOUNT, Direction.BOTTOM);

        this.noise = this.toggle(UIKeys.CAMERA_PANELS_SHAKE_NOISE, this.clip.noise);
        this.noise.tooltip(UIKeys.CAMERA_PANELS_SHAKE_NOISE_TOOLTIP, Direction.BOTTOM);

        this.presets = new UIButton(IKey.constant("Kiểu rung: xe cộ, nổ, đấm, chạy..."), (b) -> b.getContext().replaceContextMenu((menu) ->
        {
            menu.action(Icons.CAMERA, IKey.constant("Cầm tay (Handheld)"), () -> this.preset(3F, 0.3F, true, 0b0111000, 0, 0));
            menu.action(Icons.CAMERA, IKey.constant("Xe cộ (rung máy)"), () -> this.preset(0.6F, 0.15F, true, 0b0110010, 0, 0));
            menu.action(Icons.CAMERA, IKey.constant("Chạy (nảy theo bước)"), () -> this.preset(1.6F, 0.8F, false, 0b0110000, 0, 0));
            menu.action(Icons.CAMERA, IKey.constant("Vụ nổ (mạnh, tắt dần)"), () -> this.preset(0.8F, 4F, true, 0b0111000, 0, 25));
            menu.action(Icons.CAMERA, IKey.constant("Cú đấm (giật ngắn)"), () -> this.preset(0.5F, 2.5F, true, 0b0011000, 0, 6));
            menu.action(Icons.CAMERA, IKey.constant("Động đất (kéo dài)"), () -> this.preset(1.2F, 1.5F, true, 0b0110101, 10, 10));
        }));
        this.presets.tooltip(IKey.constant("Điền sẵn chu kỳ, biên độ, trục và fade — chỉnh lại thoải mái sau"), Direction.BOTTOM);

        this.active = this.bind(new UIBitToggle((value) -> this.clip.active.set(value)).all(), () -> this.active.setValue(this.clip.active.get()));
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(UIKeys.C_CLIP.get("bbs:shake"), UI.row(UIConstants.MARGIN, 0, 20, this.shake, this.shakeAmount), this.noise, this.presets));
        this.panels.add(this.active);
    }

    /** Fill the shake with a ready-made character; fadeIn/fadeOut of 0 leave the envelope alone. */
    private void preset(float period, float amount, boolean noise, int mask, int fadeIn, int fadeOut)
    {
        this.clip.shake.set(period);
        this.clip.shakeAmount.set(amount);
        this.clip.noise.set(noise);
        this.clip.active.set(mask);

        if (fadeIn > 0 || fadeOut > 0)
        {
            this.clip.envelope.enabled.set(true);
            this.clip.envelope.fadeIn.set((float) fadeIn);
            this.clip.envelope.fadeOut.set((float) fadeOut);
        }
    }
}
