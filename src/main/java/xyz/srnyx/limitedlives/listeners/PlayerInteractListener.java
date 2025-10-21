package xyz.srnyx.limitedlives.listeners;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.AnnoyingListener;
import xyz.srnyx.annoyingapi.cooldown.AnnoyingCooldown;
import xyz.srnyx.annoyingapi.data.ItemData;
import xyz.srnyx.annoyingapi.message.AnnoyingMessage;
import xyz.srnyx.annoyingapi.message.DefaultReplaceType;

import xyz.srnyx.limitedlives.LimitedLives;
import xyz.srnyx.limitedlives.config.CraftingTrigger;
import xyz.srnyx.limitedlives.config.Feature;
import xyz.srnyx.limitedlives.managers.player.PlayerManager;
import xyz.srnyx.limitedlives.managers.player.exception.MoreThanMaxLives;


public class PlayerInteractListener extends AnnoyingListener {
    @NotNull private static final String COOLDOWN_KEY = "use_item";

    @NotNull private final LimitedLives plugin;

    public PlayerInteractListener(@NotNull LimitedLives plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public LimitedLives getAnnoyingPlugin() {
        return plugin;
    }

    @EventHandler
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        final Action action = event.getAction();
        final ItemStack item = event.getItem();

        // Return if:
        if (
                // Physical "click"
                action == Action.PHYSICAL
                // Isn't left click
                || (((action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) || !plugin.config.obtaining.crafting.triggers.contains(CraftingTrigger.LEFT_CLICK))
                // Isn't right click
                && ((action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) || !plugin.config.obtaining.crafting.triggers.contains(CraftingTrigger.RIGHT_CLICK)))
                // Isn't holding item
                || (item == null || !new ItemData(plugin, item).has(PlayerManager.ITEM_KEY))) return;

        event.setCancelled(true);
        final Player player = event.getPlayer();

        // LIFE_USE disabled in world
        final World world = player.getWorld();
        if (!plugin.config.worldsBlacklist.isWorldEnabled(world, Feature.LIFE_USE)) {
            new AnnoyingMessage(plugin, "feature-disabled")
                    .replace("%feature%", Feature.LIFE_USE)
                    .replace("%world%", world.getName())
                    .send(player);
            return;
        }

        // Check cooldown
        final AnnoyingCooldown cooldown = plugin.cooldownManager.getCooldownElseNew(player.getUniqueId(), COOLDOWN_KEY);
        if (cooldown.isOnCooldownStart(plugin.config.obtaining.crafting.cooldown.toMillis())) {
            new AnnoyingMessage(plugin, "eat.cooldown")
                    .replace("%remaining%", cooldown.getRemaining(), DefaultReplaceType.TIME)
                    .send(player);
            return;
        }

        try {
            // Remove item
            item.setAmount(item.getAmount() - 1);

            // Send message and add lives
            new AnnoyingMessage(plugin, "eat.success")
                    .replace("%lives%", new PlayerManager(plugin, player).addLives(plugin.config.obtaining.crafting.amount))
                    .send(player);
        } catch (final MoreThanMaxLives e) {
            new AnnoyingMessage(plugin, "eat.max")
                    .replace("%max%", plugin.config.lives.max)
                    .send(player);
        }
    }
}
