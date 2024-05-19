package xyz.srnyx.limitedlives;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.PluginPlatform;
import xyz.srnyx.annoyingapi.file.AnnoyingData;
import xyz.srnyx.annoyingapi.file.AnnoyingFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;


public class LimitedLives extends AnnoyingPlugin {
    @NotNull public LimitedConfig config = new LimitedConfig(this);
    @Nullable public WorldGuardManager worldGuard = null;

    // OLD DATA
    @Nullable public AnnoyingData oldData = new AnnoyingData(this, "data.yml", new AnnoyingFile.Options<>().canBeEmpty(false));
    /**
     * player, lives
     */
    @Nullable public Map<UUID, Integer> oldLivesData = new HashMap<>();
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
                .dataOptions(dataOptions -> dataOptions
                        .enabled(true)
                        .entityDataColumns(
                                PlayerManager.LIVES_KEY,
                                PlayerManager.DEAD_KEY,
                                PlayerManager.FIRST_JOIN_KEY))
                .registrationOptions
                .toRegister(this, PlayerListener.class)
                .papiExpansionToRegister(() -> new LimitedPlaceholders(this))
                .automaticRegistration.packages("xyz.srnyx.limitedlives.commands");

        // Register WorldGuardManager
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) worldGuard = new WorldGuardManager();

        // oldLives
        final ConfigurationSection livesSection = oldData.getConfigurationSection("lives");
        if (livesSection != null) for (final String key : livesSection.getKeys(false)) try {
            oldLivesData.put(UUID.fromString(key), livesSection.getInt(key));
        } catch (final IllegalArgumentException e) {
            log(Level.WARNING, "&cInvalid UUID in &4data.yml&c: &4" + key);
        }
        if (oldLivesData.isEmpty()) oldLivesData = null;

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
        if (oldLivesData != null || oldDeadPlayers != null) return;
        oldData.delete(true);
        oldData = null;
    }

    @Override
    public void enable() {
        if (config.obtaining.crafting.recipe != null) Bukkit.addRecipe(config.obtaining.crafting.recipe);
    }

    @Override
    public void reload() {
        config = new LimitedConfig(this);
    }
}
