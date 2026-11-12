package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.TransitionClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;

public class UITransitionClip extends UIClip<TransitionClip>
{
    private UICirculate type;
    private UITrackpad intensity;
    private UIColor color;

    public UITransitionClip(TransitionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.type = this.bind(new UICirculate((b) -> this.clip.type.set(b.getValue())), () -> this.type.setValue(this.clip.type.get()));

        for (String label : TransitionClip.LABELS)
        {
            this.type.addLabel(IKey.constant(label));
        }

        this.intensity = this.trackpad(this.clip.intensity);
        this.intensity.tooltip(IKey.constant("Intensity: how opaque the colour gets at most"));
        this.color = this.color(this.clip.color);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(UIKeys.C_CLIP.get("bbs:transition"), this.type, this.intensity, this.color));
    }
}
