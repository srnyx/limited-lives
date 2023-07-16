package xyz.srnyx.limitedlives;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPAPIExpansion;
import xyz.srnyx.annoyingapi.utility.BukkitUtility;


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
        if (player != null && identifier.equals("lives")) return String.valueOf(plugin.getLives(player.getUniqueId()));

        // lives_<player>
        if (identifier.startsWith("lives_")) {
            final OfflinePlayer target = BukkitUtility.getOfflinePlayer(identifier.substring(6));
            if (target == null) return "N/A";
            return String.valueOf(plugin.getLives(target.getUniqueId()));
        }

        // default
        if (identifier.equals("default")) return String.valueOf(plugin.config.livesDefault);

        // max
        if (identifier.equals("max")) return String.valueOf(plugin.config.livesMax);

        // min
        if (identifier.equals("min")) return String.valueOf(plugin.config.livesMin);

        return null;
    }
}
