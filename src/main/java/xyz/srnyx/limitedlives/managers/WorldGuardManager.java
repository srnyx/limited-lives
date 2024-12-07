package xyz.srnyx.limitedlives.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;


public class WorldGuardManager {
    @NotNull private final StateFlag flag;
    @NotNull private final WorldGuardPlugin instance;
    @NotNull private final RegionContainer regionContainer;

    public WorldGuardManager() {
        flag = new StateFlag("limited-lives", true);
        final WorldGuard inst = WorldGuard.getInstance();
        inst.getFlagRegistry().register(flag);
        instance = WorldGuardPlugin.inst();
        regionContainer = inst.getPlatform().getRegionContainer();
    }

    public boolean test(@NotNull Player player) {
        return regionContainer.createQuery().testState(BukkitAdapter.adapt(player.getLocation()), instance.wrapPlayer(player), flag);
    }
}
