package xyz.srnyx.limitedlives;

import org.bukkit.configuration.ConfigurationSection;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.file.AnnoyingResource;


public class LimitedConfig {
    public int livesDefault = 5;
    public int livesMax = 10;
    public int livesMin = 0;

    public LimitedConfig(@NotNull LimitedLives plugin) {
        final AnnoyingResource config = new AnnoyingResource(plugin, "config.yml");
        final ConfigurationSection section = config.getConfigurationSection("lives");
        if (section == null) return;
        livesDefault = section.getInt("default", 5);
        livesMax = section.getInt("max", 10);
        livesMin = section.getInt("min", 0);
    }
}
