package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.modifiers.OrbitTrackClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.BezierEase;

public class UIOrbitTrackClip extends UIClip<OrbitTrackClip>
{
    private UIButton preset;
    private UIButton ease;
    private UITrackpad angle, distance, height, zoom, x1, y1, x2, y2;

    public UIOrbitTrackClip(OrbitTrackClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.preset = this.bind(new UIButton(IKey.constant("Presets: orbit, helix, push in..."), (b) -> b.getContext().replaceContextMenu((menu) ->
        {
            menu.action(Icons.GLOBE, IKey.constant("Orbit left 90°"), () -> this.apply(-90F, 0F, 0F));
            menu.action(Icons.GLOBE, IKey.constant("Orbit right 90°"), () -> this.apply(90F, 0F, 0F));
            menu.action(Icons.GLOBE, IKey.constant("Half orbit 180°"), () -> this.apply(180F, 0F, 0F));
            menu.action(Icons.GLOBE, IKey.constant("Full orbit 360°"), () -> this.apply(360F, 0F, 0F));
            menu.action(Icons.GLOBE, IKey.constant("Helix rise"), () -> this.apply(270F, 4F, 0F));
            menu.action(Icons.GLOBE, IKey.constant("Orbit + push in"), () -> this.apply(120F, 0F, -25F));
        })), () -> {});

        this.ease = this.bind(new UIButton(IKey.constant("Easing: curve presets"), (b) -> b.getContext().replaceContextMenu((menu) ->
        {
            for (int i = 0; i < BezierEase.LABELS.length; i++)
            {
                int index = i;

                menu.action(Icons.GRAPH, IKey.constant(BezierEase.LABELS[i]), () -> this.clip.setEase(BezierEase.PRESETS[index]));
            }
        })), () -> {});

        this.angle = this.trackpad(this.clip.angle);
        this.angle.tooltip(IKey.constant("Sweep angle in degrees (positive = orbit right)"));
        this.distance = this.trackpad(this.clip.distance);
        this.distance.tooltip(IKey.constant("Orbit radius: distance to the subject in blocks; the subject is straight ahead of the camera"));
        this.height = this.trackpad(this.clip.height);
        this.height.tooltip(IKey.constant("Rise in blocks (a helix together with the orbit)"));
        this.zoom = this.trackpad(this.clip.zoom);
        this.zoom.tooltip(IKey.constant("Zoom in FOV degrees (negative = push in)"));
        this.x1 = this.trackpad(this.clip.easeX1);
        this.y1 = this.trackpad(this.clip.easeY1);
        this.x2 = this.trackpad(this.clip.easeX2);
        this.y2 = this.trackpad(this.clip.easeY2);
    }

    private void apply(float angle, float height, float zoom)
    {
        this.clip.angle.set(angle);
        this.clip.height.set(height);
        this.clip.zoom.set(zoom);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(IKey.constant("Orbit track"),
            this.preset,
            this.angle, this.distance,
            UI.row(UIConstants.MARGIN, 0, 20, this.height, this.zoom),
            this.ease,
            UI.row(UIConstants.MARGIN, 0, 20, this.x1, this.y1),
            UI.row(UIConstants.MARGIN, 0, 20, this.x2, this.y2)));
    }
}
