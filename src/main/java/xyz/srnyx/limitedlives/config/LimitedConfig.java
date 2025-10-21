package xyz.srnyx.limitedlives.config;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Recipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.data.ItemData;
import xyz.srnyx.annoyingapi.file.AnnoyingResource;
import xyz.srnyx.annoyingapi.libs.javautilities.manipulation.Mapper;

import xyz.srnyx.limitedlives.LimitedLives;
import xyz.srnyx.limitedlives.managers.player.PlayerManager;

import java.time.Duration;
import java.util.*;
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
                def = Mapper.toEnum(config.getString("keep-inventory.actions.default"), KeepInventoryAction.class).orElse(KeepInventoryAction.KEEP);

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
                    Mapper.toEnum(config.getString("keep-inventory.actions." + key), KeepInventoryAction.class)
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
        @NotNull public final Duration duration = Duration.ofSeconds(config.getInt("grace-period.duration", 60));
        @NotNull public final Set<GracePeriodTrigger> triggers = new HashSet<>();
        @NotNull public final Set<String> bypassCauses = getDamageCauses(config.getStringList("grace-period.bypass-causes"));
        @NotNull public final Set<String> disabledDamageCauses = getDamageCauses(config.getStringList("grace-period.disabled-damage-causes"));

        public GracePeriod() {
            for (final String string : config.getStringList("grace-period.triggers")) Mapper.toEnum(string, GracePeriodTrigger.class).ifPresent(triggers::add);
        }
    }

    public class Commands {
        @NotNull public final Punishment punishment = new Punishment();
        @NotNull public final List<String> revive = config.getStringList("commands.revive");

        public class Punishment {
            @NotNull private static final String COMMANDS_PUNISHMENT_RESPAWN = "commands.punishment.respawn";

            @NotNull public final List<String> death = config.getStringList("commands.punishment.death");
            @NotNull public final List<String> respawn = config.getStringList(COMMANDS_PUNISHMENT_RESPAWN);

            public Punishment() {
                // Folia check for respawn commands
                if (AnnoyingPlugin.FOLIA && !respawn.isEmpty()) AnnoyingPlugin.log(Level.WARNING, "&c&lThe respawn punishment commands are not supported on Folia! &cPlease enable the doImmediateRespawn gamerule and use death commands instead.\n&c&oTo disable this message, set &4&o" + COMMANDS_PUNISHMENT_RESPAWN + "&c&o to &4&o[]&c&o in &4&oconfig.yml");
            }
        }
    }

    public class Obtaining {
        public final boolean stealing = config.getBoolean("obtaining.stealing", true);
        @NotNull public final Crafting crafting = new Crafting();

        public class Crafting {
            @NotNull private static final String OBTAINING_CRAFTING_TRIGGERS = "obtaining.crafting.triggers";

            public final int amount = config.getInt("obtaining.crafting.amount", 1);
            @NotNull public final Set<CraftingTrigger> triggers = new HashSet<>();
            @NotNull public final Duration cooldown = Duration.ofMillis(config.getLong("obtaining.crafting.cooldown", 500));
            @Nullable public final Recipe recipe = config.getBoolean("obtaining.crafting.enabled", true) ? config.getRecipe("obtaining.crafting.recipe", item -> new ItemData(config.plugin, item).setChain(PlayerManager.ITEM_KEY, true).target, "life").orElse(null) : null;

            public Crafting() {
                if (config.isSet(OBTAINING_CRAFTING_TRIGGERS)) {
                    for (final String string : config.getStringList(OBTAINING_CRAFTING_TRIGGERS)) Mapper.toEnum(string, CraftingTrigger.class).ifPresent(triggers::add);
                } else {
                    triggers.add(CraftingTrigger.CONSUME);
                }
            }
        }
    }

    public class WorldsBlacklist {
        @NotNull private static final String WORLDS_BLACKLIST_AFFECTED_FEATURES = "worlds-blacklist.affected-features";

        @NotNull public final Set<String> list = config.getStringList("worlds-blacklist.list").stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        public final boolean actAsWhitelist = config.getBoolean("worlds-blacklist.act-as-whitelist", false);
        private final Set<Feature> affectedFeatures = new HashSet<>();

        public WorldsBlacklist() {
            if (config.isSet(WORLDS_BLACKLIST_AFFECTED_FEATURES)) {
                for (final String string : config.getStringList(WORLDS_BLACKLIST_AFFECTED_FEATURES)) Mapper.toEnum(string, Feature.class).ifPresent(affectedFeatures::add);
            } else {
                affectedFeatures.addAll(Arrays.asList(Feature.values()));
            }
        }

        public boolean isWorldEnabled(@NotNull World world, @NotNull Feature feature) {
            final boolean inList = list.contains(world.getName().toLowerCase());
            final boolean affectsFeature = affectedFeatures.contains(feature);
            return actAsWhitelist
                    ? inList && affectsFeature
                    : !inList || !affectsFeature;
        }
    }
}
