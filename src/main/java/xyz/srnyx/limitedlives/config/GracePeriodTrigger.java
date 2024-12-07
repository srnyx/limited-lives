package xyz.srnyx.limitedlives.config;

import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;

import java.util.logging.Level;


public enum GracePeriodTrigger {
    FIRST_JOIN,
    JOIN,
    REVIVE;

    @Nullable
    public static GracePeriodTrigger fromString(@Nullable String string) {
        if (string != null) try {
            return valueOf(string.toUpperCase());
        } catch (final IllegalArgumentException e) {
            AnnoyingPlugin.log(Level.WARNING, "Invalid value in grace-period.trigger: " + string);
        }
        return null;
    }
}
