package bbsfx.client;

import bbsfx.LangKeys;
import bbsfx.client.lyrics.ActorLayer;
import bbsfx.client.lyrics.DeepGlow;
import bbsfx.client.lyrics.Lyrics3DRenderer;
import bbsfx.client.lyrics.TextFlashRenderer;
import bbsfx.client.ui.UILyrics3DClip;
import bbsfx.client.ui.UILyricsClip;
import bbsfx.client.ui.UITextFlashClip;
import bbsfx.lyrics.LyricsClip;
import bbsfx.text.Lyrics3DClip;
import bbsfx.text.TextFlashClip;
import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.api.client.events.RegisterClipPanelsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFrameOverlaysEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class BBSFxClientAddon implements BBSAddonMod
{
    @Subscribe
    public void registerFrameOverlays(RegisterFrameOverlaysEvent event)
    {
        event.register(ActorLayer::prepareOverlay);
        event.register(TextFlashRenderer::render);
        event.register(ActorLayer::renderOverlay);
    }

    @Subscribe
    public void registerL10n(RegisterL10nEvent event)
    {
        event.l10n.registerOne((lang) -> new Link("bbsfx", "strings/" + lang + ".json"));
    }

    @Subscribe
    public void registerClipPanels(RegisterClipPanelsEvent event)
    {
        event.register(LyricsClip.class, UILyricsClip::new);
        event.register(Lyrics3DClip.class, UILyrics3DClip::new);
        event.register(TextFlashClip.class, UITextFlashClip::new);
    }

    static
    {
        LangKeys.resolver((key) -> L10n.lang(key).get());
        DeepGlow.registerShaders();
        FilmEvents.RENDER_AFTER.register(ActorLayer::capture);
        FilmEvents.RENDER_AFTER.register(Lyrics3DRenderer::render);
        FilmEvents.SHUTDOWN.register(ActorLayer::clear);
    }
}
