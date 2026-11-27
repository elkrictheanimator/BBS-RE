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
        t = t.replace("FabricDefaultAttributeRegistry.register(ACTOR_ENTITY, ActorEntity.createActorAttributes());", "FabricDefaultAttributeRegistry.registerLazy(ACTOR_ENTITY, ActorEntity::createActorAttributes);")
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
        return t.rstrip("\n") + "\naccessible method net/minecraft/world/GameRules$BooleanRule create (Z)Lnet/minecraft/world/GameRules$Type;\naccessible field net/minecraft/server/command/CommandManager$RegistrationEnvironment dedicated Z\n"

    edit(root, "src/main/resources/bbs.accesswidener", widener)

    edit(root, "src/client/java/mchorse/bbs_mod/client/BBSRendering.java",
         lambda t: t.replace("import net.irisshaders.iris.uniforms.custom.cached.CachedUniform;", "import mchorse.bbs_mod.utils.iris.IrisUtils.CachedUniform;"))

    # Registry.register is locked on Forge outside the register event: route it through the queue
    for base in ("src/main/java", "src/client/java"):
        for dirpath, _, files in os.walk(os.path.join(root, base)):
            for f in files:
                if not f.endswith(".java") or dirpath.endswith("/forge"):
                    continue

                rel = os.path.relpath(os.path.join(dirpath, f), root)

                edit(root, rel, lambda t: re.sub(r"(?<![\w.])Registry\.register\(", "mchorse.bbs_mod.forge.ForgeRegistry.register(", t) if "Registry.register(" in t else t)

    def minversion(text):
        data = json.loads(text)
        data.setdefault("minVersion", "0.8")

        return json.dumps(data, indent=4)

    edit(root, "src/main/resources/bbs.mixins.json", minversion)
    edit(root, "src/client/resources/bbs.client.mixins.json", minversion)

    # The Fabric API shim must not live in net.fabricmc.*: when Sinytra Connector / Fabric API are in
    # the mods folder they own those packages too, and Forge's module layer refuses two modules with
    # the same package (the game then exits during loading). Move the shim into our own package.
    old_dir = os.path.join(root, "src/main/java/net/fabricmc")
    new_dir = os.path.join(root, "src/main/java/mchorse/bbs_mod/fabricshim")

    if os.path.isdir(old_dir):
        import shutil

        os.makedirs(os.path.dirname(new_dir), exist_ok=True)
        shutil.move(old_dir, new_dir)

        parent = os.path.join(root, "src/main/java/net")

        if os.path.isdir(parent) and not os.listdir(parent):
            os.rmdir(parent)

    for base in ("src/main/java", "src/client/java"):
        for dirpath, _, files in os.walk(os.path.join(root, base)):
            for f in files:
                if f.endswith(".java"):
                    rel = os.path.relpath(os.path.join(dirpath, f), root)

                    edit(root, rel, lambda t: t.replace("net.fabricmc.", "mchorse.bbs_mod.fabricshim.") if "net.fabricmc." in t else t)

    edit(root, "src/client/java/mchorse/bbs_mod/forms/renderers/ModelFormRenderer.java",
         lambda t: t.replace("ActorEntityRenderer.armorRenderer.renderArmorSlot(", "mchorse.bbs_mod.forge.ForgeArmor.get().renderArmorSlot("))
