package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.modifiers.PunchClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;

public class UIPunchClip extends UIClip<PunchClip>
{
    private UICirculate shape;
    private UITrackpad fov;
    private UITrackpad roll;
    private UITrackpad pitch;
    private UITrackpad yaw;

    public UIPunchClip(PunchClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.shape = this.bind(new UICirculate((b) -> this.clip.shape.set(b.getValue())), () -> this.shape.setValue(this.clip.shape.get()));

        for (String label : PunchClip.SHAPES)
        {
            this.shape.addLabel(IKey.constant(label));
        }

        this.fov = this.trackpad(this.clip.fov);
        this.fov.tooltip(IKey.constant("FOV change (negative zooms in)"));
        this.roll = this.trackpad(this.clip.roll);
        this.roll.tooltip(IKey.constant("Camera tilt (dutch angle)"));
        this.pitch = this.trackpad(this.clip.pitch);
        this.yaw = this.trackpad(this.clip.yaw);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(UIKeys.C_CLIP.get("bbs:punch"), this.shape, this.fov, this.roll, this.pitch, this.yaw));
    }
}
