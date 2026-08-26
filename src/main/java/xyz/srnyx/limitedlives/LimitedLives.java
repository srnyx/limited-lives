package xyz.srnyx.limitedlives;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.limitedlives.config.CraftingTrigger;
import xyz.srnyx.limitedlives.config.LimitedConfig;
import xyz.srnyx.limitedlives.config.serdes.DamageCauseWrapperSerializer;
import xyz.srnyx.limitedlives.listeners.CraftListener;
import xyz.srnyx.limitedlives.listeners.PlayerInteractListener;
import xyz.srnyx.limitedlives.listeners.PlayerItemConsumeListener;
import xyz.srnyx.limitedlives.managers.PlaceholderManager;
import xyz.srnyx.limitedlives.managers.WorldGuardManager;
import xyz.srnyx.limitedlives.managers.player.PlayerManager;
import xyz.srnyx.limitedlives.messages.LLMessagesProvider;
import xyz.srnyx.limitedlives.stats.FastStats;

import java.io.File;
import java.util.logging.Level;


public class LimitedLives extends AnnoyingPlugin {
    public LimitedConfig config;
    @NotNull public final PlayerItemConsumeListener playerItemConsumeListener = new PlayerItemConsumeListener(this);
    @NotNull public final PlayerInteractListener playerInteractListener = new PlayerInteractListener(this);
    @NotNull public final CraftListener craftListener = new CraftListener(this);
    @Nullable public final WorldGuardManager worldGuard;

    public LimitedLives() {
        options
                .statsOptions(statsOptions -> statsOptions
                        .bStats(bStats -> bStats.id(18304))
                        .fastStats(fastStats -> fastStats.loader(FastStats.class)))
                .dataOptions(dataOptions -> dataOptions.entityDataColumns(
                        PlayerManager.LIVES_KEY,
                        PlayerManager.DEAD_KEY,
                        PlayerManager.GRACE_START_KEY))
                .registrationOptions.papiExpansionToRegister(() -> new PlaceholderManager(this));

        // Register WorldGuardManager (needs to happen on load before WorldGuard enables)
        WorldGuardManager worldGuardManager = null;
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) try {
            worldGuardManager = new WorldGuardManager();
        } catch (final Exception e) {
            AnnoyingPlugin.log(Level.WARNING, "&cFailed to register WorldGuard flag!", e);
        }
        worldGuard = worldGuardManager;
    }

    @Override @NotNull
    public LLMessagesProvider getMessages() {
        return (LLMessagesProvider) super.getMessages();
    }

    @Override
    public void load() {
        config = configLoader.build(build -> build
                .config(new LimitedConfig(this))
                .configure(configure -> configure.serdes(new DamageCauseWrapperSerializer())));
    }

    @Override
    public void enable() {
        setup();

        if (config.obtaining.crafting.enabled && config.obtaining.crafting.recipe != null) try {
            Bukkit.addRecipe(config.obtaining.crafting.recipe);
        } catch (final Exception e) {
            log(Level.WARNING, "&cFailed to add crafting recipe!", e);
        }
    }

    @Override
    public void reload() {
        config.reload();
        setup();
    }

    private void setup() {
        // Store WorldGuard RegionContainer (needs to happen on enable after WorldGuard enables)
        if (worldGuard != null) worldGuard.storeRegionContainer();
        // Detect very old data (data/data.yml, 2.0.1 and lower)
        final File oldDataFile = new File(getDataFolder(), "data/data.yml");
        if (oldDataFile.exists()) log(Level.SEVERE, "&c&lOld data detected!&c To keep your old data, please update to &43.0.1&c FIRST and then to &4" + getDescription().getVersion() + "&c! &oIf this is incorrect, delete &4&o" + oldDataFile.getPath());

        // Register appropriate listeners
        playerItemConsumeListener.setRegistered(config.obtaining.crafting.triggers.contains(CraftingTrigger.CONSUME));
        playerInteractListener.setRegistered(config.obtaining.crafting.triggers.contains(CraftingTrigger.LEFT_CLICK) || config.obtaining.crafting.triggers.contains(CraftingTrigger.RIGHT_CLICK));
        craftListener.setRegistered(config.obtaining.crafting.recipe != null);
    }
}
