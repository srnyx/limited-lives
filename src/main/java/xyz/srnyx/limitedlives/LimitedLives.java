package xyz.srnyx.limitedlives;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.PluginPlatform;
import xyz.srnyx.annoyingapi.data.EntityData;
import xyz.srnyx.annoyingapi.file.AnnoyingData;
import xyz.srnyx.annoyingapi.file.AnnoyingFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;


public class LimitedLives extends AnnoyingPlugin {
    @NotNull public static final String LIVES_KEY = "ll_lives";
    @NotNull public static final String DEAD_KEY = "ll_dead";
    @NotNull public static final String ITEM_KEY = "ll_item";

    @NotNull public LimitedConfig config = new LimitedConfig(this);
    @Nullable public AnnoyingData oldData = new AnnoyingData(this, "data.yml", new AnnoyingFile.Options<>().canBeEmpty(false));
    /**
     * player, lives
     */
    @Nullable public Map<UUID, Integer> oldLives = new HashMap<>();
    /**
     * player, killer
     */
    @Nullable public Map<UUID, UUID> oldDeadPlayers = new HashMap<>();

    public LimitedLives() {
        options
                .pluginOptions(pluginOptions -> pluginOptions.updatePlatforms(new PluginPlatform.Multi(
                        PluginPlatform.modrinth("limited-lives"),
                        PluginPlatform.hangar(this, "srnyx"),
                        PluginPlatform.spigot("109078"))))
                .bStatsOptions(bStatsOptions -> bStatsOptions.id(18304))
                .registrationOptions
                .automaticRegistration(automaticRegistration -> automaticRegistration.packages(
                        "xyz.srnyx.limitedlives.commands",
                        "xyz.srnyx.limitedlives.listeners"))
                .papiExpansionToRegister(() -> new LimitedPlaceholders(this));

        // oldLives
        final ConfigurationSection livesSection = oldData.getConfigurationSection("lives");
        if (livesSection != null) for (final String key : livesSection.getKeys(false)) try {
            oldLives.put(UUID.fromString(key), livesSection.getInt(key));
        } catch (final IllegalArgumentException e) {
            log(Level.WARNING, "&cInvalid UUID in &4data.yml&c: &4" + key);
        }
        if (oldLives.isEmpty()) oldLives = null;

        // oldDeadPlayers
        final ConfigurationSection deadPlayersSection = oldData.getConfigurationSection("dead-players");
        if (deadPlayersSection != null) for (final String key : deadPlayersSection.getKeys(false)) {
            final UUID player;
            try {
                player = UUID.fromString(key);
            } catch (final IllegalArgumentException e) {
                log(Level.WARNING, "&cInvalid UUID in &4data.yml&c: &4" + key);
                continue;
            }
            UUID killer = null;
            final String killerString = deadPlayersSection.getString(key);
            if (killerString != null) try {
                killer = UUID.fromString(killerString);
            } catch (final IllegalArgumentException e) {
                log(Level.WARNING, "&cInvalid UUID in &4data.yml&c: &4" + killerString);
                continue;
            }
            oldDeadPlayers.put(player, killer);
        }
        if (oldDeadPlayers.isEmpty()) oldDeadPlayers = null;

        // No old data loaded
        if (oldLives != null || oldDeadPlayers != null) return;
        oldData.delete();
        oldData = null;
    }

    @Override
    public void enable() {
        if (config.recipe != null) Bukkit.addRecipe(config.recipe);
    }

    @Override
    public void reload() {
        config = new LimitedConfig(this);
    }

    public int getLives(@NotNull Player player) {
        final String livesString = new EntityData(this, player).get(LIVES_KEY);
        if (livesString != null) try {
            return Integer.parseInt(livesString);
        } catch (final NumberFormatException e) {
            log(Level.WARNING, "&cInvalid lives for &4" + player.getName());
        }
        return config.livesDefault;
    }

    @Nullable
    public Integer setLives(@NotNull Player player, int amount) {
        if (amount > config.livesMax || amount < config.livesMin) return null;
        final int oldLives = getLives(player);
        new EntityData(this, player).set(LIVES_KEY, amount);
        if (oldLives <= config.livesMin && amount > config.livesMin) revive(player);
        if (amount == config.livesMin) kill(player, null);
        return amount;
    }

    @Nullable
    public Integer addLives(@NotNull Player player, int amount) {
        final int oldLives = getLives(player);
        final int newLives = oldLives + amount;
        if (newLives > config.livesMax) return null;
        new EntityData(this, player).set(LIVES_KEY, newLives);
        if (oldLives <= config.livesMin && newLives > config.livesMin) revive(player);
        return newLives;
    }

    @Nullable
    public Integer removeLives(@NotNull Player player, int amount, @Nullable Player killer) {
        int newLives = getLives(player) - amount;
        if (newLives < config.livesMin) return null;
        new EntityData(this, player).set(LIVES_KEY, newLives);
        if (newLives == config.livesMin) kill(player, killer);
        return newLives;
    }

    private void revive(@NotNull Player player) {
        new EntityData(this, player).remove(DEAD_KEY);
        dispatchCommands(config.commandsRevive, player, null);
    }

    private void kill(@NotNull Player player, @Nullable Player killer) {
        new EntityData(this, player).set(DEAD_KEY, killer != null ? killer.getUniqueId().toString() : null);
        dispatchCommands(config.commandsPunishmentDeath, player, killer);
    }

    public static void dispatchCommands(@NotNull List<String> commands, @NotNull OfflinePlayer player, @Nullable OfflinePlayer killer) {
        for (String command : commands) {
            command = command.replace("%player%", player.getName());
            if (killer == null && command.contains("%killer%")) continue;
            if (killer != null) command = command.replace("%killer%", killer.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
}
