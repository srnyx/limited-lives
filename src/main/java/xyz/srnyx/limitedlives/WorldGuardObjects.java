package xyz.srnyx.limitedlives;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;


public class WorldGuardObjects {
    @NotNull private final Object instance;
    @NotNull private final Object regionContainer;
    @NotNull private final Object flag;

    public WorldGuardObjects(@NotNull Object instance, @NotNull Object regionContainer, @NotNull Object flag) {
        this.instance = instance;
        this.regionContainer = regionContainer;
        this.flag = flag;
    }

    public boolean test(@NotNull Player player) {
        return ((RegionContainer) regionContainer).createQuery().testState(BukkitAdapter.adapt(player.getLocation()), ((WorldGuardPlugin) instance).wrapPlayer(player), (StateFlag) flag);
    }
}
