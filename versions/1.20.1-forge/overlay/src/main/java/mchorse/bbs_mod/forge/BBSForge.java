package mchorse.bbs_mod.forge;

import net.minecraftforge.fml.common.Mod;

/** Forge entry point. The mod's own initialisers are Fabric-style and are driven from here. */
@Mod("bbs")
public class BBSForge
{
    public BBSForge()
    {
        ForgeBootstrap.init();
    }
}
