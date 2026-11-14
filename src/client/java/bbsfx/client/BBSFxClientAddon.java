package bbsfx.client;

import bbsfx.LangKeys;
import bbsfx.client.forms.renderers.FilmClock;
import bbsfx.client.forms.renderers.FxFormRenderer;
import bbsfx.client.forms.renderers.ImageFormRenderer;
import bbsfx.client.forms.renderers.TypoFormRenderer;
import bbsfx.client.lyrics.ActorLayer;
import bbsfx.client.lyrics.DeepGlow;
import bbsfx.client.lyrics.Lyrics3DRenderer;
import bbsfx.client.lyrics.TextFlashRenderer;
import bbsfx.client.ui.BBSFxKeys;
import bbsfx.client.ui.UIFxForm;
import bbsfx.client.ui.UIImageForm;
import bbsfx.client.ui.UITypoForm;
import bbsfx.client.ui.UILyrics3DClip;
import bbsfx.client.ui.UILyricsClip;
import bbsfx.client.ui.UITextFlashClip;
import bbsfx.forms.FxForm;
import bbsfx.forms.ImageForm;
import bbsfx.forms.TypoForm;
import bbsfx.lyrics.LyricsClip;
import bbsfx.text.Lyrics3DClip;
import bbsfx.text.TextFlashClip;
import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.api.client.events.RegisterClipPanelsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormEditorsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormRenderersEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormSectionsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFrameOverlaysEvent;
import mchorse.bbs_mod.api.client.events.RegisterTrackStylesEvent;
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
    public void registerTrackStyles(RegisterTrackStylesEvent event) {
        event.registerLabel("text", BBSFxKeys.TRACK_TEXT);
        event.registerLabel("uppercase", BBSFxKeys.TRACK_UPPERCASE);
        event.registerLabel("fontSize", BBSFxKeys.TRACK_FONT_SIZE);
        event.registerLabel("tracking", BBSFxKeys.TRACK_TRACKING);
        event.registerLabel("lineHeight", BBSFxKeys.TRACK_LINE_HEIGHT);
        event.registerLabel("align", BBSFxKeys.TRACK_ALIGN);
        event.registerLabel("wrap", BBSFxKeys.TRACK_WRAP);
        event.registerLabel("color", BBSFxKeys.TRACK_COLOR);
        event.registerLabel("outlineColor", BBSFxKeys.TRACK_OUTLINE_COLOR);
        event.registerLabel("outlineWidth", BBSFxKeys.TRACK_OUTLINE_WIDTH);
        event.registerLabel("shadowColor", BBSFxKeys.TRACK_SHADOW_COLOR);
        event.registerLabel("shadowX", BBSFxKeys.TRACK_SHADOW_X);
        event.registerLabel("shadowY", BBSFxKeys.TRACK_SHADOW_Y);
        event.registerLabel("background", BBSFxKeys.TRACK_BACKGROUND);
        event.registerLabel("padding", BBSFxKeys.TRACK_PADDING);
        event.registerLabel("billboard", BBSFxKeys.TRACK_BILLBOARD);
        event.registerLabel("animMode", BBSFxKeys.TRACK_ANIM_MODE);
        event.registerLabel("animProgress", BBSFxKeys.TRACK_ANIM_PROGRESS);
        event.registerLabel("animStagger", BBSFxKeys.TRACK_ANIM_STAGGER);
        event.registerLabel("animAmount", BBSFxKeys.TRACK_ANIM_AMOUNT);
        event.registerLabel("animLoop", BBSFxKeys.TRACK_ANIM_LOOP);
        event.registerLabel("animSpeed", BBSFxKeys.TRACK_ANIM_SPEED);
        event.registerLabel("animOrder", BBSFxKeys.AE_ORDER);
        event.registerLabel("animSeed", BBSFxKeys.AE_SEED);
        event.registerLabel("timeRemap", BBSFxKeys.AE_TIME_ENABLE);
        event.registerLabel("timeSpeed", BBSFxKeys.AE_TIME_SPEED);
        event.registerLabel("timeOffset", BBSFxKeys.AE_TIME_OFFSET);
        event.registerLabel("timeDuration", BBSFxKeys.AE_TIME_DURATION);
        event.registerLabel("timeMode", BBSFxKeys.AE_TIME);
        event.registerLabel("easeX1", BBSFxKeys.TRACK_EASE_X1);
        event.registerLabel("easeY1", BBSFxKeys.TRACK_EASE_Y1);
        event.registerLabel("easeX2", BBSFxKeys.TRACK_EASE_X2);
        event.registerLabel("easeY2", BBSFxKeys.TRACK_EASE_Y2);
    }

    @Subscribe
    public void registerFormRenderers(RegisterFormRenderersEvent event)
    {
        event.register(TypoForm.class, TypoFormRenderer::new);
        event.register(FxForm.class, FxFormRenderer::new);
        event.register(ImageForm.class, ImageFormRenderer::new);
    }

    @Subscribe
    public void registerFormEditors(RegisterFormEditorsEvent event)
    {
        event.register(TypoForm.class, UITypoForm::new);
        event.register(FxForm.class, UIFxForm::new);
        event.register(ImageForm.class, UIImageForm::new);
    }

    @Subscribe
    public void registerFormSections(RegisterFormSectionsEvent event)
    {
        event.register(BBSFxFormSection::new);
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
        FilmEvents.TICK_BEFORE.register(FilmClock::capture);
        FilmEvents.RENDER_AFTER.register(ActorLayer::capture);
        FilmEvents.RENDER_AFTER.register(Lyrics3DRenderer::render);
        FilmEvents.SHUTDOWN.register(FilmClock::clear);
        FilmEvents.SHUTDOWN.register(ActorLayer::clear);
    }
}
