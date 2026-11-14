package bbsfx;

import bbsfx.lyrics.LyricsClip;
import bbsfx.text.Lyrics3DClip;
import bbsfx.text.TextFlashClip;
import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterCameraClipsEvent;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * Typography clips ported from the BBS FX Toolkit by ZCode (MIT, see assets/bbsfx/LICENSE_bbsfx):
 * Text Flash, Lyrics and 3D Lyrics. The Toolkit's orbit / crane clips live in RE as bbs:orbit_track / bbs:crane.
 */
public class BBSFxAddon implements BBSAddonMod
{
    @Subscribe
    public void registerSourcePacks(RegisterSourcePacksEvent event)
    {
        event.registerAddon("bbsfx", BBSFxAddon.class);
    }

    @Subscribe
    public void registerCameraClips(RegisterCameraClipsEvent event)
    {
        event.factory
            .register(new Link("bbsfx", "lyrics"), LyricsClip.class, new ClipFactoryData(Icons.SHAPES, 14241690))
            .register(new Link("bbsfx", "text3d"), Lyrics3DClip.class, new ClipFactoryData(Icons.SHAPES, 4827097))
            .register(new Link("bbsfx", "textflash"), TextFlashClip.class, new ClipFactoryData(Icons.FADING, 14725434));
    }
}
