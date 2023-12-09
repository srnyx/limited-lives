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
        // Check death cause and WorldGuard regions
        final Player player = event.getEntity();
        final EntityDamageEvent damageEvent = player.getLastDamageCause();
        if ((damageEvent != null && !plugin.config.deathCauses.isEmpty() && !plugin.config.deathCauses.contains(damageEvent.getCause())) || (plugin.worldGuard != null && !plugin.worldGuard.test(player))) return;
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
        final EntityData data = new EntityData(plugin, player);
        final String killerString = data.get(LimitedLives.DEAD_KEY);
        if (killerString == null) return;
        data.remove(LimitedLives.DEAD_KEY);
        OfflinePlayer killer = null;
        if (!killerString.equals("null")) try {
            killer = Bukkit.getOfflinePlayer(UUID.fromString(killerString));
        } catch (final IllegalArgumentException ignored) {
            // ignored
        }
        final OfflinePlayer finalKiller = killer;
        new BukkitRunnable() {
            public void run() {
                LimitedLives.dispatchCommands(plugin.config.commandsPunishmentRespawn, player, finalKiller);
            }
        }.runTaskLater(plugin, 1);
    }

    @EventHandler
    public void onPlayerItemConsume(@NotNull PlayerItemConsumeEvent event) {
        if (plugin.config.recipe == null || !new ItemData(plugin, event.getItem()).has(LimitedLives.ITEM_KEY)) return;
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

    /**
     * Called when a player joins a server
     *
     * @deprecated  Used to convert old data
     */
    @EventHandler @Deprecated
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        if (plugin.oldData == null) return;
        final Player player = event.getPlayer();
        final EntityData data = new EntityData(plugin, player);
        final UUID uuid = player.getUniqueId();
        boolean save = false;

        // oldLives
        if (plugin.oldLivesData != null) {
            final Integer lives = plugin.oldLivesData.remove(uuid);
            if (lives != null) {
                data.set(LimitedLives.LIVES_KEY, lives);
                plugin.oldData.set("lives." + uuid, null);
                save = true;
            }
        }

        // oldDeadPlayers
        if (plugin.oldDeadPlayers != null) {
            final UUID killer = plugin.oldDeadPlayers.remove(uuid);
            if (killer != null) {
                data.set(LimitedLives.DEAD_KEY, killer);
                plugin.oldData.set("dead-players." + uuid, null);
                save = true;
            }
}
