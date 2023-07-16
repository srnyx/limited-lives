package xyz.srnyx.limitedlives.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.AnnoyingListener;
import xyz.srnyx.annoyingapi.message.AnnoyingMessage;
import xyz.srnyx.annoyingapi.utility.ItemDataUtility;

import xyz.srnyx.limitedlives.LimitedLives;

import java.util.UUID;


public class PlayerListener implements AnnoyingListener {
    @NotNull private final LimitedLives plugin;

    public PlayerListener(@NotNull LimitedLives plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public LimitedLives getAnnoyingPlugin() {
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
        final Integer newLives = plugin.removeLives(player, 1, killer);
        if (newLives == null || newLives == plugin.config.livesMin) {
            // No more lives
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
        final Integer newKillerLives = plugin.addLives(killer, 1);
        if (newKillerLives != null) new AnnoyingMessage(plugin, "lives.steal")
                .replace("%target%", player.getName())
                .replace("%lives%", newKillerLives)
                .send(killer);
    }

    @EventHandler
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        if (!plugin.deadPlayers.containsKey(player.getUniqueId())) return;
        final UUID killerUuid = plugin.deadPlayers.remove(player.getUniqueId());
        new BukkitRunnable() {
            public void run() {
                LimitedLives.dispatchCommands(plugin.config.commandsPunishmentRespawn, player, killerUuid == null ? null : Bukkit.getOfflinePlayer(killerUuid));
            }
        }.runTaskLater(plugin, 1);
    }

    @EventHandler
    public void onPlayerItemConsume(@NotNull PlayerItemConsumeEvent event) {
        if (plugin.config.recipe == null || new ItemDataUtility(plugin, event.getItem()).get("ll_item") == null) return;
        final Player player = event.getPlayer();
        final Integer newLives = plugin.addLives(player, plugin.config.recipeAmount);
        if (newLives == null) {
            event.setCancelled(true);
            new AnnoyingMessage(plugin, "eat.max")
                    .replace("%max%", plugin.config.livesMax)
                    .send(player);
            return;
        }
        new AnnoyingMessage(plugin, "eat.success")
                .replace("%lives%", newLives)
                .send(player);
    }
}
