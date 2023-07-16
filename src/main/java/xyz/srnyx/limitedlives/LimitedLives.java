package xyz.srnyx.limitedlives;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.PluginPlatform;
import xyz.srnyx.annoyingapi.file.AnnoyingData;

import java.util.*;
import java.util.logging.Level;


public class LimitedLives extends AnnoyingPlugin {
    @NotNull public LimitedConfig config = new LimitedConfig(this);
    @NotNull public final AnnoyingData data = new AnnoyingData(this, "data.yml");
    /**
     * player, lives
     */
    @NotNull public final Map<UUID, Integer> lives = new HashMap<>();
    /**
     * player, killer
     */
    @NotNull public final Map<UUID, UUID> deadPlayers = new HashMap<>();

    public LimitedLives() {
        options
                .bStatsOptions(bStatsOptions -> bStatsOptions.id(18304))
                .pluginOptions(pluginOptions -> pluginOptions.updatePlatforms(new PluginPlatform.Multi(
                        PluginPlatform.modrinth("limited-lives"),
                        PluginPlatform.hangar(this, "srnyx"),
                        PluginPlatform.spigot("109078"))))
                .registrationOptions(registrationOptions -> registrationOptions
                        .automaticRegistration(automaticRegistration -> automaticRegistration.packages(
                                "xyz.srnyx.limitedlives.commands",
                                "xyz.srnyx.limitedlives.listeners"))
                        .papiExpansionToRegister(() -> new LimitedPlaceholders(this)));

        // lives
        final ConfigurationSection livesSection = data.getConfigurationSection("lives");
        if (livesSection != null) for (final String key : livesSection.getKeys(false)) try {
            lives.put(UUID.fromString(key), livesSection.getInt(key));
        } catch (final IllegalArgumentException e) {
            log(Level.WARNING, "&cInvalid UUID in &4data.yml&c: &4" + key);
        }

        // deadPlayers
        final ConfigurationSection deadPlayersSection = data.getConfigurationSection("dead-players");
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
            deadPlayers.put(player, killer);
        }
    }

    @Override
    public void enable() {
        if (config.recipe != null) Bukkit.addRecipe(config.recipe);
    }

    @Override
    public void disable() {
        data.set("lives", lives.entrySet().stream().collect(HashMap::new, (m, e) -> m.put(e.getKey().toString(), e.getValue()), HashMap::putAll));
        data.setSave("dead-players", deadPlayers.entrySet().stream().collect(HashMap::new, (m, e) -> {
            final UUID killer = e.getValue();
            m.put(e.getKey().toString(), killer == null ? null : killer.toString());
        }, HashMap::putAll));
    }

    @Override
    public void reload() {
        config = new LimitedConfig(this);
        disable();
    }

    public int getLives(@NotNull UUID uuid) {
        return lives.getOrDefault(uuid, config.livesDefault);
    }

    @Nullable
    public Integer setLives(@NotNull OfflinePlayer player, int amount) {
        if (amount > config.livesMax || amount < config.livesMin) return null;
        final UUID uuid = player.getUniqueId();
        final int oldLives = getLives(uuid);
        lives.put(uuid, amount);
        if (oldLives <= config.livesMin && amount > config.livesMin) revive(player);
        if (amount == config.livesMin) kill(player, null);
        return amount;
    }

    @Nullable
    public Integer addLives(@NotNull OfflinePlayer player, int amount) {
        final UUID uuid = player.getUniqueId();
        final int oldLives = getLives(uuid);
        final int newLives = oldLives + amount;
        if (newLives > config.livesMax) return null;
        lives.put(uuid, newLives);
        if (oldLives <= config.livesMin && newLives > config.livesMin) revive(player);
        return newLives;
    }

    @Nullable
    public Integer removeLives(@NotNull OfflinePlayer player, int amount, @Nullable Player killer) {
        final UUID uuid = player.getUniqueId();
        int newLives = getLives(uuid) - amount;
        if (newLives < config.livesMin) return null;
        lives.put(uuid, newLives);
        if (newLives == config.livesMin) kill(player, killer);
        return newLives;
    }

    private void revive(@NotNull OfflinePlayer player) {
        deadPlayers.remove(player.getUniqueId());
        dispatchCommands(config.commandsRevive, player, null);
    }

    private void kill(@NotNull OfflinePlayer player, @Nullable Player killer) {
        deadPlayers.put(player.getUniqueId(), killer != null && player != killer ? killer.getUniqueId() : null);
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
