package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.modifiers.ProceduralShakeClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UIProceduralShakeClip extends UIClip<ProceduralShakeClip>
{
    private UIButton preset;
    private UITrackpad frequency;
    private UITrackpad octaves;
    private UITrackpad seed;
    private UITrackpad intensity;
    private UITrackpad posX, posY, posZ, rotPitch, rotYaw, rotRoll, rotFov;

    public UIProceduralShakeClip(ProceduralShakeClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.preset = this.bind(new UIButton(IKey.EMPTY, (b) -> b.getContext().replaceContextMenu((menu) ->
        {
            for (ProceduralShakeClip.Preset p : ProceduralShakeClip.Preset.values())
            {
                if (p != ProceduralShakeClip.Preset.CUSTOM)
                {
                    menu.action(Icons.EXCHANGE, IKey.constant(p.label), () -> this.applyPreset(p));
                }
            }

            menu.action(Icons.EXCHANGE, IKey.constant(ProceduralShakeClip.Preset.CUSTOM.label), () -> this.applyPreset(ProceduralShakeClip.Preset.CUSTOM));
        })), () -> this.preset.label = IKey.constant("Preset: " + ProceduralShakeClip.Preset.values()[this.clip.preset.get()].label));

        this.frequency = this.trackpad(this.clip.frequency);
        this.frequency.tooltip(IKey.constant("Frequency (Hz): how fast the camera wanders"));
        this.octaves = this.trackpad(this.clip.octaves);
        this.octaves.tooltip(IKey.constant("Octaves: more layers means rougher, more detailed shake"));
        this.seed = this.trackpad(this.clip.seed);
        this.seed.tooltip(IKey.constant("Seed: another number gives a different, equally random shake"));
        this.intensity = this.trackpad(this.clip.intensity);
        this.intensity.tooltip(IKey.constant("Intensity: multiplies every amplitude"));
        this.posX = this.trackpad(this.clip.posX);
        this.posY = this.trackpad(this.clip.posY);
        this.posZ = this.trackpad(this.clip.posZ);
        this.rotPitch = this.trackpad(this.clip.rotPitch);
        this.rotYaw = this.trackpad(this.clip.rotYaw);
        this.rotRoll = this.trackpad(this.clip.rotRoll);
        this.rotFov = this.trackpad(this.clip.rotFov);
        this.posX.tooltip(IKey.constant("Position X (blocks)"));
        this.posY.tooltip(IKey.constant("Position Y (blocks)"));
        this.posZ.tooltip(IKey.constant("Position Z (blocks)"));
        this.rotPitch.tooltip(IKey.constant("Pitch (degrees)"));
        this.rotYaw.tooltip(IKey.constant("Yaw (degrees)"));
        this.rotRoll.tooltip(IKey.constant("Roll (degrees)"));
        this.rotFov.tooltip(IKey.constant("FOV (degrees)"));
    }

    private void applyPreset(ProceduralShakeClip.Preset p)
    {
        this.clip.applyPreset(p);

        /* A blast or a hit is strong at first and dies down: let the envelope do that */
        float fadeIn = 0F;
        float fadeOut = 0F;

        switch (p)
        {
            case EXPLOSION -> fadeOut = 25F;
            case IMPACT -> fadeOut = 12F;
            case RECOIL -> fadeOut = 8F;
            case EARTHQUAKE ->
            {
                fadeIn = 10F;
                fadeOut = 15F;
            }
            default -> {}
        }

        if (fadeIn > 0F || fadeOut > 0F)
        {
            this.clip.envelope.enabled.set(true);
            this.clip.envelope.fadeIn.set(fadeIn);
            this.clip.envelope.fadeOut.set(fadeOut);
        }
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(UIKeys.C_CLIP.get("bbs:procedural_shake"),
            this.preset,
            UI.row(UIConstants.MARGIN, 0, 20, this.frequency, this.intensity),
            UI.row(UIConstants.MARGIN, 0, 20, this.octaves, this.seed),
            UI.row(UIConstants.MARGIN, 0, 20, this.posX, this.posY, this.posZ),
            UI.row(UIConstants.MARGIN, 0, 20, this.rotPitch, this.rotYaw, this.rotRoll),
            this.rotFov));
    }
}
