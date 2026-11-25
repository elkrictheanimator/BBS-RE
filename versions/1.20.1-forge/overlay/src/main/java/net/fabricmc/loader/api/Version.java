package net.fabricmc.loader.api;

public class Version
{
    private final String version;

    public Version(String version)
    {
        this.version = version;
    }

    public String getFriendlyString()
    {
        return this.version;
    }
}
