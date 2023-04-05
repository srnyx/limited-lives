package xyz.srnyx.limitedlives;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.AnnoyingListener;
import xyz.srnyx.annoyingapi.AnnoyingMessage;

import java.util.UUID;


public class PlayerListener implements AnnoyingListener {
    @NotNull private final LimitedLives plugin;

    public PlayerListener(@NotNull LimitedLives plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public LimitedLives getPlugin() {
        return plugin;
    }

    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        final Player player = event.getEntity();
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        final Player killer = player.getKiller();

        // Remove life
        final UUID uuid = player.getUniqueId();
        final int newLives = plugin.lives.getOrDefault(uuid, plugin.livesDefault) - 1;
        if (newLives >= plugin.livesMin) plugin.lives.put(uuid, newLives);

        // No more lives
        if (newLives <= plugin.livesMin) {
            player.setGameMode(GameMode.SPECTATOR);
            new AnnoyingMessage(plugin, "lives.zero").send(player);
        } else {
            if (killer != null) {
                // Lose to player
                new AnnoyingMessage(plugin, "lives.lose.player")
                        .replace("%killer%", killer.getName())
                        .replace("%lives%", newLives)
                        .send(player);
            } else {
                // Lose to other
                new AnnoyingMessage(plugin, "lives.lose.other")
                        .replace("%lives%", newLives)
                        .send(player);
            }
        }

        // Give life to killer
        if (killer == null) return;
        final UUID killerUuid = killer.getUniqueId();
        final int newKillerLives = plugin.lives.getOrDefault(killerUuid, plugin.livesDefault) + 1;
        if (newKillerLives > plugin.livesMax) return;
        plugin.lives.put(killerUuid, newKillerLives);
        new AnnoyingMessage(plugin, "lives.steal")
                .replace("%target%", player.getName())
                .replace("%lives%", newKillerLives)
                .send(killer);
    }
}
