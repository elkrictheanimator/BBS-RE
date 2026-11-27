package mchorse.bbs_mod.forms.categories;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.UIFormList;
import mchorse.bbs_mod.ui.forms.categories.UIFormCategory;
import mchorse.bbs_mod.ui.forms.categories.UIModelFormCategory;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.resources.CemSourcePack;

public class ModelFormCategory extends FormCategory
{
    public final String folderKey;

    public ModelFormCategory(IKey title, ValueBoolean visibility)
    {
        this(title, visibility, "");
    }

    public ModelFormCategory(IKey title, ValueBoolean visibility, String folderKey)
    {
        super(title, visibility);

        this.folderKey = folderKey == null ? "" : folderKey;
        this.icon(Icons.POSE);
    }

    /** Clean folder label for Home folder cards and breadcrumbs. */
    public String getFolderDisplayName()
    {
        if (this.folderKey.isEmpty())
        {
            return StringUtils.processColoredText(UIKeys.FORMS_CATEGORIES_MODELS.get());
        }

        if (this.folderKey.equals(CemSourcePack.NAME) || this.folderKey.startsWith(CemSourcePack.NAME + "/"))
        {
            String rest = this.folderKey.substring(CemSourcePack.NAME.length());

            return StringUtils.processColoredText(UIKeys.FORMS_CATEGORIES_MODELS_PACKS.get() + rest);
        }

        return StringUtils.processColoredText(this.folderKey);
    }

    @Override
    public UIFormCategory createUI(UIFormList list)
    {
        return new UIModelFormCategory(this, list);
    }
}
