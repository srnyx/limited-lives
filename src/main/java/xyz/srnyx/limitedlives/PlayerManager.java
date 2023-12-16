package xyz.srnyx.limitedlives;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.data.EntityData;
import xyz.srnyx.annoyingapi.utility.BukkitUtility;

import java.util.List;
import java.util.logging.Level;


public class PlayerManager {
    @NotNull public static final String LIVES_KEY = "ll_lives";
    @NotNull public static final String DEAD_KEY = "ll_dead";
    @NotNull public static final String ITEM_KEY = "ll_item";

    @NotNull private final LimitedLives plugin;
    @NotNull private final Player player;
    @NotNull private final EntityData data;

    public PlayerManager(@NotNull LimitedLives plugin, @NotNull Player player) {
        this.plugin = plugin;
        this.player = player;
        this.data = new EntityData(plugin, player);
    }

    public int getLives() {
        final String livesString = new EntityData(plugin, player).get(LIVES_KEY);
        if (livesString != null) try {
            return Integer.parseInt(livesString);
        } catch (final NumberFormatException e) {
            AnnoyingPlugin.log(Level.WARNING, "&cInvalid lives for &4" + player.getName());
        }
        return plugin.config.livesDefault;
    }

    public int getMaxLives() {
        return BukkitUtility.getPermissionValue(player, "limitedlives.max.", plugin.config.livesMax);
    }

    @Nullable
    public Integer setLives(int amount) {
        if (amount < plugin.config.livesMin || amount > getMaxLives()) return null;
        final int oldLives = getLives();
        data.set(LIVES_KEY, amount);
        if (oldLives <= plugin.config.livesMin && amount > plugin.config.livesMin) revive();
        if (amount == plugin.config.livesMin) kill(null);
        return amount;
    }

    @Nullable
    public Integer addLives(int amount) {
        final int oldLives = getLives();
        final int newLives = oldLives + amount;
        if (newLives > getMaxLives()) return null;
        data.set(LIVES_KEY, newLives);
        if (oldLives <= plugin.config.livesMin && newLives > plugin.config.livesMin) revive();
        return newLives;
    }

    @Nullable
    public Integer removeLives(int amount, @Nullable Player killer) {
        int newLives = getLives() - amount;
        if (newLives < plugin.config.livesMin) return null;
        data.set(LIVES_KEY, newLives);
        if (newLives == plugin.config.livesMin) kill(killer);
        return newLives;
    }

    @Nullable
    public Integer withdrawLives(@NotNull Player sender, int amount) {
        if (plugin.config.recipe == null || getLives() <= amount) return null;
        final ItemStack item = plugin.config.recipe.getResult();
        item.setAmount(amount);
        sender.getInventory().addItem(item);
        return new PlayerManager(plugin, player).removeLives(amount, null);
    }

    private void revive() {
        new EntityData(plugin, player).remove(DEAD_KEY);
        dispatchCommands(plugin.config.commandsRevive, player, null);
    }

    private void kill(@Nullable Player killer) {
        new EntityData(plugin, player).set(DEAD_KEY, killer != null ? killer.getUniqueId().toString() : "null");
        dispatchCommands(plugin.config.commandsPunishmentDeath, player, killer);
    }

    public static void dispatchCommands(@NotNull List<String> commands, @NotNull Player player, @Nullable OfflinePlayer killer) {
        for (String command : commands) {
            command = command.replace("%player%", player.getName());
            if (killer == null && command.contains("%killer%")) continue;
            if (killer != null) command = command.replace("%killer%", killer.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
}
