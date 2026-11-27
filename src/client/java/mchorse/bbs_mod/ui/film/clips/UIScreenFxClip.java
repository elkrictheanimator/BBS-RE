package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.ScreenFxClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;

public class UIScreenFxClip extends UIClip<ScreenFxClip>
{
    private UICirculate mode;
    private UITrackpad strength;
    private UITrackpad size;
    private UITrackpad seed;
    private UIColor color;

    public UIScreenFxClip(ScreenFxClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.mode = this.bind(new UICirculate((b) -> this.clip.mode.set(b.getValue())), () -> this.mode.setValue(this.clip.mode.get()));

        for (String label : ScreenFxClip.LABELS)
        {
            this.mode.addLabel(IKey.constant(label));
        }

        this.strength = this.trackpad(this.clip.strength);
        this.strength.tooltip(IKey.constant("Opacity of the effect"));
        this.size = this.trackpad(this.clip.size);
        this.size.tooltip(IKey.constant("Line density / border thickness / amount of glitch bars"));
        this.seed = this.trackpad(this.clip.seed);
        this.seed.tooltip(IKey.constant("Random seed (glitch bars)"));
        this.color = this.color(this.clip.color);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(UIKeys.C_CLIP.get("bbs:screen_fx"), this.mode, this.strength, this.size, this.seed, this.color));
    }
}
