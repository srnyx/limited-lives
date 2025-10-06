package xyz.srnyx.limitedlives.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.AnnoyingListener;
import xyz.srnyx.annoyingapi.data.ItemData;
import xyz.srnyx.annoyingapi.message.AnnoyingMessage;

import xyz.srnyx.limitedlives.LimitedLives;
import xyz.srnyx.limitedlives.managers.player.PlayerManager;
import xyz.srnyx.limitedlives.managers.player.exception.MoreThanMaxLives;


public class PlayerItemConsumeListener extends AnnoyingListener {
    @NotNull private final LimitedLives plugin;

    public PlayerItemConsumeListener(@NotNull LimitedLives plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public LimitedLives getAnnoyingPlugin() {
        return plugin;
    }

    @EventHandler
    public void onPlayerItemConsume(@NotNull PlayerItemConsumeEvent event) {
        if (!new ItemData(plugin, event.getItem()).has(PlayerManager.ITEM_KEY)) return;
        final Player player = event.getPlayer();
        try {
            new AnnoyingMessage(plugin, "eat.success")
                    .replace("%lives%", new PlayerManager(plugin, player).addLives(plugin.config.obtaining.crafting.amount))
                    .send(player);
        } catch (final MoreThanMaxLives e) {
            event.setCancelled(true);
            new AnnoyingMessage(plugin, "eat.max")
                    .replace("%max%", plugin.config.lives.max)
                    .send(player);
        }
    }
}
