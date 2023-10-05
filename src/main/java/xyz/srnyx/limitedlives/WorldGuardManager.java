package xyz.srnyx.limitedlives;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;

import java.util.logging.Level;


public class WorldGuardManager {
    private StateFlag flag;
    private WorldGuardPlugin instance;
    private RegionContainer regionContainer;

    public WorldGuardManager() {
        try {
            flag = new StateFlag("limited-lives", true);
            WorldGuard.getInstance().getFlagRegistry().register(flag);
            instance = WorldGuardPlugin.inst();
        } catch (final RuntimeException e) {
            AnnoyingPlugin.log(Level.WARNING, "&cFailed to register WorldGuard flag!");
        }
    }

    public boolean test(@NotNull Player player) {
        if (regionContainer == null) regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return regionContainer.createQuery().testState(BukkitAdapter.adapt(player.getLocation()), instance.wrapPlayer(player), flag);
    }
}
