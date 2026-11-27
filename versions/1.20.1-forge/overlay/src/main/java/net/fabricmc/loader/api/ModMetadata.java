package net.fabricmc.loader.api;

public class ModMetadata
{
    private final String id;
    private final Version version;

    public ModMetadata(String id, String version)
    {
        this.id = id;
        this.version = new Version(version);
    }

    public String getId()
    {
        return this.id;
    }

    public Version getVersion()
    {
        return this.version;
    }
}
