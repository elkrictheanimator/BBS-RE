# Versions

The source tree at the repo root is the **Minecraft 1.20.4** build. Each other version is a thin
layer on top of it:

- `versions/<ver>/overlay/` - files that differ for that version, at the same paths as in the repo.
- `versions/<ver>/deleted.txt` - files that must not exist for that version.

`python3 scripts/apply_version.py <ver>` applies the layer to the working tree (CI does this for
every matrix entry, then runs `./gradlew build`). One push builds all three jars.

A change to a file that has an overlay copy has to be made in the overlay too, otherwise that
version keeps the old text. Files without an overlay are shared, so a change reaches all versions.
