package xyz.srnyx.limitedlives.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPAPIExpansion;

import xyz.srnyx.limitedlives.LimitedLives;
import xyz.srnyx.limitedlives.managers.player.PlayerManager;


public class PlaceholderManager extends AnnoyingPAPIExpansion {
    @NotNull private final LimitedLives plugin;

    public PlaceholderManager(@NotNull LimitedLives plugin) {
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
            // grace-active
            case "grace-active": return String.valueOf(new PlayerManager(plugin, player).hasGrace());
            // grace-left
            case "grace-left": return String.valueOf(new PlayerManager(plugin, player).getGraceLeft());
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
        // grace-active_PLAYER
        if (identifier.startsWith("grace-active_")) {
            final Player target = Bukkit.getPlayerExact(identifier.substring(13));
            return target == null ? "N/A" : String.valueOf(new PlayerManager(plugin, target).hasGrace());
        }
        // grace-left_PLAYER
        if (identifier.startsWith("grace-left_")) {
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
