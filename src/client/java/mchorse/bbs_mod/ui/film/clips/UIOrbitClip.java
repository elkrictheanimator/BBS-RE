package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.modifiers.OrbitClip;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.clips.modules.UIPointModule;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIAnchorKeyframeFactory;import mchorse.bbs_mod.ui.utils.UI;

public class UIOrbitClip extends UIClip<OrbitClip>
{
    public UIButton selector;
    public UIToggle absolute;
    public UIToggle copy;
    public UITrackpad yaw;
    public UITrackpad pitch;
    public UIPointModule offset;
    public UITrackpad distance;
    public UIButton angles;

    public UIOrbitClip(OrbitClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.selector = new UIButton(UIKeys.CAMERA_PANELS_TARGET_TITLE, (b) ->
        {
            UIFilmPanel panel = this.getParent(UIFilmPanel.class);

            if (panel != null)
            {
                UIAnchorKeyframeFactory.displayActors(this.getContext(), panel.getController().getEntities(), this.clip.selector.get(), (i) -> this.clip.selector.set(i));
            }
        });
        this.selector.tooltip(UIKeys.CAMERA_PANELS_TARGET_TOOLTIP);

        this.absolute = this.toggle(UIKeys.CAMERA_PANELS_ABSOLUTE, this.clip.absolute);
        this.copy = this.toggle(UIKeys.CAMERA_PANELS_COPY_ENTITY, this.clip.copy);
        this.copy.tooltip(UIKeys.CAMERA_PANELS_COPY_ENTITY_TOOLTIP);

        this.yaw = this.trackpad(this.clip.yaw);
        this.yaw.tooltip(UIKeys.CAMERA_PANELS_YAW);

        this.pitch = this.trackpad(this.clip.pitch);
        this.pitch.tooltip(UIKeys.CAMERA_PANELS_PITCH);

        this.offset = this.bind(new UIPointModule(editor, UIKeys.CAMERA_PANELS_OFFSET).contextMenu(), () -> this.offset.fill(this.clip.offset));
        this.distance = this.trackpad(this.clip.distance);

        this.angles = new UIButton(IKey.constant("Camera angles: close-up, wide, low, high..."), (b) -> b.getContext().replaceContextMenu((menu) ->
        {
            menu.action(Icons.CAMERA, IKey.constant("Close-up (face)"), () -> this.shot(0F, 0F, 1.6F, 1.6F));
            menu.action(Icons.CAMERA, IKey.constant("Medium shot"), () -> this.shot(0F, 0F, 3.5F, 1.3F));
            menu.action(Icons.CAMERA, IKey.constant("Wide shot"), () -> this.shot(0F, 0F, 10F, 1F));
            menu.action(Icons.CAMERA, IKey.constant("Over the shoulder (left)"), () -> this.shot(155F, 6F, 2.6F, 1.6F));
            menu.action(Icons.CAMERA, IKey.constant("Over the shoulder (right)"), () -> this.shot(205F, 6F, 2.6F, 1.6F));
            menu.action(Icons.CAMERA, IKey.constant("Low angle (hero)"), () -> this.shot(0F, 22F, 4F, 0.6F));
            menu.action(Icons.CAMERA, IKey.constant("High angle (small)"), () -> this.shot(0F, -35F, 4F, 1.2F));
            menu.action(Icons.CAMERA, IKey.constant("Bird's eye"), () -> this.shot(0F, -80F, 8F, 1F));
            menu.action(Icons.CAMERA, IKey.constant("Profile (side)"), () -> this.shot(90F, 0F, 3.5F, 1.3F));
            menu.action(Icons.CAMERA, IKey.constant("From behind"), () -> this.shot(180F, 4F, 3.5F, 1.4F));
        }));
        this.angles.tooltip(IKey.constant("Fills yaw, pitch, distance and height around the actor, following the way it faces"));
    }

    private void shot(float yaw, float pitch, float distance, float height)
    {
        this.clip.copy.set(true);
        this.clip.absolute.set(false);
        this.clip.yaw.set(yaw);
        this.clip.pitch.set(pitch);
        this.clip.distance.set(distance);
        this.clip.offset.set(new mchorse.bbs_mod.camera.data.Point(0, height, 0));
        this.fillData();
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(UIKeys.CAMERA_PANELS_TARGET, this.selector, this.absolute, this.copy, this.angles));
        this.panels.add(this.section(UIKeys.CAMERA_PANELS_DISTANCE, this.distance));
        this.panels.add(this.section(UIKeys.CAMERA_PANELS_ANGLE, UI.row(5, 0, 20, this.yaw, this.pitch)));
        this.panels.add(this.offset);
    }
}