package net.fabricmc.loader.api;

public class ModContainer
{
    private final ModMetadata metadata;

    public ModContainer(String id, String version)
    {
        this.metadata = new ModMetadata(id, version);
    }

    public ModMetadata getMetadata()
    {
        return this.metadata;
    }
}
