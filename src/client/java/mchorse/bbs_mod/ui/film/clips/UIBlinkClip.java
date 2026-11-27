package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.BlinkClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;

public class UIBlinkClip extends UIClip<BlinkClip>
{
    private UICirculate mode;
    private UITrackpad blinks;
    private UITrackpad closed;
    private UITrackpad soft;
    private UIColor color;

    public UIBlinkClip(BlinkClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.mode = this.bind(new UICirculate((b) -> this.clip.mode.set(b.getValue())), () -> this.mode.setValue(this.clip.mode.get()));

        for (String label : BlinkClip.LABELS)
        {
            this.mode.addLabel(IKey.constant(label));
        }

        this.blinks = this.trackpad(this.clip.blinks);
        this.blinks.tooltip(IKey.constant("How many blinks fit in the clip (Blink mode)"));
        this.closed = this.trackpad(this.clip.closed);
        this.closed.tooltip(IKey.constant("How far the eyelids close (1 = fully shut)"));
        this.soft = this.trackpad(this.clip.soft);
        this.soft.tooltip(IKey.constant("Softness of the eyelid edge"));
        this.color = this.color(this.clip.color);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(UIKeys.C_CLIP.get("bbs:blink"), this.mode, this.blinks, this.closed, this.soft, this.color));
    }
}
