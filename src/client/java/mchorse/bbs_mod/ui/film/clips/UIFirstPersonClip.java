package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.FirstPersonClientClip;
import mchorse.bbs_mod.camera.clips.misc.FirstPersonClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.clips.modules.UIPointModule;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIAnchorKeyframeFactory;

public class UIFirstPersonClip extends UIClip<FirstPersonClientClip>
{
    private static final IKey[] MODES = {
        IKey.constant("View: first person (eyes)"),
        IKey.constant("View: third person (behind)"),
        IKey.constant("View: third person (in front)")
    };

    public UIButton selector;
    public UIButton mode;
    public UITrackpad fov;
    public UIPointModule offset;
    public UIToggle showHand;
    public UIToggle showHud;

    public UIFirstPersonClip(FirstPersonClientClip clip, IUIClipsDelegate editor)
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

        this.mode = new UIButton(MODES[0], (b) ->
        {
            int next = (this.clip.mode.get() + 1) % MODES.length;

            this.editor.editMultiple(this.clip.mode, (v) -> v.set(next));
            b.label = MODES[next];
        });
        this.bind(this.mode, () -> this.mode.label = MODES[Math.max(0, Math.min(MODES.length - 1, this.clip.mode.get()))]);

        this.offset = this.bind(new UIPointModule(this.editor, UIKeys.CAMERA_PANELS_OFFSET).contextMenu(), () -> this.offset.fill(this.clip.offset));
        this.fov = this.trackpad(this.clip.fov);
        this.fov.tooltip(UIKeys.CAMERA_PANELS_FOV);
        this.showHand = this.toggle(IKey.constant("Show first-person hand"), this.clip.showHand);
        this.showHud = this.toggle(IKey.constant("Show HUD (hotbar, crosshair)"), this.clip.showHud);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(UIKeys.CAMERA_PANELS_TARGET, this.selector, this.mode));
        this.panels.add(this.offset);
        this.panels.add(this.fov);
        this.panels.add(this.showHand);
        this.panels.add(this.showHud);
    }
}
