package com.bbsvfx.bbsvfx.client;

import com.bbsvfx.bbsvfx.camera.OrbitPathClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.film.clips.modules.UIPointModule;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIAnchorKeyframeFactory;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

/**
 * Editor panel for {@link OrbitPathClip}: pick the focus (an actor and/or a world point), set the
 * orbit with sliders, or animate angle / distance / pitch / height / roll / FOV with keyframes.
 */
public class UIOrbitPathClip extends UIClip<OrbitPathClip>
{
    private static final int[] COLORS = {0xFFFF5A5A, 0xFFFFA64D, 0xFF9BD34D, 0xFF4DC3D3, 0xFFB34DD3, 0xFFFFE14D};

    public UIButton selector;
    public UIPointModule offset;
    public UITrackpad turns;
    public UITrackpad startAngle;
    public UITrackpad distance;
    public UITrackpad pitch;
    public UITrackpad height;
    public UITrackpad roll;
    public UITrackpad fov;
    public UIKeyframeEditor keyframes;
    public UIButton editKeyframes;

    public UIOrbitPathClip(OrbitPathClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.selector = new UIButton(IKey.constant("Focus actor (optional)"), (b) ->
        {
            UIFilmPanel panel = this.getParent(UIFilmPanel.class);

            if (panel != null)
            {
                UIAnchorKeyframeFactory.displayActors(this.getContext(), panel.getController().getEntities(),
                    this.clip.selector.get(), (i) -> this.clip.selector.set(i));
            }
        });
        this.selector.tooltip(IKey.constant("Orbit around this actor. Leave empty to orbit the fixed point below."));

        this.offset = new UIPointModule(this.editor, IKey.constant("Focus point")).contextMenu();

        this.turns = new UITrackpad((v) -> this.clip.turns.set(v.floatValue()));
        this.turns.limit(-20F, 20F).tooltip(IKey.constant("Full turns over the clip (negative = other direction). Ignored once you keyframe the angle."));

        this.startAngle = new UITrackpad((v) -> this.clip.startAngle.set(v.floatValue()));
        this.startAngle.limit(-3600F, 3600F).tooltip(IKey.constant("Where on the circle the camera starts (degrees)"));

        this.distance = new UITrackpad((v) -> this.clip.distance.set(v.floatValue()));
        this.distance.limit(0F, 500F).tooltip(IKey.constant("Distance from the focus"));

        this.pitch = new UITrackpad((v) -> this.clip.pitch.set(v.floatValue()));
        this.pitch.limit(-89F, 89F).tooltip(IKey.constant("Camera elevation: positive = looking down from above"));

        this.height = new UITrackpad((v) -> this.clip.height.set(v.floatValue()));
        this.height.tooltip(IKey.constant("Lifts the focus point up or down"));

        this.roll = new UITrackpad((v) -> this.clip.roll.set(v.floatValue()));
        this.roll.limit(-180F, 180F).tooltip(IKey.constant("Camera roll (tilt)"));

        this.fov = new UITrackpad((v) -> this.clip.fov.set(v.floatValue()));
        this.fov.limit(1F, 170F).tooltip(IKey.constant("Field of view"));

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.rulerRenderer((context) -> UIReplaysEditor.renderRuler(
            context, this.keyframes.view, (UIClipsPanel) this.editor, (Clips) this.clip.getParent(), this.clip.tick.get()));
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("orbit_path_keyframes");

        this.editKeyframes = new UIButton(IKey.constant("Edit keyframes"), (b) ->
        {
            this.setupSheets();
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            this.keyframes.view.getGraph().clearSelection();
        });
        this.editKeyframes.tooltip(IKey.constant("Animate the orbit: a keyframed channel overrides its slider"));
    }

    private void setupSheets()
    {
        UIKeyframes view = this.keyframes.view;
        OrbitPathClip c = this.clip;
        KeyframeChannel[] channels = {c.angleKf, c.distanceKf, c.pitchKf, c.heightKf, c.rollKf, c.fovKf};
        String[] names = {"Angle", "Distance", "Pitch", "Height", "Roll", "FOV"};

        view.removeAllSheets();

        for (int i = 0; i < channels.length; i++)
        {
            view.addSheet(new UIKeyframeSheet(channels[i].getId(), IKey.constant(names[i]), COLORS[i % COLORS.length], channels[i], null));
        }
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(BbsVfxUI.section(IKey.constant("Focus"), this.selector));
        this.panels.add(this.offset.marginTop(UIConstants.SECTION_GAP));
        this.panels.add(BbsVfxUI.section(IKey.constant("Orbit"),
            UI.row(this.turns, this.startAngle),
            UI.row(this.distance, this.pitch, this.height),
            UI.row(this.roll, this.fov)));
        this.panels.add(BbsVfxUI.section(IKey.constant("Keyframes"), this.editKeyframes));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.offset.fill(this.clip.offset);
        this.turns.setValue(this.clip.turns.get());
        this.startAngle.setValue(this.clip.startAngle.get());
        this.distance.setValue(this.clip.distance.get());
        this.pitch.setValue(this.clip.pitch.get());
        this.height.setValue(this.clip.height.get());
        this.roll.setValue(this.clip.roll.get());
        this.fov.setValue(this.clip.fov.get());
    }
}
