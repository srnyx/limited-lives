package xyz.srnyx.limitedlives;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.file.AnnoyingResource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class LimitedConfig {
    public int livesDefault = 5;
    public int livesMax = 10;
    public int livesMin = 0;
    @NotNull public final Set<EntityDamageEvent.DamageCause> deathCauses;
    public final boolean stealing;
    @NotNull public final List<String> punishmentCommands;

    public LimitedConfig(@NotNull LimitedLives plugin) {
        final AnnoyingResource config = new AnnoyingResource(plugin, "config.yml");

        this.deathCauses = config.getStringList("death-causes").stream()
                .map(EntityDamageEvent.DamageCause::valueOf)
                .collect(Collectors.toSet());
        this.stealing = config.getBoolean("stealing");
        this.punishmentCommands = config.getStringList("punishment-commands");

        // lives
        final ConfigurationSection section = config.getConfigurationSection("lives");
        if (section == null) return;
        this.livesDefault = section.getInt("default", 5);
        this.livesMax = section.getInt("max", 10);
        this.livesMin = section.getInt("min", 0);
    }
}
