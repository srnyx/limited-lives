package xyz.srnyx.limitedlives;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.file.AnnoyingResource;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class LimitedConfig {
    public int livesDefault = 5;
    public int livesMax = 10;
    public int livesMin = 0;
    @NotNull public final Set<EntityDamageEvent.DamageCause> deathCauses;
    public final boolean stealing;
    @NotNull public List<String> punishmentCommandsDeath = Collections.emptyList();
    @NotNull public List<String> punishmentCommandsRespawn = Collections.emptyList();

    public LimitedConfig(@NotNull LimitedLives plugin) {
        final AnnoyingResource config = new AnnoyingResource(plugin, "config.yml");

        this.deathCauses = config.getStringList("death-causes").stream()
                .map(EntityDamageEvent.DamageCause::valueOf)
                .collect(Collectors.toSet());
        this.stealing = config.getBoolean("stealing");

        // punishmentCommands
        final ConfigurationSection punishmentCommands = config.getConfigurationSection("punishment-commands");
        if (punishmentCommands != null) {
            this.punishmentCommandsDeath = punishmentCommands.getStringList("death");
            this.punishmentCommandsRespawn = punishmentCommands.getStringList("respawn");
        }

        // lives
        final ConfigurationSection lives = config.getConfigurationSection("lives");
        if (lives != null) {
            this.livesDefault = lives.getInt("default", 5);
            this.livesMax = lives.getInt("max", 10);
            this.livesMin = lives.getInt("min", 0);
        }
    }
}
