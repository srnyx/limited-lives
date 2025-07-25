package xyz.srnyx.limitedlives.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;


public class WorldGuardManager {
    @NotNull private final StateFlag flag = new StateFlag("limited-lives", true);
    @NotNull private final WorldGuardPlugin worldGuardPlugin = WorldGuardPlugin.inst();
    private RegionContainer regionContainer;

    public WorldGuardManager() {
        WorldGuard.getInstance().getFlagRegistry().register(flag);
    }

    public void storeRegionContainer() {
        regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
    }

    public boolean test(@NotNull Player player) {
        return regionContainer.createQuery().testState(BukkitAdapter.adapt(player.getLocation()), worldGuardPlugin.wrapPlayer(player), flag);
    }
}
