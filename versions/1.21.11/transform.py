"""Token-level renames for 1.21.11, run by scripts/apply_version.py after the overlay is copied.

Every rule is idempotent. Anything that needs more than a rename lives as a whole file in overlay/."""
import os
import re

RULES = [
    (r"net\.minecraft\.client\.render\.model\.json\.ModelTransformationMode", "net.minecraft.item.ItemDisplayContext"),
    (r"\bModelTransformationMode\b", "ItemDisplayContext"),
    (r"net\.minecraft\.util\.UseAction", "net.minecraft.item.consume.UseAction"),
    (r"EntityAttributes\.GENERIC_", "EntityAttributes."),
    (r"EntityPose\.FALL_FLYING", "EntityPose.GLIDING"),
    (r"\.selectedSlot = ([^;]+);", r".setSelectedSlot(\1);"),
    (r"\.selectedSlot\b(?!\s*=[^=])(?!\()", ".getSelectedSlot()"),
    (r"\.isClient\b(?!\()", ".isClient()"),
    (r"\.createNbtWithId\(", ".createNbtWithIdentifyingData("),
    (r"\.executeWithPrefix\(", ".parseAndExecute("),
    (r"\.hasPermissionLevel\((\d)\)", r".getPermissions().hasPermission(new net.minecraft.command.permission.Permission.Level(net.minecraft.command.permission.PermissionLevel.fromLevel(\1)))"),
]


def run(root):
    count = 0

    for top in ("src",):
        for folder, _, names in os.walk(os.path.join(root, top)):
            for name in names:
                if not name.endswith(".java"):
                    continue

                path = os.path.join(folder, name)
                text = open(path, encoding="utf8").read()
                new = text

                for pattern, repl in RULES:
                    new = re.sub(pattern, repl, new)

                if new != text:
                    open(path, "w", encoding="utf8").write(new)
                    count += 1

    print("transform: touched %d files" % count)
