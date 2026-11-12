package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.LetterboxClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;

public class UILetterboxClip extends UIClip<LetterboxClip>
{
    private UICirculate preset;
    private UITrackpad ratio;
    private UIColor color;

    public UILetterboxClip(LetterboxClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.preset = this.bind(new UICirculate((b) -> this.clip.preset.set(b.getValue())), () -> this.preset.setValue(this.clip.preset.get()));

        for (String label : LetterboxClip.LABELS)
        {
            this.preset.addLabel(IKey.constant(label));
        }

        this.ratio = this.trackpad(this.clip.customRatio);
        this.ratio.tooltip(IKey.constant("Custom ratio (width / height), used by the Custom preset"));
        this.color = this.color(this.clip.color);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(UIKeys.C_CLIP.get("bbs:letterbox"), this.preset, this.ratio, this.color));
    }
}
