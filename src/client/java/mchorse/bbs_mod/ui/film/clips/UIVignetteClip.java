package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.VignetteClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;

public class UIVignetteClip extends UIClip<VignetteClip>
{
    private UITrackpad strength;
    private UITrackpad size;
    private UIColor color;
    private UITrackpad pulses;

    public UIVignetteClip(VignetteClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.strength = this.trackpad(this.clip.strength);
        this.strength.tooltip(IKey.constant("How dark the edges get"));
        this.size = this.trackpad(this.clip.size);
        this.size.tooltip(IKey.constant("How far the vignette reaches into the frame"));
        this.color = this.color(this.clip.color);
        this.pulses = this.trackpad(this.clip.pulses);
        this.pulses.tooltip(IKey.constant("Heartbeat: pulses over the clip (0 = steady)"));
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(UIKeys.C_CLIP.get("bbs:vignette"), this.strength, this.size, this.color, this.pulses));
    }
}
