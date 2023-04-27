package xyz.srnyx.limitedlives;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.file.AnnoyingData;

import xyz.srnyx.limitedlives.commands.LivesCommand;
import xyz.srnyx.limitedlives.commands.ReloadCommand;
import xyz.srnyx.limitedlives.commands.SetCommand;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class LimitedLives extends AnnoyingPlugin {
    @NotNull public LimitedConfig config = new LimitedConfig(this);
    @NotNull public final AnnoyingData data = new AnnoyingData(this, "data.yml");
    @NotNull public final Map<UUID, Integer> lives = new HashMap<>();

    public LimitedLives() {
        super();
        options.bStatsId = 18304;
        Collections.addAll(options.commandsToRegister,
                new LivesCommand(this),
                new ReloadCommand(this),
                new SetCommand(this));
        options.listenersToRegister.add(new PlayerListener(this));
        final ConfigurationSection section = data.getConfigurationSection("lives");
        if (section != null) section.getKeys(false).forEach(key -> lives.put(UUID.fromString(key), section.getInt(key)));
    }

    @Override
    public void enable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) new LimitedPlaceholders(this).register();
    }

    @Override
    public void disable() {
        data.setSave("lives", lives.entrySet().stream().collect(HashMap::new, (m, e) -> m.put(e.getKey().toString(), e.getValue()), HashMap::putAll));
    }

    @Override
    public void reload() {
        config = new LimitedConfig(this);
        disable();
    }

    public int getLives(@NotNull UUID uuid) {
        return lives.getOrDefault(uuid, config.livesDefault);
    }
}
