package xyz.srnyx.limitedlives;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingListener;
import xyz.srnyx.annoyingapi.AnnoyingMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class PlayerListener implements AnnoyingListener {
    @NotNull private final LimitedLives plugin;
    @NotNull private final Map<UUID, UUID> deadPlayers = new HashMap<>();

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
        final EntityDamageEvent damageEvent = player.getLastDamageCause();
        if (damageEvent != null && !plugin.config.deathCauses.isEmpty() && !plugin.config.deathCauses.contains(damageEvent.getCause())) return;
        final Player killer = player.getKiller();
        final boolean isPvp = killer != null && killer != player;

        // Remove life
        final UUID uuid = player.getUniqueId();
        final int newLives = plugin.getLives(uuid) - 1;
        if (newLives >= plugin.config.livesMin) plugin.lives.put(uuid, newLives);

        if (newLives <= plugin.config.livesMin) {
            // No more lives
            deadPlayers.put(uuid, isPvp ? killer.getUniqueId() : null);
            dispatchCommands(plugin.config.punishmentCommandsDeath, player, killer);
            new AnnoyingMessage(plugin, "lives.zero").send(player);
        } else if (isPvp) {
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

        // Give life to killer
        if (!plugin.config.stealing || !isPvp) return;
        final UUID killerUuid = killer.getUniqueId();
        final int newKillerLives = plugin.getLives(killerUuid) + 1;
        if (newKillerLives > plugin.config.livesMax) return;
        plugin.lives.put(killerUuid, newKillerLives);
        new AnnoyingMessage(plugin, "lives.steal")
                .replace("%target%", player.getName())
                .replace("%lives%", newKillerLives)
                .send(killer);
    }

    @EventHandler
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        if (!deadPlayers.containsKey(player.getUniqueId())) return;
        final UUID killerUuid = deadPlayers.remove(player.getUniqueId());
        final OfflinePlayer killer = killerUuid == null ? null : Bukkit.getOfflinePlayer(killerUuid);
        new BukkitRunnable() {
            public void run() {
                dispatchCommands(plugin.config.punishmentCommandsRespawn, player, killer);
            }
        }.runTaskLater(plugin, 1);
    }

    private void dispatchCommands(@NotNull List<String> commands, @NotNull Player player, @Nullable OfflinePlayer killer) {
        commands.forEach(command -> {
            command = command.replace("%player%", player.getName());
            if (killer == null && command.contains("%killer%")) return;
            if (killer != null) command = command.replace("%killer%", killer.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        });
    }
}
