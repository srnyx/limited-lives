package xyz.srnyx.limitedlives;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.data.StringData;
import xyz.srnyx.annoyingapi.utility.BukkitUtility;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;


public class PlayerManager {
    @NotNull public static final String LIVES_KEY = "ll_lives";
    @NotNull public static final String DEAD_KEY = "ll_dead";
    @NotNull public static final String FIRST_JOIN_KEY = "first_join";
    @NotNull public static final String ITEM_KEY = "ll_item";

    @NotNull private final LimitedLives plugin;
    @NotNull private final OfflinePlayer offline;
    @NotNull private final StringData data;

    public PlayerManager(@NotNull LimitedLives plugin, @NotNull OfflinePlayer offline) {
        this.plugin = plugin;
        this.offline = offline;
        this.data = new StringData(plugin, offline);
    }

    public int getLives() {
        final String livesString = data.get(LIVES_KEY);
        if (livesString != null) try {
            return Integer.parseInt(livesString);
        } catch (final NumberFormatException e) {
            AnnoyingPlugin.log(Level.WARNING, "&cRemoving invalid lives from &4" + offline.getName() + "&c: &4" + livesString);
            data.remove(LIVES_KEY);
        }
        return plugin.config.lives.def;
    }

    public int getMaxLives() {
        final Player online = offline.getPlayer();
        return online != null ? BukkitUtility.getPermissionValue(online, "limitedlives.max.").orElse(plugin.config.lives.max) : plugin.config.lives.max;
    }

    /**
     * {@link #getMaxLives()} - {@link #getLives()}
     *
     * @return  the amount of deaths the player has
     */
    public int getDeaths() {
        return getMaxLives() - getLives();
    }

    public long getGraceLeft() {
        if (!plugin.config.gracePeriod.enabled) return 0;
        final String firstJoin = data.get(FIRST_JOIN_KEY);
        if (firstJoin != null) try {
            return plugin.config.gracePeriod.duration - (System.currentTimeMillis() - Long.parseLong(firstJoin));
        } catch (final NumberFormatException e) {
            AnnoyingPlugin.log(Level.WARNING, "&cRemoved invalid " + FIRST_JOIN_KEY + " value for &4" + offline.getName() + "&c: &4" + firstJoin, e);
            data.remove(FIRST_JOIN_KEY);
        }
        return 0;
    }

    public boolean hasGrace() {
        return getGraceLeft() > 0;
    }

    @NotNull
    public Optional<Integer> setLives(int amount) {
        if (amount < plugin.config.lives.min || amount > getMaxLives()) return Optional.empty();
        final int oldLives = getLives();
        data.set(LIVES_KEY, amount);
        if (oldLives <= plugin.config.lives.min && amount > plugin.config.lives.min) revive();
        if (amount == plugin.config.lives.min) kill(null);
        return Optional.of(amount);
    }

    @NotNull
    public Optional<Integer> addLives(int amount) {
        final int oldLives = getLives();
        final int newLives = oldLives + amount;
        if (newLives > getMaxLives()) return Optional.empty();
        data.set(LIVES_KEY, newLives);
        if (oldLives <= plugin.config.lives.min && newLives > plugin.config.lives.min) revive();
        return Optional.of(newLives);
    }

    @NotNull
    public Optional<Integer> removeLives(int amount, @Nullable Player killer) {
        int newLives = getLives() - amount;
        if (newLives < plugin.config.lives.min) return Optional.empty();
        data.set(LIVES_KEY, newLives);
        if (newLives == plugin.config.lives.min) kill(killer);
        return Optional.of(newLives);
    }

    @NotNull
    public Optional<Integer> withdrawLives(@NotNull Player sender, int amount) {
        if (plugin.config.obtaining.crafting.recipe == null || getLives() <= amount) return Optional.empty();
        final ItemStack item = plugin.config.obtaining.crafting.recipe.getResult();
        item.setAmount(amount);
        sender.getInventory().addItem(item);
        return removeLives(amount, null);
    }

    private void revive() {
        data.remove(DEAD_KEY);
        final Player online = offline.getPlayer();
        if (online != null) dispatchCommands(plugin.config.commands.revive, online, null);
    }

    private void kill(@Nullable Player killer) {
        data.set(DEAD_KEY, killer != null ? killer.getUniqueId().toString() : "null");
        final Player online = offline.getPlayer();
        if (online != null) dispatchCommands(plugin.config.commands.punishment.death, online, killer);
    }

    public static void dispatchCommands(@NotNull List<String> commands, @NotNull Player player, @Nullable OfflinePlayer killer) {
        for (String command : commands) {
            if (command.contains("%killer%")) {
                if (killer == null) continue;
                command = command.replace("%killer%", killer.getName());
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }
}
