package xyz.srnyx.limitedlives.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.command.AnnoyingCommand;
import xyz.srnyx.annoyingapi.command.AnnoyingSender;
import xyz.srnyx.annoyingapi.message.AnnoyingMessage;
import xyz.srnyx.annoyingapi.utility.BukkitUtility;

import xyz.srnyx.limitedlives.LimitedLives;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;


public class LivesCmd implements AnnoyingCommand {
    @NotNull private final LimitedLives plugin;

    public LivesCmd(@NotNull LimitedLives plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public LimitedLives getAnnoyingPlugin() {
        return plugin;
    }

    @Override @NotNull
    public String getName() {
        return "lives";
    }

    @Override
    public void onCommand(@NotNull AnnoyingSender sender) {
        final String[] args = sender.args;

        // No arguments, get
        if (args.length == 0 || (args.length == 1 && sender.argEquals(0, "get"))) {
            if (sender.checkPlayer() && sender.checkPermission("limitedlives.get.self")) new AnnoyingMessage(plugin, "get.self")
                    .replace("%lives%", plugin.getLives(sender.getPlayer().getUniqueId()))
                    .send(sender);
            return;
        }

        // Check args length
        if (args.length < 2) {
            sender.invalidArguments();
            return;
        }

        // get <player>
        if (sender.argEquals(0, "get")) {
            if (!sender.checkPermission("limitedlives.get.other")) return;
            final OfflinePlayer player = BukkitUtility.getOfflinePlayer(args[1]);
            if (player == null) {
                sender.invalidArgument(args[1]);
                return;
            }
            new AnnoyingMessage(plugin, "get.other")
                    .replace("%target%", player.getName())
                    .replace("%lives%", plugin.getLives(player.getUniqueId()))
                    .send(sender);
            return;
        }

        // Get action
        final ModificationAction action;
        try {
            action = ModificationAction.valueOf(args[0].toUpperCase());
        } catch (final IllegalArgumentException e) {
            sender.invalidArgument(args[0]);
            return;
        }

        // Get lives
        final int lives;
        try {
            lives = Integer.parseInt(args[1]);
        } catch (final NumberFormatException e) {
            sender.invalidArgument(args[1]);
            return;
        }

        // <action> <lives>
        if (args.length == 2) {
            if (!sender.checkPlayer() || !sender.checkPermission("limitedlives." + action + ".self")) return;
            final Player player = sender.getPlayer();
            final Integer newLives = action.process(plugin, player, lives);
            if (newLives == null) {
                sender.invalidArgument(args[1]);
                return;
            }
            new AnnoyingMessage(plugin, action + ".self")
                    .replace("%amount%", lives)
                    .replace("%lives%", newLives)
                    .send(player);
            return;
        }

        // <action> <lives> <player>
        if (!sender.checkPermission("limitedlives." + action + ".other")) return;
        final OfflinePlayer target = BukkitUtility.getOfflinePlayer(args[2]);
        if (target == null) {
            sender.invalidArgument(args[2]);
            return;
        }
        final Integer newLives = action.process(plugin, target, lives);
        if (newLives == null) {
            sender.invalidArgument(args[1]);
            return;
        }
        new AnnoyingMessage(plugin, action + ".other")
                .replace("%amount%", lives)
                .replace("%target%", target.getName())
                .replace("%lives%", newLives)
                .send(sender);
    }

    @Override @Nullable
    public Collection<String> onTabComplete(@NotNull AnnoyingSender sender) {
        final String[] args = sender.args;
        final int length = args.length;

        // No arguments
        if (length == 1) return Arrays.asList("get", "set", "add", "remove");
        final CommandSender cmdSender = sender.cmdSender;

        if (length == 2) {
            // get
            if (sender.argEquals(0, "get")) {
                if (cmdSender.hasPermission("limitedlives.get.self")) return Collections.singletonList(cmdSender.getName());
                if (cmdSender.hasPermission("limitedlives.get.other")) return BukkitUtility.getOnlinePlayerNames();
            }
            // set/add/remove
            return Collections.singletonList("[<lives>]");
        }

        // set/add/remove
        if (length == 3) {
            final String actionLower = args[0].toLowerCase();
            if (cmdSender.hasPermission("limitedlives." + actionLower + ".self")) return Collections.singletonList(cmdSender.getName());
            if (cmdSender.hasPermission("limitedlives." + actionLower + ".other")) return BukkitUtility.getOnlinePlayerNames();
        }

        return null;
    }

    private enum ModificationAction {
        SET(LimitedLives::setLives),
        ADD(LimitedLives::addLives),
        REMOVE((pluginAction, player, lives) -> pluginAction.removeLives(player, lives, null));

        @NotNull private final TriFunction<LimitedLives, OfflinePlayer, Integer, Integer> consumer;

        ModificationAction(@NotNull TriFunction<LimitedLives, OfflinePlayer, Integer, Integer> consumer) {
            this.consumer = consumer;
        }

        @Override @NotNull
        public String toString() {
            return name().toLowerCase();
        }

        @Nullable
        public Integer process(@NotNull LimitedLives plugin, @NotNull OfflinePlayer player, int lives) {
            return consumer.accept(plugin, player, lives);
        }

        private interface TriFunction<A, B, C, O> {
            O accept(A a, B b, C c);
        }
    }
}
