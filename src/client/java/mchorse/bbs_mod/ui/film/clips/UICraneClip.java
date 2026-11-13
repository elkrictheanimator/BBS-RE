package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.modifiers.CraneClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.BezierEase;

public class UICraneClip extends UIClip<CraneClip>
{
    private UIButton preset;
    private UIButton ease;
    private UITrackpad height, swing, tilt, zoom, x1, y1, x2, y2;

    public UICraneClip(CraneClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.preset = this.bind(new UIButton(IKey.constant("Presets: rise, arc, reveal..."), (b) -> b.getContext().replaceContextMenu((menu) ->
        {
            menu.action(Icons.UPLOAD, IKey.constant("Rise"), () -> this.apply(6F, 0F, 0F, 0F));
            menu.action(Icons.DOWNLOAD, IKey.constant("Descend"), () -> this.apply(-6F, 0F, 0F, 0F));
            menu.action(Icons.ARROW_LEFT, IKey.constant("Arc left"), () -> this.apply(0F, -8F, 0F, 0F));
            menu.action(Icons.ARROW_RIGHT, IKey.constant("Arc right"), () -> this.apply(0F, 8F, 0F, 0F));
            menu.action(Icons.UPLOAD, IKey.constant("Reveal (rise + tilt down)"), () -> this.apply(8F, 0F, 20F, 0F));
            menu.action(Icons.UPLOAD, IKey.constant("Rise + push in"), () -> this.apply(6F, 0F, 0F, -20F));
            menu.action(Icons.DOWNLOAD, IKey.constant("Descend + pull out"), () -> this.apply(-6F, 0F, 0F, 20F));
            menu.action(Icons.UPLOAD, IKey.constant("Wide reveal"), () -> this.apply(10F, 6F, 25F, 15F));
            menu.action(Icons.UPLOAD, IKey.constant("Hero push-in"), () -> this.apply(-1.5F, 0F, -8F, -30F));
        })), () -> {});

        this.ease = this.bind(new UIButton(IKey.constant("Easing: curve presets"), (b) -> b.getContext().replaceContextMenu((menu) ->
        {
            for (int i = 0; i < BezierEase.LABELS.length; i++)
            {
                int index = i;

                menu.action(Icons.GRAPH, IKey.constant(BezierEase.LABELS[i]), () -> this.clip.setEase(BezierEase.PRESETS[index]));
            }
        })), () -> {});

        this.height = this.trackpad(this.clip.height);
        this.height.tooltip(IKey.constant("Vertical travel in blocks (negative = descend)"));
        this.swing = this.trackpad(this.clip.swing);
        this.swing.tooltip(IKey.constant("Sideways travel in blocks (positive = to the camera's right)"));
        this.tilt = this.trackpad(this.clip.tilt);
        this.tilt.tooltip(IKey.constant("Pitch tilt in degrees (positive = look down)"));
        this.zoom = this.trackpad(this.clip.zoom);
        this.zoom.tooltip(IKey.constant("Zoom in FOV degrees (negative = push in)"));
        this.x1 = this.trackpad(this.clip.easeX1);
        this.y1 = this.trackpad(this.clip.easeY1);
        this.x2 = this.trackpad(this.clip.easeX2);
        this.y2 = this.trackpad(this.clip.easeY2);
    }

    private void apply(float height, float swing, float tilt, float zoom)
    {
        this.clip.height.set(height);
        this.clip.swing.set(swing);
        this.clip.tilt.set(tilt);
        this.clip.zoom.set(zoom);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(IKey.constant("Crane"),
            this.preset,
            UI.row(UIConstants.MARGIN, 0, 20, this.height, this.swing),
            UI.row(UIConstants.MARGIN, 0, 20, this.tilt, this.zoom),
            this.ease,
            UI.row(UIConstants.MARGIN, 0, 20, this.x1, this.y1),
            UI.row(UIConstants.MARGIN, 0, 20, this.x2, this.y2)));
    }
}
