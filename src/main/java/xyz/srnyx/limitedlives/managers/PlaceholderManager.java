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
        // Non-player placeholders
        switch (identifier) {
            // default
            case "default": return String.valueOf(plugin.config.lives.def);
            // max
            case "max": return String.valueOf(plugin.config.lives.max);
            // min
            case "min": return String.valueOf(plugin.config.lives.min);
        }

        // Get player
        if (player == null) {
            final int underscoreIndex = identifier.indexOf('_');
            if (underscoreIndex == -1) return null;
            player = Bukkit.getPlayerExact(identifier.substring(underscoreIndex + 1));
            if (player == null) return "N/A";
            identifier = identifier.substring(0, underscoreIndex).toLowerCase(); // Needs to be set after player
        }

        // Player placeholders
        switch (identifier) {
            // lives
            case "lives": return String.valueOf(new PlayerManager(plugin, player).getLives());
            // max
            case "max": return String.valueOf(new PlayerManager(plugin, player).getMaxLives());
            // grace-active
            case "grace-active": return String.valueOf(new PlayerManager(plugin, player).hasGrace());
            // grace-left
            case "grace-left": return String.valueOf(new PlayerManager(plugin, player).getGraceLeft());
            // bypass
            case "bypass": return String.valueOf(player.hasPermission("limitedlives.bypass"));
        }

        // Unknown placeholder
        return null;
    }
}
