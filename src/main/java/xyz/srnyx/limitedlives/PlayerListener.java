package xyz.srnyx.limitedlives;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.AnnoyingListener;
import xyz.srnyx.annoyingapi.data.EntityData;
import xyz.srnyx.annoyingapi.data.ItemData;
import xyz.srnyx.annoyingapi.message.AnnoyingMessage;

import java.util.UUID;


public class PlayerListener extends AnnoyingListener {
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
        // Get killer
        final Player player = event.getEntity();
        final Player killer = player.getKiller();
        final boolean isPvp = killer != null && killer != player;

        // Get death cause
        String cause = "PLAYER_ATTACK";
        if (!isPvp) {
            final EntityDamageEvent damageEvent = player.getLastDamageCause();
            cause = damageEvent != null ? damageEvent.getCause().name() : null;
        }

        // Check death cause
        if (cause != null && !plugin.config.deathCauses.isEmpty() && !plugin.config.deathCauses.contains(cause)) return;
        // Check WorldGuard regions
        if (plugin.worldGuard != null && !plugin.worldGuard.test(player)) return;
        // Check grace
        final PlayerManager manager = new PlayerManager(plugin, player);
        if (plugin.config.gracePeriod.enabled && (cause == null || !plugin.config.gracePeriod.bypassCauses.contains(cause)) && manager.hasGrace()) {
            new AnnoyingMessage(plugin, "grace")
                    .replace("%remaining%", manager.getGraceLeft())
                    .send(player);
            return;
        }

        // Remove life
        final Integer newLives = manager.removeLives(1, killer);
        if (newLives == null || newLives == plugin.config.lives.min) {
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

        // keepInventory integration
        if (plugin.config.keepInventory.enabled) plugin.config.keepInventory.actions.getAction(manager.getDeaths()).consumer.accept(event);

        // Give life to killer
        if (!plugin.config.obtaining.stealing || !isPvp) return;
        final Integer newKillerLives = new PlayerManager(plugin, killer).addLives(1);
        if (newKillerLives != null) new AnnoyingMessage(plugin, "lives.steal")
                .replace("%target%", player.getName())
                .replace("%lives%", newKillerLives)
                .send(killer);
    }

    @EventHandler
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final EntityData data = new EntityData(plugin, player);
        final String killerString = data.get(PlayerManager.DEAD_KEY);
        if (killerString == null) return;
        data.remove(PlayerManager.DEAD_KEY);
        OfflinePlayer killer = null;
        if (!killerString.equals("null")) try {
            killer = Bukkit.getOfflinePlayer(UUID.fromString(killerString));
        } catch (final IllegalArgumentException ignored) {
            // ignored
        }
        final OfflinePlayer finalKiller = killer;
        new BukkitRunnable() {
            public void run() {
                PlayerManager.dispatchCommands(plugin.config.commands.punishment.respawn, player, finalKiller);
            }
        }.runTaskLater(plugin, 1);
    }

    @EventHandler
    public void onPlayerItemConsume(@NotNull PlayerItemConsumeEvent event) {
        if (plugin.config.obtaining.crafting.recipe == null || !new ItemData(plugin, event.getItem()).has(PlayerManager.ITEM_KEY)) return;
        final Player player = event.getPlayer();
        final Integer newLives = new PlayerManager(plugin, player).addLives(plugin.config.obtaining.crafting.amount);
        if (newLives == null) {
            event.setCancelled(true);
            new AnnoyingMessage(plugin, "eat.max")
                    .replace("%max%", plugin.config.lives.max)
                    .send(player);
            return;
        }
        new AnnoyingMessage(plugin, "eat.success")
                .replace("%lives%", newLives)
                .send(player);
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        final EntityData data = new EntityData(plugin, event.getPlayer());
        // Convert old data
        data.convertOldData(true, PlayerManager.LIVES_KEY, PlayerManager.DEAD_KEY);
        // Set FIRST_JOIN_KEY
        if (plugin.config.gracePeriod.enabled && !data.has(PlayerManager.FIRST_JOIN_KEY)) data.set(PlayerManager.FIRST_JOIN_KEY, System.currentTimeMillis());
    }
}
