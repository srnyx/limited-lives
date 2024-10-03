package xyz.srnyx.limitedlives;

import org.bukkit.Bukkit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.PluginPlatform;

import xyz.srnyx.limitedlives.listeners.PlayerListener;

import java.io.File;
import java.util.logging.Level;


public class LimitedLives extends AnnoyingPlugin {
    @NotNull public LimitedConfig config = new LimitedConfig(this);
    @Nullable public WorldGuardManager worldGuard = null;

    public LimitedLives() {
        options
                .pluginOptions(pluginOptions -> pluginOptions.updatePlatforms(new PluginPlatform.Multi(
                        PluginPlatform.modrinth("limited-lives"),
                        PluginPlatform.hangar(this),
                        PluginPlatform.spigot("109078"))))
                .bStatsOptions(bStatsOptions -> bStatsOptions.id(18304))
                .dataOptions(dataOptions -> dataOptions
                        .enabled(true)
                        .entityDataColumns(
                                PlayerManager.LIVES_KEY,
                                PlayerManager.DEAD_KEY,
                                PlayerManager.FIRST_JOIN_KEY))
                .registrationOptions
                .toRegister(new PlayerListener(this))
                .papiExpansionToRegister(() -> new LimitedPlaceholders(this))
                .automaticRegistration.packages("xyz.srnyx.limitedlives.commands");
    }

    @Override
    public void enable() {
        if (config.obtaining.crafting.recipe != null) Bukkit.addRecipe(config.obtaining.crafting.recipe);
    }

    @Override
    public void reload() {
        // Load config
        config = new LimitedConfig(this);
        // Register WorldGuardManager
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) worldGuard = new WorldGuardManager();
        // Detect very old data (data/data.yml)
        final File oldDataFile = new File(getDataFolder(), "data/data.yml");
        if (oldDataFile.exists()) log(Level.SEVERE, "&c&lOld data detected!&c To keep your old data, please update to &43.0.1&c FIRST and then to &4" + getDescription().getVersion() + "&c! &oIf this is incorrect, delete &4&o" + oldDataFile.getPath());
    }
}
