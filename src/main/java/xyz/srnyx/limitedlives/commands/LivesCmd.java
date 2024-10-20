package xyz.srnyx.limitedlives.commands;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.command.AnnoyingCommand;
import xyz.srnyx.annoyingapi.command.AnnoyingSender;
import xyz.srnyx.annoyingapi.data.EntityData;
import xyz.srnyx.annoyingapi.data.StringData;
import xyz.srnyx.annoyingapi.libs.javautilities.FileUtility;
import xyz.srnyx.annoyingapi.libs.javautilities.MiscUtility;
import xyz.srnyx.annoyingapi.libs.javautilities.manipulation.Mapper;
import xyz.srnyx.annoyingapi.message.AnnoyingMessage;
import xyz.srnyx.annoyingapi.utility.BukkitUtility;

import xyz.srnyx.limitedlives.LimitedLives;
import xyz.srnyx.limitedlives.PlayerManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;


public class LivesCmd extends AnnoyingCommand {
    @NotNull private static final Gson GSON = new Gson();

    @NotNull private final LimitedLives plugin;

    public LivesCmd(@NotNull LimitedLives plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public LimitedLives getAnnoyingPlugin() {
        return plugin;
    }

    @Override
    public void onCommand(@NotNull AnnoyingSender sender) {
        final int length = sender.args.length;

        // No arguments, get
        if (length == 0 || (length == 1 && sender.argEquals(0, "get"))) {
            if (sender.checkPlayer() && sender.checkPermission("limitedlives.get.self")) new AnnoyingMessage(plugin, "get.self")
                    .replace("%lives%", new PlayerManager(plugin, sender.getPlayer()).getLives())
                    .send(sender);
            return;
        }

        // Check args length
        if (length < 2) {
            sender.invalidArguments();
            return;
        }

        // convert hardcorelivesplugin
        if (sender.argEquals(0, "convert")) {
            if (!sender.checkPermission("limitedlives.convert")) return;
            if (!sender.argEquals(1, "hardcorelivesplugin")) {
                sender.invalidArgumentByIndex(1);
                return;
            }

            // File: plugins/Hardcorelivesplugin/players/UUID.json
            // Structure: {"uuid":"e907083e-5db6-41fc-9e32-5c4d99a08712","username":"srnyx","lives":3,"bypassLives":false,"maxLives":5}
            // Converting: "uuid" and "lives"
            int succeeded = 0;
            int failed = 0;
            final File playersFolder = new File(plugin.getDataFolder().getParentFile(), "Hardcorelivesplugin/players");
            for (final String uuidString : FileUtility.getFileNames(playersFolder, "json")) {
                // Parse file as JSON
                final JsonObject json;
                try {
                    json = GSON.fromJson(new FileReader(new File(playersFolder, uuidString + ".json")), JsonObject.class);
                } catch (final FileNotFoundException e) {
                    AnnoyingPlugin.log(Level.WARNING, "Failed to convert Hardcore Lives Plugin data for " + uuidString + ", file not found", e);
                    failed++;
                    continue;
                }

                // Get lives
                final JsonElement livesElement = json.get("lives");
                if (livesElement == null) {
                    AnnoyingPlugin.log(Level.WARNING, "Failed to convert Hardcore Lives Plugin data for " + uuidString + ", lives not found");
                    failed++;
                    continue;
                }
                final int lives;
                try {
                    lives = livesElement.getAsInt();
                } catch (final ClassCastException e) {
                    AnnoyingPlugin.log(Level.WARNING, "Failed to convert Hardcore Lives Plugin data for " + uuidString + ", lives not an integer", e);
                    failed++;
                    continue;
                }

                // Save lives to Limited Lives
                if (!new StringData(plugin, EntityData.TABLE_NAME, uuidString).set(PlayerManager.LIVES_KEY, lives)) {
                    AnnoyingPlugin.log(Level.WARNING, "Failed to convert Hardcore Lives Plugin data for " + uuidString + ", failed to save");
                    failed++;
                    continue;
                }

                AnnoyingPlugin.log(Level.INFO, "Converted Hardcore Lives Plugin data for " + uuidString + " with " + lives + " lives");
                succeeded++;
            }

            new AnnoyingMessage(plugin, "convert")
                    .replace("%source%", "HardcoreLivesPlugin")
                    .replace("%succeeded%", succeeded)
                    .replace("%failed%", failed)
                    .send(sender);
            return;
        }

        // get <player>
        if (sender.argEquals(0, "get")) {
            if (!sender.checkPermission("limitedlives.get.other")) return;
            final OfflinePlayer player = sender.getArgument(1, arg -> BukkitUtility.getOfflinePlayer(arg).orElse(null));
            if (player == null) return;
            new AnnoyingMessage(plugin, "get.other")
                    .replace("%target%", player.getName())
                    .replace("%lives%", new PlayerManager(plugin, player).getLives())
                    .send(sender);
            return;
        }

        // Get lives
        Integer lives = sender.getArgument(1, arg -> Mapper.toInt(arg).orElse(null));
        if (lives == null) return;

        // give <lives> <player>
        if (sender.argEquals(0, "give")) {
            // Check argument count
            if (length < 3) {
                sender.invalidArguments();
                return;
            }

            // Check if player and has permission
            if (!sender.checkPlayer() || !sender.checkPermission("limitedlives.give")) return;

            // Get target and player
            final OfflinePlayer target = sender.getArgument(2, arg -> BukkitUtility.getOfflinePlayer(arg).orElse(null));
            if (target == null) return;
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
            if (targetLives + lives > targetManager.getMaxLives()) lives = plugin.config.lives.max - targetLives; // Target can't receive that many lives, give as many as possible
            if (lives <= 0) { // Lives is 0 or less (only happens if user inputs a negative number)
                new AnnoyingMessage(plugin, "give.last-life").send(sender);
                return;
            }

            // Take lives from player and give to target
            final Integer newPlayerLives = playerManager.removeLives(lives, null).orElse(null);
            final Integer newTargetLives = targetManager.addLives(lives).orElse(null);
            if (newPlayerLives == null || newTargetLives == null) {
                sender.invalidArgumentByIndex(1);
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
            if (target instanceof Player) new AnnoyingMessage(plugin, "give.target")
                    .replace("%player%", playerName)
                    .replace("%target%", targetName)
                    .replace("%playerlives%", newPlayerLives)
                    .replace("%targetlives%", newTargetLives)
                    .replace("%amount%", lives)
                    .send((Player) target);
            return;
        }

        // Get action
        final ModificationAction action = sender.getArgument(0, arg -> MiscUtility.handleException(() -> ModificationAction.valueOf(arg.toUpperCase()), IllegalArgumentException.class).orElse(null));
        if (action == null) return;

        // <action> <lives>
        if (length == 2) {
            if (!sender.checkPlayer() || !sender.checkPermission("limitedlives." + action + ".self")) return;
            final Optional<Integer> newLives = action.function.apply(new ModificationData(plugin, sender.getPlayer(), lives));
            if (!newLives.isPresent()) {
                sender.invalidArgumentByIndex(1);
                return;
            }
            new AnnoyingMessage(plugin, action + ".self")
                    .replace("%amount%", lives)
                    .replace("%lives%", newLives.get())
                    .send(sender);
            return;
        }

        // <action> <lives> <player>
        if ((action.playerOnly && !sender.checkPlayer()) || !sender.checkPermission("limitedlives." + action + ".other")) return;
        final OfflinePlayer target = sender.getArgument(2, arg -> BukkitUtility.getOfflinePlayer(arg).orElse(null));
        if (target == null) return;
        final Optional<Integer> newLives = action.function.apply(new ModificationData(plugin, sender.getPlayerOrNull(), target, lives));
        if (!newLives.isPresent()) {
            sender.invalidArgumentByIndex(1);
            return;
        }
        new AnnoyingMessage(plugin, action + ".other")
                .replace("%amount%", lives)
                .replace("%target%", target.getName())
                .replace("%lives%", newLives.get())
                .send(sender);
    }

    @NotNull private static final List<String> NO_ARGS = Arrays.asList("get", "set", "add", "remove", "give", "withdraw", "convert");

    @Override @Nullable
    public Collection<String> onTabComplete(@NotNull AnnoyingSender sender) {
        final String[] args = sender.args;
        final int length = args.length;

        // No arguments
        if (length == 1) return NO_ARGS;
        final CommandSender cmdSender = sender.cmdSender;

        if (length == 2) {
            // convert
            if (sender.argEquals(0, "convert")) {
                if (cmdSender.hasPermission("limitedlives.convert")) return Collections.singleton("hardcorelivesplugin");
                return null;
            }
            // get
            if (sender.argEquals(0, "get")) {
                if (cmdSender.hasPermission("limitedlives.get.other")) return BukkitUtility.getOnlinePlayerNames();
                if (cmdSender.hasPermission("limitedlives.get.self")) return Collections.singleton(cmdSender.getName());
                return null;
            }
            // <action>
            return Collections.singleton("[<lives>]");
        }

        // <action>
        if (length == 3) {
            final String actionLower = args[0].toLowerCase();
            if (cmdSender.hasPermission("limitedlives." + actionLower + ".other")) return BukkitUtility.getOnlinePlayerNames();
            if (cmdSender.hasPermission("limitedlives." + actionLower + ".self")) return Collections.singleton(cmdSender.getName());
        }

        return null;
    }

    private static class ModificationData {
        @NotNull private final PlayerManager manager;
        @Nullable private final Player sender;
        private int lives;

        public ModificationData(@NotNull LimitedLives plugin, @Nullable Player sender, @NotNull OfflinePlayer target, int lives) {
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
            if (data.sender == null) return Optional.empty(); // Shouldn't happen
            final int targetLives = data.manager.getLives();
            if (targetLives == 1) return Optional.empty(); // Target has 1 life left, if so, don't allow withdrawing lives
            if (targetLives <= data.lives) data.lives = targetLives - 1; // Target has enough lives, if not, take as many as possible
            if (data.lives <= 0) return Optional.empty(); // Lives is 0 or less (only happens if user inputs a negative number)
            return data.manager.withdrawLives(data.sender, data.lives);
        });

        private final boolean playerOnly;
        @NotNull private final Function<ModificationData, Optional<Integer>> function;

        ModificationAction(boolean playerOnly, @NotNull Function<ModificationData, Optional<Integer>> function) {
            this.playerOnly = playerOnly;
            this.function = function;
        }

        ModificationAction(@NotNull Function<ModificationData, Optional<Integer>> function) {
            this(false, function);
        }

        @Override @NotNull
        public String toString() {
            return name().toLowerCase();
        }
    }
}
