package mchorse.bbs_mod.ui.utils.context;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.Keybind;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Blender's F3: one list of every shortcut that can run right now, with a search box over it.
 *
 * <p>Type part of a name and press Enter to run the command; the key it is bound to stands next to
 * its name, so the list doubles as a way to learn the shortcuts. It is built from the shortcuts
 * of the elements that are on screen at that moment, so it follows whatever editor is open
 * and costs the editors nothing: a command is whatever already has a shortcut.</p>
 */
public class CommandSearch
{
    public static void open(UIContext context)
    {
        List<Keybind> found = new ArrayList<>();

        context.menu.getRoot().collectKeybinds(found);

        /* One entry per name and key: the same command is often registered by several elements. */
        Map<String, Keybind> unique = new LinkedHashMap<>();

        for (Keybind keybind : found)
        {
            if (keybind.getCombo() == Keys.COMMAND_SEARCH || keybind.getCombo() == Keys.COMMAND_SEARCH_ALT)
            {
                continue;
            }

            IKey label = keybind.getLabel();
            String name = label == null ? null : label.get();

            if (name != null && !name.isEmpty())
            {
                unique.putIfAbsent(name + "|" + keybind.getKeyCombo(), keybind);
            }
        }

        if (unique.isEmpty())
        {
            return;
        }

        List<Keybind> sorted = new ArrayList<>(unique.values());

        sorted.sort(Comparator.comparing(CommandSearch::category).thenComparing((keybind) -> keybind.getLabel().get()));

        context.replaceContextMenu((menu) ->
        {
            for (Keybind keybind : sorted)
            {
                menu.action(Icons.SEARCH, IKey.constant(text(keybind)), () -> run(context, keybind));
            }
        });
    }

    private static String category(Keybind keybind)
    {
        IKey category = keybind.getCategory();
        String text = category == null ? "" : category.get();

        return text == null ? "" : text;
    }

    private static String text(Keybind keybind)
    {
        String category = category(keybind);
        String keys = keybind.getKeyCombo();
        StringBuilder text = new StringBuilder();

        if (!category.isEmpty())
        {
            text.append(category).append(" > ");
        }

        text.append(keybind.getLabel().get());

        if (keys != null && !keys.isEmpty())
        {
            text.append("   [").append(keys).append("]");
        }

        return text.toString();
    }

    /** Commands were written for a key press over their editor; a stray one must not take the client down. */
    private static void run(UIContext context, Keybind keybind)
    {
        try
        {
            keybind.callback.run();
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            context.notifyError(UIKeys.COMMAND_SEARCH_FAILED);
        }
    }
}
