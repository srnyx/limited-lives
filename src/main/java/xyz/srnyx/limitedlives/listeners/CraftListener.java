package xyz.srnyx.limitedlives.listeners;

import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.Recipe;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.AnnoyingListener;
import xyz.srnyx.annoyingapi.data.ItemData;
import xyz.srnyx.annoyingapi.message.AnnoyingMessage;

import xyz.srnyx.limitedlives.LimitedLives;
import xyz.srnyx.limitedlives.config.Feature;
import xyz.srnyx.limitedlives.managers.player.PlayerManager;


public class CraftListener extends AnnoyingListener {
    @NotNull private final LimitedLives plugin;

    public CraftListener(@NotNull LimitedLives plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public LimitedLives getAnnoyingPlugin() {
        return plugin;
    }

    @EventHandler
    public void onPrepareItemCraft(@NotNull PrepareItemCraftEvent event) {
        // Prevent crafting if feature disabled
        final HumanEntity player = event.getView().getPlayer();
        final World world = player.getWorld();
        final Recipe recipe = event.getRecipe();
        if (recipe == null || plugin.config.worldsBlacklist.isWorldEnabled(world, Feature.OBTAINING_CRAFTING) || new ItemData(plugin, recipe.getResult()).get(PlayerManager.ITEM_KEY) == null) return;
        new AnnoyingMessage(plugin, "feature-disabled")
                .replace("%feature%", Feature.OBTAINING_CRAFTING)
                .replace("%world%", world.getName())
                .send(player);
        event.getInventory().setResult(null);
    }
}
