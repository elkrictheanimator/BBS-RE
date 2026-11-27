package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.CameraEffectClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;

public class UICameraEffectClip extends UIClip<CameraEffectClip>
{
    private UIButton type;
    private UITrackpad strength;
    private UITrackpad size;
    private UITrackpad speed;
    private UIToggle customColor;
    private UIColor color;
    private UITrackpad seed;
    private UITrackpad pulses;

    private UITrackpad blinks;
    private UITrackpad closed;
    private UITrackpad soft;
    private UITrackpad curve;
    private UITrackpad dip;

    private UICirculate shape;
    private UITrackpad fov;
    private UITrackpad roll;
    private UITrackpad pitch;
    private UITrackpad yaw;

    public UICameraEffectClip(CameraEffectClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.type = this.bind(new UIButton(IKey.constant(""), (b) ->
        {
            UIListOverlayPanel panel = new UIListOverlayPanel(IKey.constant("Camera effect"), (label) ->
            {
                for (int i = 0; i < CameraEffectClip.LABELS.length; i++)
                {
                    if (CameraEffectClip.LABELS[i].equals(label))
                    {
                        this.editor.editMultiple(this.clip.type, i);

                        break;
                    }
                }
            });

            for (int index : CameraEffectClip.ORDER)
            {
                panel.list.list.add(CameraEffectClip.LABELS[index]);
            }

            panel.setValue(CameraEffectClip.LABELS[this.clip.type.get()]);
            UIOverlay.addOverlay(this.getContext(), panel, 260, 0.8F);
        }), () -> this.type.label = IKey.constant(CameraEffectClip.LABELS[this.clip.type.get()] + "  \u25BE"));

        this.strength = this.trackpad(this.clip.strength);
        this.strength.tooltip(IKey.constant("Overall strength of the effect"));
        this.size = this.trackpad(this.clip.size);
        this.size.tooltip(IKey.constant("Vignette reach / scanline density / border thickness / glitch amount"));
        this.speed = this.trackpad(this.clip.speed);
        this.speed.tooltip(IKey.constant("Pulse / wobble speed of the Minecraft effects and the vignette heartbeat"));
        this.customColor = this.toggle(IKey.constant("Custom colour"), this.clip.customColor);
        this.color = this.color(this.clip.color);
        this.seed = this.trackpad(this.clip.seed);
        this.seed.tooltip(IKey.constant("Glitch pattern seed; also flips the tilt direction of Hurt"));

        this.pulses = this.trackpad(this.clip.pulses);
        this.pulses.tooltip(IKey.constant("Vignette type: heartbeat pulses over the clip (0 = steady)"));

        this.blinks = this.trackpad(this.clip.blinks);
        this.blinks.tooltip(IKey.constant("How many blinks fit in the clip (Blink type)"));
        this.closed = this.trackpad(this.clip.closed);
        this.closed.tooltip(IKey.constant("How far the eyelids close (1 = fully shut)"));
        this.soft = this.trackpad(this.clip.soft);
        this.soft.tooltip(IKey.constant("Softness of the eyelid edge"));
        this.curve = this.trackpad(this.clip.curve);
        this.curve.tooltip(IKey.constant("How much the lids arch (0 = straight bars, like a real eye above 0.5)"));
        this.dip = this.trackpad(this.clip.dip);
        this.dip.tooltip(IKey.constant("Head dips down by this many degrees while the eyes close"));

        this.shape = this.bind(new UICirculate((b) -> this.clip.shape.set(b.getValue())), () -> this.shape.setValue(this.clip.shape.get()));

        for (String label : CameraEffectClip.SHAPES)
        {
            this.shape.addLabel(IKey.constant(label));
        }

        this.fov = this.trackpad(this.clip.fov);
        this.fov.tooltip(IKey.constant("Punch type: FOV change (negative zooms in)"));
        this.roll = this.trackpad(this.clip.roll);
        this.roll.tooltip(IKey.constant("Punch type: camera tilt (dutch angle)"));
        this.pitch = this.trackpad(this.clip.pitch);
        this.yaw = this.trackpad(this.clip.yaw);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(UIKeys.C_CLIP.get("bbs:camera_effect"), this.type, this.strength, this.size, this.speed, this.customColor, this.color, this.seed, this.pulses));
        this.panels.add(this.section(IKey.constant("Eyelids (Blink / Wake up / Fall asleep / Squint)"), this.blinks, this.closed, this.soft, this.curve, this.dip));
        this.panels.add(this.section(IKey.constant("Punch (camera tilt / FOV)"), this.shape, this.fov, this.roll, this.pitch, this.yaw));
    }
}
