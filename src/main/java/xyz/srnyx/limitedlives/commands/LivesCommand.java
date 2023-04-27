package xyz.srnyx.limitedlives.commands;

import org.bukkit.OfflinePlayer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingMessage;
import xyz.srnyx.annoyingapi.AnnoyingUtility;
import xyz.srnyx.annoyingapi.command.AnnoyingCommand;
import xyz.srnyx.annoyingapi.command.AnnoyingSender;

import xyz.srnyx.limitedlives.LimitedLives;

import java.util.Collection;


public class LivesCommand implements AnnoyingCommand {
    @NotNull private final LimitedLives plugin;

    public LivesCommand(@NotNull LimitedLives plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public LimitedLives getPlugin() {
        return plugin;
    }

    @Override @NotNull
    public String getName() {
        return "lives";
    }

    @Override @NotNull
    public String getPermission() {
        return "limitedlives.lives";
    }

    @Override
    public void onCommand(@NotNull AnnoyingSender sender) {
        final String[] args = sender.args;

        // No arguments
        if (args.length == 0 && sender.checkPlayer()) {
            new AnnoyingMessage(plugin, "get.self")
                    .replace("%lives%", plugin.getLives(sender.getPlayer().getUniqueId()))
                    .send(sender);
            return;
        }

        // [<player>]
        if (args.length == 1) {
            final OfflinePlayer player = AnnoyingUtility.getOfflinePlayer(args[0]);
            if (player == null) {
                new AnnoyingMessage(plugin, "error.invalid-argument")
                        .replace("%argument%", args[0])
                        .send(sender);
                return;
            }

            new AnnoyingMessage(plugin, "get.other")
                    .replace("%target%", player.getName())
                    .replace("%lives%", plugin.getLives(player.getUniqueId()))
                    .send(sender);
            return;
        }

        new AnnoyingMessage(plugin, "error.invalid-arguments").send(sender);
    }

    @Override @Nullable
    public Collection<String> onTabComplete(@NotNull AnnoyingSender sender) {
        if (sender.args.length == 1) return AnnoyingUtility.getOnlinePlayerNames();
        return null;
    }
}
