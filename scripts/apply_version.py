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

def resolve_placeholders():
    """1.21.11: fill the @NAME@ markers with the newest matching versions, looked up when the build runs."""
    import json
    import re
    import urllib.request

    def get(url):
        with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "bbs-re-build"}), timeout=60) as r:
            return r.read().decode("utf8")

    def maven(group_path, artifact):
        text = get("https://maven.fabricmc.net/%s/%s/maven-metadata.xml" % (group_path, artifact))
        return re.findall(r"<version>([^<]+)</version>", text)

    def key(v):
        return [int(p) if p.isdigit() else 0 for p in re.split(r"[.+\-]", v)]

    values = {}
    mc = version

    yarn = [v for v in maven("net/fabricmc", "yarn") if v.startswith(mc + "+build.")]
    values["YARN"] = max(yarn, key=key)

    loader = [v for v in maven("net/fabricmc", "fabric-loader") if re.fullmatch(r"\d+\.\d+\.\d+", v)]
    values["LOADER"] = max(loader, key=key)

    api = [v for v in maven("net/fabricmc/fabric-api", "fabric-api") if v.endswith("+" + mc)]
    values["FABRIC"] = max(api, key=key)

    for name, project in (("SODIUM", "sodium"), ("IRIS", "iris")):
        url = "https://api.modrinth.com/v2/project/%s/version?game_versions=%s&loaders=%s" % (project, '["%s"]' % mc, '["fabric"]')
        found = json.loads(get(url.replace("[", "%5B").replace("]", "%5D").replace('"', "%22")))
        found = [v for v in found if v.get("version_type") in ("release", "beta")] or found
        values[name] = found[0]["version_number"]

    print("resolved", values)

    for folder, _, names in os.walk(root):
        if ".git" in folder or "build" in folder.split(os.sep):
            continue

        for n in names:
            if n in ("build.gradle", "gradle.properties"):
                path = os.path.join(folder, n)
                text = open(path, encoding="utf8").read()
                new = re.sub(r"@([A-Z]+)@", lambda m: values.get(m.group(1), m.group(0)), text)

                if new != text:
                    open(path, "w", encoding="utf8").write(new)


def apply_layer(version):
    base = os.path.join(root, "versions", version)

    if not os.path.isdir(base):
        sys.exit("unknown version %s (have: %s)" % (version, ", ".join(["1.20.4"] + sorted(d for d in os.listdir(os.path.join(root, "versions")) if os.path.isdir(os.path.join(root, "versions", d))))))

    parent = os.path.join(base, "parent.txt")

    if os.path.isfile(parent):
        apply_layer(open(parent, encoding="utf8").read().strip())

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


if version == "1.20.4":
    print("1.20.4 is the base tree; nothing to apply")
    sys.exit(0)

apply_layer(version)

_transform = os.path.join(root, "versions", version, "transform.py")

if os.path.isfile(_transform):
    _ns = {}
    exec(compile(open(_transform, encoding="utf8").read(), _transform, "exec"), _ns)
    _ns["run"](root)

if version == "1.21.11" and not os.environ.get("BBS_SKIP_RESOLVE"):
    resolve_placeholders()
