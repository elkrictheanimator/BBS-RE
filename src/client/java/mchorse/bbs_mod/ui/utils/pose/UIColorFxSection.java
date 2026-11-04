package mchorse.bbs_mod.ui.utils.pose;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.ColorFx;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The editor of a {@link ColorFx}: one folded section with every field of the effect. It owns no data —
 * every change goes to {@code editor} as an edit to apply, so the host decides what to modify (the
 * selected bones of a pose, a keyframe, ...) and how to record it; {@link #setFx} refreshes the widgets
 * from whichever effect is being shown.
 */
public class UIColorFxSection extends UISection
{
    private static final Map<String, Boolean> FOLDS = new HashMap<>();

    private final Consumer<Consumer<ColorFx>> editor;
    private ColorFx shown = new ColorFx();

    public final UISliderTrackpad strength;
    public final UIColor color2;
    public final UICirculate style;
    public final UICirculate mode;
    public final UICirculate axis;
    public final UICirculate space;
    public final UITrackpad offset;
    public final UITrackpad length;
    public final UITrackpad speed;
    public final UITrackpad cycles;
    public final UIButton image;
    public final UITrackpad frames;
    public final UITrackpad fps;
    public final UITrackpad tiling;

    public UIColorFxSection(IKey title, String foldKey, Consumer<Consumer<ColorFx>> editor)
    {
        super(title);

        this.editor = editor;
        this.remember(FOLDS, foldKey, false);

        this.strength = new UISliderTrackpad((v) -> this.edit((fx) -> fx.strength = v.floatValue()));
        this.strength.limit(0D, 1D);
        this.strength.tooltip(IKey.raw("0 = effect off, 1 = the effect fully replaces the colour at the far end"));

        this.color2 = new UIColor((c) -> this.edit((fx) -> fx.color2.set(c)));
        this.color2.tooltip(IKey.raw("Second colour. Rainbow and image are tinted by it (white = untouched)"));

        this.style = cycle((i) -> this.edit((fx) -> fx.style = i), "Solid", "Rainbow", "Neon", "Image");
        this.style.tooltip(IKey.raw("Solid: Color 2. Rainbow: hue runs along the gradient and scrolls. Neon: pulsing glow that ignores lighting. Image: an animated picture"));

        this.mode = cycle((i) -> this.edit((fx) -> fx.mode = i), "Color", "Overlay");
        this.mode.tooltip(IKey.raw("Color: tints the texture like the Color field. Overlay: washes it like the Overlay field"));

        this.axis = cycle((i) -> this.edit((fx) -> fx.axis = i), "X", "Y", "Z");
        this.space = cycle((i) -> this.edit((fx) -> fx.space = i), "Bone", "Model");
        this.space.tooltip(IKey.raw("Bone: measured from the bone's pivot. Model: from the model's origin, so bones sharing it form one continuous gradient"));

        this.offset = new UITrackpad((v) -> this.edit((fx) -> fx.offset = v.floatValue()));
        this.offset.tooltip(IKey.raw("Slides the start of the gradient along the axis, in model units. Keyframe it to sweep the colour in from the bottom or top"));

        this.length = new UITrackpad((v) -> this.edit((fx) -> fx.length = v.floatValue()));
        this.length.limit(-512D, 512D);
        this.length.tooltip(IKey.raw("How far the colour takes to fade in, in model units. Small = hard edge. Negative flips the direction"));

        this.speed = new UITrackpad((v) -> this.edit((fx) -> fx.speed = v.floatValue()));
        this.speed.tooltip(IKey.raw("Animation speed, cycles per second: rainbow scroll, neon pulse, image scroll"));

        this.cycles = new UITrackpad((v) -> this.edit((fx) -> fx.cycles = v.floatValue()));
        this.cycles.limit(0D, 64D);
        this.cycles.tooltip(IKey.raw("Rainbow: how many full colour cycles fit in one gradient length"));

        this.image = new UIButton(IKey.raw("Pick image"), (b) ->
        {
            UITexturePicker.open(this.getContext(), this.shown.image.isEmpty() ? null : Link.create(this.shown.image),
                (link) -> this.edit((fx) -> fx.image = link == null ? "" : link.toString()));
        });
        this.image.tooltip(IKey.raw("The picture laid over the model's texture layout. Several frames can be stacked top to bottom"));

        this.frames = new UITrackpad((v) -> this.edit((fx) -> fx.frames = Math.max(1, v.intValue())));
        this.frames.limit(1D, 256D, true);
        this.frames.tooltip(IKey.raw("Frames stacked top to bottom in the picture (1 = a still image)"));

        this.fps = new UITrackpad((v) -> this.edit((fx) -> fx.fps = v.floatValue()));
        this.fps.limit(0D, 120D);

        this.tiling = new UITrackpad((v) -> this.edit((fx) -> fx.tiling = v.floatValue()));
        this.tiling.tooltip(IKey.raw("How many times the picture repeats over the texture"));

        this.fields.add(
            UI.labelRow(IKey.raw("Strength"), this.strength),
            UI.labelRow(IKey.raw("Color 2"), this.color2),
            UI.labelRow(IKey.raw("Style"), this.style),
            UI.labelRow(IKey.raw("Mode"), this.mode),
            UI.labelRow(IKey.raw("Axis"), this.axis),
            UI.labelRow(IKey.raw("Space"), this.space),
            UI.labelRow(IKey.raw("Offset"), this.offset),
            UI.labelRow(IKey.raw("Length"), this.length),
            UI.labelRow(IKey.raw("Speed"), this.speed),
            UI.labelRow(IKey.raw("Rainbow cycles"), this.cycles),
            this.image,
            UI.labelRow(IKey.raw("Frames"), this.frames),
            UI.labelRow(IKey.raw("Frame rate"), this.fps),
            UI.labelRow(IKey.raw("Tiling"), this.tiling)
        );

        this.setFx(null);
    }

    private static UICirculate cycle(Consumer<Integer> callback, String... labels)
    {
        UICirculate circulate = new UICirculate((b) -> callback.accept(b.getValue()));

        for (String label : labels)
        {
            circulate.addLabel(IKey.raw(label));
        }

        return circulate;
    }

    private void edit(Consumer<ColorFx> edit)
    {
        this.editor.accept(edit);
    }

    /** Show an effect in the widgets (null = a fresh, switched-off one). Does not fire any edit. */
    public void setFx(ColorFx fx)
    {
        ColorFx value = fx == null ? new ColorFx() : fx;

        this.shown = value;

        this.strength.setValue(value.strength);
        this.color2.setColor(value.color2.getARGBColor());
        this.style.setValue(value.style);
        this.mode.setValue(value.mode);
        this.axis.setValue(value.axis);
        this.space.setValue(value.space);
        this.offset.setValue(value.offset);
        this.length.setValue(value.length);
        this.speed.setValue(value.speed);
        this.cycles.setValue(value.cycles);
        this.frames.setValue(value.frames);
        this.fps.setValue(value.fps);
        this.tiling.setValue(value.tiling);
    }
}
