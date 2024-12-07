package xyz.srnyx.limitedlives.managers.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.data.StringData;
import xyz.srnyx.annoyingapi.utility.BukkitUtility;

import xyz.srnyx.limitedlives.LimitedLives;
import xyz.srnyx.limitedlives.config.GracePeriodTrigger;
import xyz.srnyx.limitedlives.managers.player.exception.LessThanMinLives;
import xyz.srnyx.limitedlives.managers.player.exception.MoreThanMaxLives;
import xyz.srnyx.limitedlives.managers.player.exception.RecipeNotSet;

import java.util.List;
import java.util.logging.Level;


public class PlayerManager {
    @NotNull public static final String LIVES_KEY = "ll_lives";
    @NotNull public static final String DEAD_KEY = "ll_dead";
    @NotNull public static final String GRACE_START_KEY = "grace_start";
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
        final String graceStart = data.get(GRACE_START_KEY);
        if (graceStart == null) return 0;

        // Calculate
        final long graceLeft;
        try {
            graceLeft = plugin.config.gracePeriod.duration - (System.currentTimeMillis() - Long.parseLong(graceStart));
        } catch (final NumberFormatException e) {
            AnnoyingPlugin.log(Level.WARNING, "&cRemoved invalid " + GRACE_START_KEY + " value for &4" + offline.getName() + "&c: &4" + graceStart, e);
            data.remove(GRACE_START_KEY);
            return 0;
        }

        // Return
        if (graceLeft <= 0) {
            data.remove(GRACE_START_KEY);
            return 0;
        }
        return graceLeft;
    }

    public boolean hasGrace() {
        return getGraceLeft() > 0;
    }

    public int setLives(int amount) throws LessThanMinLives, MoreThanMaxLives {
        if (amount < plugin.config.lives.min) throw new LessThanMinLives();
        if (amount > getMaxLives()) throw new MoreThanMaxLives();
        final int oldLives = getLives();
        data.set(LIVES_KEY, amount);
        if (oldLives <= plugin.config.lives.min && amount > plugin.config.lives.min) revive();
        if (amount == plugin.config.lives.min) kill(null);
        return amount;
    }

    public int addLives(int amount) throws MoreThanMaxLives {
        final int oldLives = getLives();
        final int newLives = oldLives + amount;
        if (newLives > getMaxLives()) throw new MoreThanMaxLives();
        data.set(LIVES_KEY, newLives);
        if (oldLives <= plugin.config.lives.min && newLives > plugin.config.lives.min) revive();
        return newLives;
    }

    public int removeLives(int amount, @Nullable Player killer) throws LessThanMinLives {
        int newLives = getLives() - amount;
        if (newLives < plugin.config.lives.min) throw new LessThanMinLives();
        data.set(LIVES_KEY, newLives);
        if (newLives == plugin.config.lives.min) kill(killer);
        return newLives;
    }

    public int withdrawLives(@NotNull Player sender, int amount) throws LessThanMinLives, RecipeNotSet {
        if (plugin.config.obtaining.crafting.recipe == null) throw new RecipeNotSet();
        final ItemStack item = plugin.config.obtaining.crafting.recipe.getResult();
        item.setAmount(amount);
        sender.getInventory().addItem(item);
        return removeLives(amount, null);
    }

    private void revive() {
        data.remove(DEAD_KEY);

        // Start grace period
        if (plugin.config.gracePeriod.triggers.contains(GracePeriodTrigger.REVIVE)) data.set(GRACE_START_KEY, System.currentTimeMillis());

        // Dispatch revive commands
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
