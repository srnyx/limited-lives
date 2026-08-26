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
import xyz.srnyx.annoyingapi.message.DefaultReplaceType;
import xyz.srnyx.limitedlives.config.Feature;
import xyz.srnyx.limitedlives.config.GracePeriodTrigger;
import xyz.srnyx.limitedlives.LimitedLives;
import xyz.srnyx.limitedlives.config.damagecause.CustomDamageCause;
import xyz.srnyx.limitedlives.config.damagecause.DamageCauseWrapper;
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
        if (!plugin.config.worlds_blacklist.isWorldEnabled(world, Feature.LIFE_LOSS) || player.hasPermission("limitedlives.bypass")) return;

        // Get killer
        final Player killer = player.getKiller();
        final boolean isPvp = killer != null && killer != player;

        // Get death cause
        DamageCauseWrapper cause = CustomDamageCause.PLAYER_ATTACK.wrap();
        if (!isPvp) {
            final EntityDamageEvent damageEvent = player.getLastDamageCause();
            cause = damageEvent != null ? new DamageCauseWrapper(damageEvent.getCause()) : null;
        }

        // Check death cause
        if (cause != null && !plugin.config.death_causes.isEmpty() && !plugin.config.death_causes.contains(cause)) return;
        // Check WorldGuard regions
        if (plugin.worldGuard != null && !plugin.worldGuard.test(player)) return;
        // Check grace
        final PlayerManager manager = new PlayerManager(plugin, player);
        if (cause == null || !plugin.config.grace_period.bypass_causes.contains(cause)) {
            final long graceLeft = manager.getGraceLeft();
            if (graceLeft > 0) {
                plugin.getMessages().get().lives.grace.newMessage()
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
                plugin.getMessages().get().lives.zero.newMessage().send(player);
            } else if (isPvp) {
                // Lose to player
                plugin.getMessages().get().lives.lose.player.newMessage()
                        .replace("%killer%", killer.getName())
                        .replace("%lives%", newLives)
                        .send(player);
            } else {
                // Lose to other
                plugin.getMessages().get().lives.lose.other.newMessage()
                        .replace("%lives%", newLives)
                        .send(player);
            }
        } catch (final LessThanMinLives e) {
            // No more lives
            plugin.getMessages().get().lives.zero.newMessage().send(player);
        }

        // keepInventory integration
        if (plugin.config.keep_inventory.enabled && plugin.config.worlds_blacklist.isWorldEnabled(world, Feature.KEEP_INVENTORY)) plugin.config.keep_inventory.actions.getAction(manager.getDeaths()).consumer.accept(event);

        // Give life to killer
        if (plugin.config.obtaining.stealing && isPvp && plugin.config.worlds_blacklist.isWorldEnabled(world, Feature.OBTAINING_STEALING)) try {
            plugin.getMessages().get().lives.steal.newMessage()
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
        final DamageCauseWrapper cause = event instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) event).getDamager() instanceof Player ? CustomDamageCause.PLAYER_ATTACK.wrap() : new DamageCauseWrapper(event.getCause());
        if (plugin.config.grace_period.disabled_damage_causes.contains(cause) && new PlayerManager(plugin, (Player) entity).hasGrace()) event.setCancelled(true);
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
        if (plugin.config.grace_period.enabled && (plugin.config.grace_period.triggers.contains(GracePeriodTrigger.JOIN) || (plugin.config.grace_period.triggers.contains(GracePeriodTrigger.FIRST_JOIN) && !player.hasPlayedBefore()))) data.set(PlayerManager.GRACE_START_KEY, System.currentTimeMillis());
    }
}
