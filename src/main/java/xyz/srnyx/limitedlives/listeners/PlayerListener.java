package xyz.srnyx.limitedlives.listeners;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.AnnoyingListener;
import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.data.EntityData;
import xyz.srnyx.annoyingapi.message.AnnoyingMessage;
import xyz.srnyx.annoyingapi.message.DefaultReplaceType;

import xyz.srnyx.limitedlives.config.Feature;
import xyz.srnyx.limitedlives.config.GracePeriodTrigger;
import xyz.srnyx.limitedlives.LimitedLives;
import xyz.srnyx.limitedlives.managers.player.PlayerManager;
import xyz.srnyx.limitedlives.managers.player.exception.ActionException;
import xyz.srnyx.limitedlives.managers.player.exception.LessThanMinLives;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;


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
        final Player player = event.getEntity();
        
        // Check if plugin enabled in world or player bypasses
        final World world = player.getWorld();
        if (!plugin.config.worldsBlacklist.isWorldEnabled(world, Feature.LIFE_LOSS) || player.hasPermission("limitedlives.bypass")) return;

        // Get killer
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
        if (cause == null || !plugin.config.gracePeriod.bypassCauses.contains(cause)) {
            final long graceLeft = manager.getGraceLeft();
            if (graceLeft > 0) {
                new AnnoyingMessage(plugin, "lives.grace")
                        .replace("%remaining%", graceLeft, DefaultReplaceType.TIME)
                        .send(player);
                return;
            }
        }

        // Remove life
        try {
            final int newLives = manager.removeLives(1, killer);
            if (newLives <= plugin.config.lives.min) {
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
        } catch (final LessThanMinLives e) {
            // No more lives
            new AnnoyingMessage(plugin, "lives.zero").send(player);
        }

        // keepInventory integration
        if (plugin.config.keepInventory.enabled && plugin.config.worldsBlacklist.isWorldEnabled(world, Feature.KEEP_INVENTORY)) plugin.config.keepInventory.actions.getAction(manager.getDeaths()).consumer.accept(event);

        // Give life to killer
        if (plugin.config.obtaining.stealing && isPvp && plugin.config.worldsBlacklist.isWorldEnabled(world, Feature.OBTAINING_STEALING)) try {
            new AnnoyingMessage(plugin, "lives.steal")
                    .replace("%target%", player.getName())
                    .replace("%lives%", new PlayerManager(plugin, killer).addLives(1))
                    .send(killer);
        } catch (final ActionException ignored) {}
    }

    @EventHandler
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final EntityData data = new EntityData(plugin, player);
        final String killerString = data.get(PlayerManager.DEAD_KEY);
        if (killerString == null) return;
        data.remove(PlayerManager.DEAD_KEY);

        // Get killer
        OfflinePlayer killer = null;
        if (!killerString.equals("null")) try {
            killer = Bukkit.getOfflinePlayer(UUID.fromString(killerString));
        } catch (final IllegalArgumentException ignored) {}
        final OfflinePlayer finalKiller = killer;

        // Run respawn commands
        new PlayerManager(plugin, player).dispatchCommands(plugin.config.commands.punishment.respawn, finalKiller);
    }

    @EventHandler
    public void onEntityDamageByEntity(@NotNull EntityDamageEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;
        final String cause = event instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) event).getDamager() instanceof Player ? "PLAYER_ATTACK" : event.getCause().name();
        if (plugin.config.gracePeriod.disabledDamageCauses.contains(cause) && new PlayerManager(plugin, (Player) entity).hasGrace()) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final EntityData data = new EntityData(plugin, player);

        // Convert old data
        final Map<String, String> failed = data.convertOldData(true, PlayerManager.LIVES_KEY, PlayerManager.DEAD_KEY);
        if (failed == null) {
            AnnoyingPlugin.log(Level.SEVERE, "Failed to convert old data for player " + player.getName());
        } else if (!failed.isEmpty()) {
            AnnoyingPlugin.log(Level.WARNING, "Failed to convert some old data for player " + player.getName() + ": " + failed);
        }

        // Start grace period
        if (plugin.config.gracePeriod.enabled && (plugin.config.gracePeriod.triggers.contains(GracePeriodTrigger.JOIN) || (plugin.config.gracePeriod.triggers.contains(GracePeriodTrigger.FIRST_JOIN) && !player.hasPlayedBefore()))) data.set(PlayerManager.GRACE_START_KEY, System.currentTimeMillis());
    }
}
