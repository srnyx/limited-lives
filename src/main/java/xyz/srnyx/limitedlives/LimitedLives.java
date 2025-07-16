package xyz.srnyx.limitedlives;

import org.bukkit.Bukkit;

import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.PluginPlatform;

import xyz.srnyx.limitedlives.config.LimitedConfig;
import xyz.srnyx.limitedlives.listeners.PlayerListener;
import xyz.srnyx.limitedlives.managers.PlaceholderManager;
import xyz.srnyx.limitedlives.managers.WorldGuardManager;
import xyz.srnyx.limitedlives.managers.player.PlayerManager;

import java.io.File;
import java.util.logging.Level;


public class LimitedLives extends AnnoyingPlugin {
    public LimitedConfig config;
    @Nullable public final WorldGuardManager worldGuard;

    public LimitedLives() {
        options
                .pluginOptions(pluginOptions -> pluginOptions.updatePlatforms(new PluginPlatform.Multi(
                        PluginPlatform.modrinth("LvTKDASD"),
                        PluginPlatform.hangar(this),
                        PluginPlatform.spigot("109078"))))
                .bStatsOptions(bStatsOptions -> bStatsOptions.id(18304))
                .dataOptions(dataOptions -> dataOptions
                        .enabled(true)
                        .entityDataColumns(
                                PlayerManager.LIVES_KEY,
                                PlayerManager.DEAD_KEY,
                                PlayerManager.GRACE_START_KEY))
                .registrationOptions
                .toRegister(new PlayerListener(this))
                .papiExpansionToRegister(() -> new PlaceholderManager(this))
                .automaticRegistration.packages("xyz.srnyx.limitedlives.commands");

        // Register WorldGuardManager (needs to happen on load before WorldGuard enables)
        WorldGuardManager worldGuardManager = null;
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) try {
            worldGuardManager = new WorldGuardManager();
        } catch (final Exception e) {
            AnnoyingPlugin.log(Level.WARNING, "&cFailed to register WorldGuard flag!", e);
        }
        worldGuard = worldGuardManager;
    }

    @Override
    public void enable() {
        reload();
        if (config.obtaining.crafting.recipe != null) Bukkit.addRecipe(config.obtaining.crafting.recipe);
    }

    @Override
    public void reload() {
        // Load config
        config = new LimitedConfig(this);
        // Store WorldGuard RegionContainer (needs to happen on enable after WorldGuard enables)
        if (worldGuard != null) worldGuard.storeRegionContainer();
        // Detect very old data (data/data.yml, 2.0.1 and lower)
        final File oldDataFile = new File(getDataFolder(), "data/data.yml");
        if (oldDataFile.exists()) log(Level.SEVERE, "&c&lOld data detected!&c To keep your old data, please update to &43.0.1&c FIRST and then to &4" + getDescription().getVersion() + "&c! &oIf this is incorrect, delete &4&o" + oldDataFile.getPath());
    }
}
