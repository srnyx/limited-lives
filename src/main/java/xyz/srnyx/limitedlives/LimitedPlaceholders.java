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
        // lives
        if (player != null && identifier.equals("lives")) return String.valueOf(new PlayerManager(plugin, player).getLives());

        // lives_PLAYER
        if (identifier.startsWith("lives_")) {
            final Player target = Bukkit.getPlayerExact(identifier.substring(6));
            return target == null ? "N/A" : String.valueOf(new PlayerManager(plugin, target).getLives());
        }

        // default
        if (identifier.equals("default")) return String.valueOf(plugin.config.livesDefault);

        // max
        if (identifier.equals("max")) return String.valueOf(player == null ? plugin.config.livesMax : new PlayerManager(plugin, player).getMaxLives());

        // min
        if (identifier.equals("min")) return String.valueOf(plugin.config.livesMin);

        return null;
    }
}
