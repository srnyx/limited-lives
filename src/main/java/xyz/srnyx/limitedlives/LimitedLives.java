package xyz.srnyx.limitedlives;

import org.bukkit.configuration.ConfigurationSection;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.file.AnnoyingData;

import xyz.srnyx.annoyingapi.file.AnnoyingResource;
import xyz.srnyx.limitedlives.commands.ReloadCommand;
import xyz.srnyx.limitedlives.commands.SetCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class LimitedLives extends AnnoyingPlugin {
    public int livesDefault = 5;
    public int livesMax = 10;
    public int livesMin = 0;

    @NotNull public final AnnoyingData data = new AnnoyingData(this, "data.yml", true);
    @NotNull public final Map<UUID, Integer> lives = new HashMap<>();

    public LimitedLives() {
        super();
        options.commandsToRegister.add(new ReloadCommand(this));
        options.commandsToRegister.add(new SetCommand(this));
        options.listenersToRegister.add(new PlayerListener(this));
        final ConfigurationSection section = data.getConfigurationSection("lives");
        if (section != null) section.getKeys(false).forEach(key -> lives.put(UUID.fromString(key), section.getInt(key)));
    }

    @Override
    public void enable() {
        final AnnoyingResource config = new AnnoyingResource(this, "config.yml");
        final ConfigurationSection section = config.getConfigurationSection("lives");
        if (section == null) return;
        livesDefault = section.getInt("default", 5);
        livesMax = section.getInt("max", 10);
        livesMin = section.getInt("min", 0);
    }

    @Override
    public void disable() {
        data.set("lives", lives.entrySet().stream().collect(HashMap::new, (m, e) -> m.put(e.getKey().toString(), e.getValue()), HashMap::putAll), true);
    }

    @Override
    public void reload() {
        enable();
        disable();
    }
}
