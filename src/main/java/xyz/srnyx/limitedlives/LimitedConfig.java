package xyz.srnyx.limitedlives;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Recipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.data.ItemData;
import xyz.srnyx.annoyingapi.file.AnnoyingResource;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;


public class LimitedConfig {
    @NotNull private final AnnoyingResource config;
    @NotNull public final Lives lives;
    @NotNull public final Set<String> deathCauses;
    @NotNull public final KeepInventory keepInventory;
    @NotNull public final GracePeriod gracePeriod;
    @NotNull public final Commands commands;
    @NotNull public final Obtaining obtaining;
    @NotNull public final WorldsBlacklist worldsBlacklist;

    public LimitedConfig(@NotNull LimitedLives plugin) {
        config = new AnnoyingResource(plugin, "config.yml");
        lives = new Lives();
        deathCauses = getDamageCauses(config.getStringList("death-causes"));
        keepInventory = new KeepInventory();
        gracePeriod = new GracePeriod();
        commands = new Commands();
        obtaining = new Obtaining();
        worldsBlacklist = new WorldsBlacklist();
    }

    @NotNull
    private static Set<String> getDamageCauses(@NotNull List<String> collection) {
        return collection.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    public class Lives {
        public final int def = config.getInt("lives.default", 5);
        public final int max = config.getInt("lives.max", 10);
        public final int min = config.getInt("lives.min", 0);
    }

    public class KeepInventory {
        public final boolean enabled = config.getBoolean("keep-inventory.enabled", false);
        @NotNull public final Actions actions = new Actions();

        public KeepInventory() {
            // Disable keepInventory in worlds where it is enabled
            if (enabled) Bukkit.getWorlds().stream()
                    .filter(world -> world.getGameRuleValue("keepInventory").equals("true"))
                    .forEach(world -> {
                        AnnoyingPlugin.log(Level.WARNING, "keepInventory is enabled in " + world.getName() + "! The plugin is disabling it to ensure the keep-inventory feature works properly");
                        world.setGameRuleValue("keepInventory", "false");
                    });
        }

        public class Actions {
            @NotNull private final KeepInventoryAction def;
            @NotNull private final Map<Integer, KeepInventoryAction> exact = new HashMap<>();

            public Actions() {
                // def
                def = KeepInventoryAction.fromString(config.getString("keep-inventory.actions.default")).orElse(KeepInventoryAction.KEEP);

                // actions
                final ConfigurationSection section = config.getConfigurationSection("keep-inventory.actions");
                if (section != null) for (final String key : section.getKeys(false)) {
                    if (key.equals("default") || key.equals("first") || key.equals("last")) continue;
                    final int count;
                    try {
                        count = Integer.parseInt(key);
                    } catch (final NumberFormatException e) {
                        AnnoyingPlugin.log(Level.WARNING, "Invalid keep inventory action count: " + key);
                        continue;
                    }
                    KeepInventoryAction.fromString(config.getString("keep-inventory.actions." + key))
                            .ifPresent(keepInventoryAction -> exact.put(count, keepInventoryAction));
                }
            }

            @NotNull
            public KeepInventoryAction getAction(int deaths) {
                final KeepInventoryAction action = exact.get(deaths);
                return action != null ? action : def;
            }
        }
    }

    public class GracePeriod {
        public final boolean enabled = config.getBoolean("grace-period.enabled", false);
        public final int duration = config.getInt("grace-period.duration", 60) * 1000;
        @NotNull public final Set<String> bypassCauses = getDamageCauses(config.getStringList("grace-period.bypass-causes"));
    }

    public class Commands {
        @NotNull public final Punishment punishment = new Punishment();
        @NotNull public final List<String> revive = config.getStringList("commands.revive");

        public class Punishment {
            @NotNull public final List<String> death = config.getStringList("commands.punishment.death");
            @NotNull public final List<String> respawn = config.getStringList("commands.punishment.respawn");
        }
    }

    public class Obtaining {
        public final boolean stealing = config.getBoolean("obtaining.stealing", true);
        @NotNull public final Crafting crafting = new Crafting();

        public class Crafting {
            public final int amount = config.getInt("obtaining.crafting.amount", 1);
            @Nullable public final Recipe recipe = config.getBoolean("obtaining.crafting.enabled", true) ? config.getRecipe("obtaining.crafting.recipe", item -> new ItemData(config.plugin, item).setChain(PlayerManager.ITEM_KEY, true).target, "life").orElse(null) : null;
        }
    }

    public class WorldsBlacklist {
        @NotNull public final Set<String> list = config.getStringList("worlds-blacklist.list").stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        public final boolean actAsWhitelist = config.getBoolean("worlds-blacklist.act-as-whitelist", false);

        public boolean isWorldEnabled(@NotNull World world) {
            return actAsWhitelist == list.contains(world.getName().toLowerCase());
        }
    }

    public enum KeepInventoryAction {
        KEEP(event -> {
            event.getDrops().clear();
            event.setKeepInventory(true);
        }),
        DROP(event -> event.setKeepInventory(false)),
        DESTROY(event -> {
            event.getDrops().clear();
            event.setKeepInventory(false);
        });

        @NotNull public final Consumer<PlayerDeathEvent> consumer;

        KeepInventoryAction(@NotNull Consumer<PlayerDeathEvent> consumer) {
            this.consumer = consumer;
        }

        @NotNull
        public static Optional<KeepInventoryAction> fromString(@Nullable String string) {
            if (string != null) try {
                return Optional.of(valueOf(string.toUpperCase()));
            } catch (final IllegalArgumentException e) {
                AnnoyingPlugin.log(Level.WARNING, "Invalid keep inventory action: " + string);
            }
            return Optional.empty();
        }
    }
}
