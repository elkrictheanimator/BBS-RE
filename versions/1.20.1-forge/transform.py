"""Forge build: small textual edits on top of the layered tree."""
import json
import os
import re


def edit(root, rel, fn):
    path = os.path.join(root, rel)

    if not os.path.isfile(path):
        return

    text = open(path, encoding="utf8").read()
    new = fn(text)

    if new != text:
        open(path, "w", encoding="utf8").write(new)


def mixins(text):
    data = json.loads(text)

    for key in ("mixins", "client", "server"):
        if key in data:
            data[key] = [m for m in data[key] if not m.startswith("iris.") and not m.startswith("sodium.")]

    return json.dumps(data, indent=4)


def run(root):
    edit(root, "src/client/resources/bbs.client.mixins.json", mixins)
    edit(root, "src/client/java/mchorse/bbs_mod/utils/iris/ShaderCurves.java",
         lambda t: t.replace("import net.irisshaders.iris.uniforms.custom.cached.CachedUniform;", "import mchorse.bbs_mod.utils.iris.IrisUtils.CachedUniform;"))


    def bbsmod(t):
        t = t.replace("new BlockItem(MODEL_BLOCK, new Item.Settings())", "new mchorse.bbs_mod.forge.RenderedBlockItem(MODEL_BLOCK, new Item.Settings())")

        return t

    edit(root, "src/main/java/mchorse/bbs_mod/BBSMod.java", bbsmod)

    def gun(t):
        i = t.rindex("}")

        return t[:i] + """
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(mchorse.bbs_mod.forge.ForgeItemHooks.extensions());
    }
}
"""

    edit(root, "src/main/java/mchorse/bbs_mod/items/GunItem.java", gun)

    def widener(t):
        return t.rstrip("\n") + "\naccessible method net/minecraft/world/GameRules$BooleanRule create (Z)Lnet/minecraft/world/GameRules$Type;\n"

    edit(root, "src/main/resources/bbs.accesswidener", widener)
