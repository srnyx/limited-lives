package xyz.srnyx.limitedlives.config;

import com.cryptomorin.xseries.XGameRule;
import com.cryptomorin.xseries.XMaterial;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.annotation.Serdes;
import eu.okaeri.configs.serdes.commons.duration.DurationSpec;
import eu.okaeri.validator.annotation.NotNull;
import eu.okaeri.validator.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.ServerSoftware;
import xyz.srnyx.annoyingapi.file.okaeri.RootConfig;
import xyz.srnyx.annoyingapi.file.okaeri.SubConfig;
import xyz.srnyx.annoyingapi.file.okaeri.serdes.recipe.spec.RecipeSpec;
import xyz.srnyx.annoyingapi.reflection.org.bukkit.inventory.RefShapedRecipe;
import xyz.srnyx.annoyingapi.stats.Stat;
import xyz.srnyx.annoyingapi.utility.BukkitUtility;
import xyz.srnyx.limitedlives.LimitedLives;
import xyz.srnyx.limitedlives.config.damagecause.DamageCauseWrapper;
import xyz.srnyx.limitedlives.config.serdes.KeepInventoryActionsSerializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;


@Header("DOCUMENTATION: https://annoying-api.srnyx.com/wiki/File-objects")
public class LimitedConfig extends RootConfig {
    @Comment
    @Comment
    @Comment
    @NotNull public Lives lives = new Lives(this);

    @Comment
    @Comment
    @Comment
    @Comment("The causes that will result in a player to losing a life, leave empty for all causes")
    @Comment("Available causes:")
    @Comment("- https://srnyx.com/docs/spigot/org/bukkit/event/entity/EntityDamageEvent.DamageCause.html")
    @Comment("- PLAYER_ATTACK: Death caused by another player (PVP)")
    @Stat
    @NotNull public Set<DamageCauseWrapper> death_causes = Set.of();

    @Comment
    @Comment
    @Comment
    @Comment("A list of worlds where the plugin won't be enabled")
    @NotNull public WorldsBlacklist worlds_blacklist = new WorldsBlacklist(this);

    @Comment
    @Comment
    @Comment
    @Comment("THE keep_inventory GAMERULE MUST BE SET TO false FOR THIS TO WORK (the plugin will disable it if it's enabled)!")
    @Comment("If it isn't, some players' inventories may be permanently lost!")
    @NotNull public KeepInventory keep_inventory = new KeepInventory(this);

    @Comment
    @Comment
    @Comment
    @Comment("A period of time after a player joins the server and/or is revived where they won't lose lives")
    @NotNull public GracePeriod grace_period = new GracePeriod(this);

    @Comment
    @Comment
    @Comment
    @NotNull public Commands commands = new Commands(this);

    @Comment
    @Comment
    @Comment
    @Comment("Different ways of obtaining lives")
    @NotNull public Obtaining obtaining;


    @org.jetbrains.annotations.NotNull protected transient final LimitedLives plugin;

    public LimitedConfig(@org.jetbrains.annotations.NotNull LimitedLives plugin) {
        this.plugin = plugin;

        this.obtaining = new Obtaining(this);
    }

    @Override
    public void onLoad() {
        // keep_inventory enabled: disable keep_inventory in worlds where enabled
        if (keep_inventory.enabled) Bukkit.getWorlds().stream()
                .filter(world -> Boolean.TRUE.equals(XGameRule.KEEP_INVENTORY.getValue(world)))
                .forEach(world -> {
                    AnnoyingPlugin.log(Level.WARNING, "keep_inventory is enabled in " + world.getName() + "! The plugin is disabling it to ensure the keep-inventory feature works properly");
                    XGameRule.KEEP_INVENTORY.setValue(world, false);
                });

        // Folia check for respawn commands
        if (ServerSoftware.SOFTWARE.hasFolia() && !commands.punishment.respawn.isEmpty()) {
            AnnoyingPlugin.log(Level.WARNING, "&c&lThe respawn punishment commands are not supported on Folia! &cPlease enable the " + XGameRule.KEEP_INVENTORY.name() + " gamerule and use death commands instead.\n&c&oTo disable this message, set &4&ocommands.punishment.respawn&c&o to &4&o[]&c&o in &4&oconfig.yml");
        }
    }

    public static class Lives extends SubConfig<LimitedConfig, LimitedConfig> {
        public Lives(@org.jetbrains.annotations.NotNull LimitedConfig config) {
            super(config);
        }

        @Comment("The amount of lives a player starts with")
        @CustomKey("default")
        @Stat
        public int def = 5;

        @Comment("The maximum amount of lives a player can have")
        @Stat
        public int max = 10;

        @Comment("The amount of lives that triggers the punishment commands")
        @Stat
        public int min = 0;
    }

    public static class WorldsBlacklist extends SubConfig<LimitedConfig, LimitedConfig> {
        public WorldsBlacklist(@org.jetbrains.annotations.NotNull LimitedConfig config) {
            super(config);
        }

        @Stat
        @NotNull public Set<String> list = Collections.emptySet();

        @Comment
        @Comment("If true, the list of worlds above will act as a whitelist, meaning only those worlds will have the plugin enabled")
        @Stat
        public boolean act_as_whitelist = false;

        @Comment
        @Comment("The features that will be affected by the worlds blacklist/whitelist")
        @Comment("Example: \"world_1\" is in the list and only \"LIFE_LOSS\" is specified below. If act-as-whitelist is false, players in \"world_1\" will not lose lives, but all other features will still work. If act-as-whitelist is true, players in \"world_1\" will only be affected by the \"LIFE_LOSS\" feature, and all other features will NOT work.")
        @Comment("Available features:")
        @Comment("- COMMANDS: The /lives commands for the plugin")
        @Comment("- LIFE_LOSS: Players losing lives")
        @Comment("- LIFE_USE: Players using lives (ex: right-clicking the life item)")
        @Comment("- OBTAINING_STEALING: Gaining lives from killing players")
        @Comment("- OBTAINING_CRAFTING: Ability to craft the life item (for using it, see LIFE_USE)")
        @Comment("- KEEP_INVENTORY: The keep-inventory feature (so will just use the world's keepInventory gamerule setting)")
        @Stat
        @NotNull public Set<Feature> affected_features = Set.of(Feature.values());

        public boolean isWorldEnabled(@org.jetbrains.annotations.NotNull World world, @org.jetbrains.annotations.NotNull Feature feature) {
            final String worldName = world.getName().toLowerCase();
            final boolean inList = list.stream()
                    .map(String::toLowerCase)
                    .anyMatch(listed -> listed.equals(worldName));
            final boolean affectsFeature = affected_features.contains(feature);
            return act_as_whitelist
                    ? inList && affectsFeature
                    : !inList || !affectsFeature;
        }
    }

    public static class KeepInventory extends SubConfig<LimitedConfig, LimitedConfig> {
        public KeepInventory(@org.jetbrains.annotations.NotNull LimitedConfig config) {
            super(config);
        }

        @Comment("This will toggle this entire feature on or off")
        @Stat
        public boolean enabled = false;

        @Comment
        @Comment("The actions that will be taken when a player dies for the X time (death count is calculated using: max lives - current lives)")
        @Comment("Available actions:")
        @Comment("- KEEP: Keep the player's inventory, as if keepInventory was true")
        @Comment("- DROP: Drop the player's inventory on the ground, as if keepInventory was false")
        @Comment("- DESTROY: Destroy all items in the player's inventory")
        @Comment(" ")
        @Comment("EXAMPLE:")
        @Comment("actions:")
        @Comment("  default: KEEP # Default action if not caught by one below")
        @Comment("  1: DROP # This would drop the player's inventory on their 1st death")
        @Comment("  5: DROP # This would drop the player's inventory on their 5th death")
        @Comment("  10: DESTROY # This would destroy the player's inventory on their 10th death")
        @Serdes(serializer = KeepInventoryActionsSerializer.class) @Stat
        @NotNull public KeepInventoryActions actions = new KeepInventoryActions();
    }

    public static class GracePeriod extends SubConfig<LimitedConfig, LimitedConfig> {
        public GracePeriod(@org.jetbrains.annotations.NotNull LimitedConfig config) {
            super(config);
        }

        public boolean enabled = false;

        @Comment
        @Comment("The duration of the grace period")
        @DurationSpec(fallbackUnit = ChronoUnit.SECONDS)
        @Stat
        @NotNull public Duration duration = Duration.ofMinutes(1);

        @Comment
        @Comment("When players should be given the grace period")
        @Comment("Available options:")
        @Comment("- FIRST_JOIN: When a player joins the server for the first time")
        @Comment("- JOIN: When a player joins the server (overrules FIRST_JOIN)")
        @Comment("- REVIVE: When a player is revived")
        @Stat
        @NotNull public Set<GracePeriodTrigger> triggers = Set.of(
                GracePeriodTrigger.FIRST_JOIN,
                GracePeriodTrigger.REVIVE);

        @Comment
        @Comment("Death causes that will bypass the grace period so the player still loses a life")
        @Comment("Available causes:")
        @Comment("- https://srnyx.com/docs/spigot/org/bukkit/event/entity/EntityDamageEvent.DamageCause.html")
        @Comment("- PLAYER_ATTACK: Death caused by another player (PVP)")
        @Stat
        @NotNull public Set<DamageCauseWrapper> bypass_causes = Set.of();

        @Comment
        @Comment("Damage causes that will be cancelled while a player is in the grace period")
        @Comment("Available causes:")
        @Comment("- https://srnyx.com/docs/spigot/org/bukkit/event/entity/EntityDamageEvent.DamageCause.html")
        @Comment("- PLAYER_ATTACK: Damage caused when a player attacks another player (PVP)")
        @Stat
        @NotNull public Set<DamageCauseWrapper> disabled_damage_causes = Set.of();
    }

    public static class Commands extends SubConfig<LimitedConfig, LimitedConfig> {
        public Commands(@org.jetbrains.annotations.NotNull LimitedConfig config) {
            super(config);
        }

        @Comment("The commands that will be executed when a player loses all their lives (executed by the console).")
        @Comment("It's best to have the do_immediate_respawn gamerule set to true and then only use death commands.")
        @Comment("%player% - The player that lost all their lives")
        @Comment("%killer% - The player that got the final kill on %player%. If %player% died from a non-player cause, any commands that use %killer% will be ignored")
        @NotNull public Punishment punishment = new Punishment(this);

        @Comment
        @Comment("The commands that will be executed when a player goes from the minimum lives to above the minimum lives (executed by the console)")
        @Comment("%player% - The player that gained lives")
        @Stat(sizeOnly = true)
        @NotNull public List<String> revive = List.of("gamemode survival %player%");

        public static class Punishment extends SubConfig<LimitedConfig, Commands> {
            public Punishment(@org.jetbrains.annotations.NotNull Commands commands) {
                super(commands);
            }

            @Comment("Executed right when the player dies and/or when they lose all their lives from a command (ex: /lives remove)")
            @Stat(sizeOnly = true)
            @NotNull public List<String> death = List.of();

            @Comment
            @Comment("Executed when the player respawns")
            @Comment("FOLIA: These do not work on Folia servers! Please enable the do_immediate_respawn gamerule and use death commands instead")
            @Stat(sizeOnly = true)
            @NotNull public List<String> respawn = List.of("gamemode spectator %player%");
        }
    }

    public static class Obtaining extends SubConfig<LimitedConfig, LimitedConfig> {
        public Obtaining(@org.jetbrains.annotations.NotNull LimitedConfig config) {
            super(config);
        }

        @Comment("Whether to enable killers gaining a life when they kill a player")
        @Stat
        public boolean stealing = true;

        @Comment
        @Comment
        @NotNull public Crafting crafting = new Crafting(this);

        public static class Crafting extends SubConfig<LimitedConfig, Obtaining> {
            @org.jetbrains.annotations.NotNull private static final String RECIPE_NAME = "life";


            @Comment("Whether to enable crafting an item that can be used to gain lives")
            @Stat
            public boolean enabled = true;

            @Comment
            @Comment("The amount of lives that will be gained when the item is used")
            @Stat
            public int amount = 1;

            @Comment
            @Comment("The action that triggers the item to be used")
            @Comment("Available options:")
            @Comment("- CONSUME: When the item is consumed (material must be a CONSUMABLE item)")
            @Comment("- LEFT_CLICK: When the item is left-clicked with")
            @Comment("- RIGHT_CLICK: When the item is right-clicked with")
            @Stat
            @NotNull public Set<CraftingTrigger> triggers = Set.of(CraftingTrigger.CONSUME);

            @Comment
            @Comment("The cooldown before the item can be used again (only applies to LEFT_CLICK and RIGHT_CLICK triggers).")
            @Comment("This should probably be greater than 0 to prevent accidental uses.")
            @DurationSpec(fallbackUnit = ChronoUnit.MILLIS) @Stat
            @NotNull public Duration cooldown = Duration.ofSeconds(1);

            @Comment
            @Comment("RECIPE (see documentation)")
            @RecipeSpec(name = RECIPE_NAME, resultTransformer = ObtainingRecipeTransformer.class)
            @Nullable public Recipe recipe;

            public Crafting(@org.jetbrains.annotations.NotNull Obtaining obtaining) {
                super(obtaining);

                // Default recipe
                final ItemStack result = new ItemStack(Objects.requireNonNull(XMaterial.APPLE.get()));
                final ItemMeta meta = result.getItemMeta();
                meta.setDisplayName(BukkitUtility.color("&c&lLife"));
                meta.setLore(List.of(BukkitUtility.color("&7Eat to gain a life!")));
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                result.setItemMeta(meta);
                recipe = RefShapedRecipe.newShapedRecipe(result, getRoot().plugin, RECIPE_NAME)
                        .shape(
                                "RDR",
                                "DSD",
                                "RDR")
                        .setIngredient('S', XMaterial.NETHER_STAR.get())
                        .setIngredient('D', XMaterial.DIAMOND.get())
                        .setIngredient('R', XMaterial.REDSTONE.get());
            }
        }
    }
}
