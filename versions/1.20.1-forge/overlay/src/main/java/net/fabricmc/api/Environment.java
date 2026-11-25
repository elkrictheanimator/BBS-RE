package net.fabricmc.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Compatibility marker: Forge decides sides on its own, so this only documents intent. */
@Retention(RetentionPolicy.CLASS)
public @interface Environment
{
    EnvType value();
}
