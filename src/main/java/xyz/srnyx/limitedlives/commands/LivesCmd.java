package xyz.srnyx.limitedlives.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.command.AnnoyingCommand;
import xyz.srnyx.annoyingapi.command.AnnoyingSender;
import xyz.srnyx.annoyingapi.message.AnnoyingMessage;
import xyz.srnyx.annoyingapi.utility.BukkitUtility;

import xyz.srnyx.limitedlives.LimitedLives;
import xyz.srnyx.limitedlives.PlayerManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;


public class LivesCmd extends AnnoyingCommand {
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
                    .replace("%lives%", new PlayerManager(plugin, sender.getPlayer()).getLives())
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
            final Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.invalidArgument(args[1]);
                return;
            }
            new AnnoyingMessage(plugin, "get.other")
                    .replace("%target%", player.getName())
                    .replace("%lives%", new PlayerManager(plugin, player).getLives())
                    .send(sender);
            return;
        }

        // Get lives
        int lives;
        try {
            lives = Integer.parseInt(args[1]);
        } catch (final NumberFormatException e) {
            sender.invalidArgument(args[1]);
            return;
        }

        // give <lives> <player>
        if (sender.argEquals(0, "give")) {
            // Check argument count
            if (args.length < 3) {
                sender.invalidArguments();
                return;
            }

            // Check if player and has permission
            if (!sender.checkPlayer() || !sender.checkPermission("limitedlives.give")) return;

            // Get target and player
            final Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.invalidArgument(args[2]);
                return;
            }
            final Player player = sender.getPlayer();
            final PlayerManager playerManager = new PlayerManager(plugin, player);
            final PlayerManager targetManager = new PlayerManager(plugin, target);

            // CHECKS
            final int playerLives = playerManager.getLives();
            final int targetLives = targetManager.getLives();
            if (playerLives == 1) { // Player has 1 life left, don't allow giving lives
                new AnnoyingMessage(plugin, "give.last-life").send(sender);
                return;
            }
            if (playerLives <= lives) lives = playerLives - 1; // Player doesn't have enough lives, give as many as possible
            if (targetLives + lives > targetManager.getMaxLives()) lives = plugin.config.livesMax - targetLives; // Target can't receive that many lives, give as many as possible
            if (lives <= 0) { // Lives is 0 or less (shouldn't happen)
                new AnnoyingMessage(plugin, "give.last-life").send(sender);
                return;
            }

            // Take lives from player and give to target
            final Integer newPlayerLives = playerManager.removeLives(lives, null);
            final Integer newTargetLives = targetManager.addLives(lives);
            if (newPlayerLives == null || newTargetLives == null) {
                sender.invalidArgument(args[1]);
                return;
            }

            // Send messages
            final String playerName = player.getName();
            final String targetName = target.getName();
            new AnnoyingMessage(plugin, "give.player")
                    .replace("%player%", playerName)
                    .replace("%target%", targetName)
                    .replace("%playerlives%", newPlayerLives)
                    .replace("%targetlives%", newTargetLives)
                    .replace("%amount%", lives)
                    .send(sender);
            new AnnoyingMessage(plugin, "give.target")
                    .replace("%player%", playerName)
                    .replace("%target%", targetName)
                    .replace("%playerlives%", newPlayerLives)
                    .replace("%targetlives%", newTargetLives)
                    .replace("%amount%", lives)
                    .send(target);

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

        // <action> <lives>
        if (args.length == 2) {
            if (!sender.checkPlayer() || !sender.checkPermission("limitedlives." + action + ".self")) return;
            final Integer newLives = action.process(new ModificationData(plugin, sender.getPlayer(), lives));
            if (newLives == null) {
                sender.invalidArgument(args[1]);
                return;
            }
            new AnnoyingMessage(plugin, action + ".self")
                    .replace("%amount%", lives)
                    .replace("%lives%", newLives)
                    .send(sender);
            return;
        }

        // <action> <lives> <player>
        if ((action.playerOnly && !sender.checkPlayer()) || !sender.checkPermission("limitedlives." + action + ".other")) return;
        final Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.invalidArgument(args[2]);
            return;
        }
        final Integer newLives = action.process(new ModificationData(plugin, sender.getPlayer(), target, lives));
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

    @NotNull private static final List<String> NO_ARGS = Arrays.asList("get", "set", "add", "remove", "give", "withdraw");

    @Override @Nullable
    public Collection<String> onTabComplete(@NotNull AnnoyingSender sender) {
        final String[] args = sender.args;
        final int length = args.length;

        // No arguments
        if (length == 1) return NO_ARGS;
        final CommandSender cmdSender = sender.cmdSender;

        if (length == 2) {
            // get
            if (sender.argEquals(0, "get")) {
                if (cmdSender.hasPermission("limitedlives.get.self")) return Collections.singleton(cmdSender.getName());
                if (cmdSender.hasPermission("limitedlives.get.other")) return BukkitUtility.getOnlinePlayerNames();
            }
            // <action>
            return Collections.singleton("[<lives>]");
        }

        // <action>
        if (length == 3) {
            final String actionLower = args[0].toLowerCase();
            if (cmdSender.hasPermission("limitedlives." + actionLower + ".self")) return Collections.singleton(cmdSender.getName());
            if (cmdSender.hasPermission("limitedlives." + actionLower + ".other")) return BukkitUtility.getOnlinePlayerNames();
        }

        return null;
    }

    private static class ModificationData {
        @NotNull private final PlayerManager manager;
        @NotNull private final Player sender;
        private int lives;

        public ModificationData(@NotNull LimitedLives plugin, @NotNull Player sender, @NotNull Player target, int lives) {
            manager = new PlayerManager(plugin, target);
            this.sender = sender;
            this.lives = lives;
        }

        public ModificationData(@NotNull LimitedLives plugin, @NotNull Player target, int lives) {
            this(plugin, target, target, lives);
        }
    }

    private enum ModificationAction {
        SET(data -> data.manager.setLives(data.lives)),
        ADD(data -> data.manager.addLives(data.lives)),
        REMOVE(data -> data.manager.removeLives(data.lives, null)),
        WITHDRAW(true, data -> {
            final int targetLives = data.manager.getLives();
            if (targetLives == 1) return null; // Target has 1 life left, if so, don't allow withdrawing lives
            if (targetLives <= data.lives) data.lives = targetLives - 1; // Target has enough lives, if not, take as many as possible
            return data.manager.withdrawLives(data.sender, data.lives);
        });

        private final boolean playerOnly;
        @NotNull private final Function<ModificationData, Integer> function;

        ModificationAction(boolean playerOnly, @NotNull Function<ModificationData, Integer> function) {
            this.playerOnly = playerOnly;
            this.function = function;
        }

        ModificationAction(@NotNull Function<ModificationData, Integer> function) {
            this(false, function);
        }

        @Override @NotNull
        public String toString() {
            return name().toLowerCase();
        }

        @Nullable
        public Integer process(@NotNull ModificationData data) {
            return function.apply(data);
        }
    }
}
