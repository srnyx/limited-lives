package xyz.srnyx.limitedlives;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPAPIExpansion;


public class LimitedPlaceholders extends AnnoyingPAPIExpansion {
    @NotNull private final LimitedLives plugin;

    public LimitedPlaceholders(@NotNull LimitedLives plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public LimitedLives getAnnoyingPlugin() {
        return plugin;
    }

    @Override @NotNull
    public String getIdentifier() {
        return "lives";
    }

    @Override @Nullable
    public String onPlaceholderRequest(@Nullable Player player, @NotNull String identifier) {
        if (player != null) switch (identifier) {
            // lives
            case "lives": return String.valueOf(new PlayerManager(plugin, player).getLives());
            // max
            case "max": return String.valueOf(new PlayerManager(plugin, player).getMaxLives());
            // grace
            case "grace": return String.valueOf(new PlayerManager(plugin, player).hasGrace());
            // graceremaining
            case "graceremaining": return String.valueOf(new PlayerManager(plugin, player).getGraceLeft());
        }

        // lives_PLAYER
        if (identifier.startsWith("lives_")) {
            final Player target = Bukkit.getPlayerExact(identifier.substring(6));
            return target == null ? "N/A" : String.valueOf(new PlayerManager(plugin, target).getLives());
        }
        // max_PLAYER
        if (identifier.startsWith("max_")) {
            final Player target = Bukkit.getPlayerExact(identifier.substring(4));
            return target == null ? "N/A" : String.valueOf(new PlayerManager(plugin, target).getMaxLives());
        }
        // grace_PLAYER
        if (identifier.startsWith("grace_")) {
            final Player target = Bukkit.getPlayerExact(identifier.substring(6));
            return target == null ? "N/A" : String.valueOf(new PlayerManager(plugin, target).hasGrace());
        }
        // graceremaining_PLAYER
        if (identifier.startsWith("graceremaining_")) {
            final Player target = Bukkit.getPlayerExact(identifier.substring(11));
            return target == null ? "N/A" : String.valueOf(new PlayerManager(plugin, target).getGraceLeft());
        }

        switch (identifier) {
            // default
            case "default": return String.valueOf(plugin.config.lives.def);
            // max
            case "max": return String.valueOf(plugin.config.lives.max);
            // min
            case "min": return String.valueOf(plugin.config.lives.min);

            // Unknown
            default: return null;
        }
    }
}
