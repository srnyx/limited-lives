package xyz.srnyx.limitedlives;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Recipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.file.AnnoyingResource;
import xyz.srnyx.annoyingapi.utility.ItemDataUtility;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;


public class LimitedConfig {
    public final int livesDefault;
    public final int livesMax;
    public final int livesMin;
    @NotNull public final Set<EntityDamageEvent.DamageCause> deathCauses;
    public final boolean stealing;
    @Nullable public final Recipe recipe;
    public final int recipeAmount;
    @NotNull public final List<String> commandsPunishmentDeath;
    @NotNull public final List<String> commandsPunishmentRespawn;
    @NotNull public final List<String> commandsRevive;

    public LimitedConfig(@NotNull LimitedLives plugin) {
        final AnnoyingResource config = new AnnoyingResource(plugin, "config.yml");

        // deathCauses
        deathCauses = config.getStringList("death-causes").stream()
                .map(string -> {
                    try {
                        return EntityDamageEvent.DamageCause.valueOf(string.toUpperCase());
                    } catch (final IllegalArgumentException e) {
                        AnnoyingPlugin.log(Level.WARNING, "Invalid death cause: " + string);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // commandsPunishmentDeath, commandsPunishmentRespawn, & commandsRevive
        final ConfigurationSection commands = config.getConfigurationSection("commands");
        final boolean hasCommands = commands != null;
        final ConfigurationSection punishment = hasCommands ? commands.getConfigurationSection("punishment") : null;
        final boolean hasPunishment = punishment != null;
        commandsPunishmentDeath = hasPunishment ? punishment.getStringList("death") : Collections.emptyList();
        commandsPunishmentRespawn = hasPunishment ? punishment.getStringList("respawn") : Collections.emptyList();
        commandsRevive = hasCommands ? commands.getStringList("revive") : Collections.emptyList();

        // livesDefault, livesMax, & livesMin
        final ConfigurationSection lives = config.getConfigurationSection("lives");
        final boolean hasLives = lives != null;
        livesDefault = hasLives ? lives.getInt("default", 5) : 5;
        livesMax = hasLives ? lives.getInt("max", 10) : 10;
        livesMin = hasLives ? lives.getInt("min", 0) : 0;

        // stealing
        final ConfigurationSection obtaining = config.getConfigurationSection("obtaining");
        final boolean hasObtaining = obtaining != null;
        stealing = hasObtaining && obtaining.getBoolean("stealing", true);

        // recipe
        final ConfigurationSection crafting = hasObtaining ? obtaining.getConfigurationSection("crafting") : null;
        final boolean hasCrafting = crafting != null;
        recipeAmount = hasCrafting ? crafting.getInt("amount", 1) : 1;
        recipe = hasCrafting && crafting.getBoolean("enabled", true) ? config.getRecipe("obtaining.crafting", item -> new ItemDataUtility(plugin, item).set("ll_item", true).item, null, "life") : null;
    }
}
