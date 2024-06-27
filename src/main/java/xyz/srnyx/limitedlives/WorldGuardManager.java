package xyz.srnyx.limitedlives;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;

import java.util.logging.Level;


public class WorldGuardManager {
    @Nullable private StateFlag flag;
    @Nullable private WorldGuardPlugin instance;
    @Nullable private RegionContainer regionContainer;

    public WorldGuardManager() {
        try {
            flag = new StateFlag("limited-lives", true);
            final WorldGuard inst = WorldGuard.getInstance();
            inst.getFlagRegistry().register(flag);
            instance = WorldGuardPlugin.inst();
            regionContainer = inst.getPlatform().getRegionContainer();
        } catch (final RuntimeException e) {
            AnnoyingPlugin.log(Level.WARNING, "&cFailed to register WorldGuard flag!");
        }
    }

    public boolean test(@NotNull Player player) {
        return flag != null && instance != null && regionContainer != null && regionContainer.createQuery().testState(BukkitAdapter.adapt(player.getLocation()), instance.wrapPlayer(player), flag);
    }
}
