#!/usr/bin/env python3
"""Turn this source tree into the build for one Minecraft version.

The tree itself is the 1.20.4 build. Every other version is a small set of files that differ
(versions/<ver>/overlay, mirrored at the repo root) plus files that must not exist there
(versions/<ver>/deleted.txt). Run before gradle:

    python3 scripts/apply_version.py 1.21.1
    ./gradlew build

1.20.4 is the base, so it changes nothing. The script edits the working tree in place; use a
clean checkout (CI does) or `git checkout . && git clean -fd src` to go back.
"""
import os
import shutil
import sys

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
version = sys.argv[1] if len(sys.argv) > 1 else "1.20.4"

if version == "1.20.4":
    print("1.20.4 is the base tree; nothing to apply")
    sys.exit(0)

base = os.path.join(root, "versions", version)

if not os.path.isdir(base):
    sys.exit("unknown version %s (have: %s)" % (version, ", ".join(["1.20.4"] + sorted(os.listdir(os.path.join(root, "versions"))))))

deleted = os.path.join(base, "deleted.txt")

if os.path.isfile(deleted):
    for line in open(deleted, encoding="utf8").read().splitlines():
        line = line.strip()

        if line and os.path.isfile(os.path.join(root, line)):
            os.remove(os.path.join(root, line))
            print("deleted", line)

count = 0
overlay = os.path.join(base, "overlay")

for folder, _, names in os.walk(overlay):
    for name in names:
        source = os.path.join(folder, name)
        target = os.path.join(root, os.path.relpath(source, overlay))

        os.makedirs(os.path.dirname(target), exist_ok=True)
        shutil.copyfile(source, target)
        count += 1

print("applied %d files for %s" % (count, version))
