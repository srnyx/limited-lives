package xyz.srnyx.limitedlives.commands;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.command.AnnoyingCommand;
import xyz.srnyx.annoyingapi.command.AnnoyingSender;
import xyz.srnyx.annoyingapi.command.selector.Selector;
import xyz.srnyx.annoyingapi.data.EntityData;
import xyz.srnyx.annoyingapi.data.StringData;
import xyz.srnyx.annoyingapi.libs.javautilities.FileUtility;
import xyz.srnyx.annoyingapi.libs.javautilities.manipulation.Mapper;
import xyz.srnyx.annoyingapi.message.AnnoyingMessage;
import xyz.srnyx.annoyingapi.utility.BukkitUtility;

import xyz.srnyx.limitedlives.LimitedLives;
import xyz.srnyx.limitedlives.config.Feature;
import xyz.srnyx.limitedlives.managers.player.PlayerManager;
import xyz.srnyx.limitedlives.managers.player.exception.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
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
        // Check if commands enabled
        if (sender.isPlayer) {
            final World world = sender.getPlayer().getWorld();
            if (!plugin.config.worldsBlacklist.isWorldEnabled(world, Feature.COMMANDS)) {
                new AnnoyingMessage(plugin, "feature-disabled")
                        .replace("%feature%", Feature.COMMANDS)
                        .replace("%world%", world.getName())
                        .send(sender);
                return;
            }
        }
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
            final List<OfflinePlayer> players = sender.getSelector(1, OfflinePlayer.class)
                    .orElseFlatSingle(BukkitUtility::getOfflinePlayer);
            if (players != null) for (final OfflinePlayer player : players) {
                new AnnoyingMessage(plugin, "get.other")
                        .replace("%target%", player.getName())
                        .replace("%lives%", new PlayerManager(plugin, player).getLives())
                        .send(sender);
            }
            return;
        }

        // Get lives
        Integer lives = sender.getArgumentOptionalFlat(1, Mapper::toInt).orElse(null);
        if (lives == null) return;

        if (length == 2) {
            if (!sender.checkPlayer()) return;
            final String action = sender.getArgument(0, String::toLowerCase);
            if (action == null || !sender.checkPermission("limitedlives." + action + ".self")) return;
            final Player player = sender.getPlayer();
            final String playerName = player.getName();

            // Get new lives after action
            final int newLives;
            final PlayerManager manager = new PlayerManager(plugin, player);
            try {
                switch (action) {
                    // set <lives>
                    case "set":
                        newLives = manager.setLives(lives);
                        break;
                    // add <lives>
                    case "add":
                        newLives = manager.addLives(lives);
                        break;
                    // remove <lives>
                    case "remove":
                        newLives = manager.removeLives(lives, null);
                        break;
                    // withdraw <lives>
                    case "withdraw":
                        if (lives <= 0) {
                            new AnnoyingMessage(plugin, "withdraw.negative").send(sender);
                            return;
                        }
                        final int currentLives = manager.getLives();
                        if (currentLives <= lives) lives = currentLives - 1; // Withdraw as many possible
                        if (lives <= plugin.config.lives.min) throw new LessThanMinLives();
                        newLives = manager.withdrawLives(player, lives);
                        break;
                    default:
                        sender.invalidArgumentByIndex(0);
                        return;
                }
            } catch (final ActionException e) {
                new AnnoyingMessage(plugin, action + "." + e.getMessageKey())
                        .replace("%amount%", lives)
                        .replace("%target%", playerName)
                        .replace("%min%", plugin.config.lives.min)
                        .replace("%max%", manager.getMaxLives())
                        .send(sender);
                return;
            }

            // Send message
            new AnnoyingMessage(plugin, action + ".self")
                    .replace("%amount%", lives)
                    .replace("%lives%", newLives)
                    .send(sender);
            return;
        }

        if (length != 3) {
            sender.invalidArguments();
            return;
        }

        // give <lives> <player>
        if (sender.argEquals(0, "give")) {
            // Check if player and has permission
            if (!sender.checkPlayer() || !sender.checkPermission("limitedlives.give")) return;
            // Inputted negative number
            if (lives <= 0) {
                new AnnoyingMessage(plugin, "give.negative").send(sender);
                return;
            }

            // Get target and player
            final OfflinePlayer target = sender.getArgumentOptionalFlat(2, BukkitUtility::getOfflinePlayer).orElse(null);
            if (target == null) return;
            final Player player = sender.getPlayer();
            if (target.getUniqueId().equals(player.getUniqueId())) {
                new AnnoyingMessage(plugin, "give.self").send(sender);
                return;
            }
            final PlayerManager playerManager = new PlayerManager(plugin, player);
            final PlayerManager targetManager = new PlayerManager(plugin, target);

            // Check if player has +1 than min lives
            final int playerLives = playerManager.getLives();
            if (playerLives <= plugin.config.lives.min + 1) {
                new AnnoyingMessage(plugin, "give.last-life").send(sender);
                return;
            }

            // CLAMPING
            // Player doesn't have enough lives, give as many as possible
            if (playerLives <= lives) lives = playerLives - 1;
            // Target can't receive that many lives, give as many as possible
            final int targetLives = targetManager.getLives();
            if (targetLives + lives > targetManager.getMaxLives()) lives = plugin.config.lives.max - targetLives;

            // Take lives from player and give to target
            final int newPlayerLives;
            final int newTargetLives;
            try {
                newPlayerLives = playerManager.removeLives(lives, null);
                newTargetLives = targetManager.addLives(lives);
            } catch (final ActionException e) {
                // Shouldn't happen
                sender.invalidArguments();
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
        final String action = sender.getArgument(0, String::toLowerCase);
        if (action == null || !sender.checkPermission("limitedlives." + action + ".other")) return;

        // Get target
        final OfflinePlayer target = sender.getArgumentOptionalFlat(2, BukkitUtility::getOfflinePlayer).orElse(null);
        if (target == null) return;
        final String targetName = target.getName();

        // Get new lives after action
        final int newLives;
        final PlayerManager manager = new PlayerManager(plugin, target);
        try {
            switch (action) {
                // set <lives> <player>
                case "set":
                    newLives = manager.setLives(lives);
                    break;
                // add <lives> <player>
                case "add":
                    newLives = manager.addLives(lives);
                    break;
                // remove <lives> <player>
                case "remove":
                    newLives = manager.removeLives(lives, null);
                    break;
                // withdraw <lives> <player>
                case "withdraw":
                    if (!sender.checkPlayer()) return;
                    if (lives <= 0) {
                        new AnnoyingMessage(plugin, "withdraw.negative").send(sender);
                        return;
                    }
                    final int currentLives = manager.getLives();
                    if (currentLives <= lives) lives = currentLives - 1; // Withdraw as many possible
                    if (lives <= plugin.config.lives.min) throw new LessThanMinLives();
                    newLives = manager.withdrawLives(sender.getPlayer(), lives);
                    break;
                default:
                    sender.invalidArgumentByIndex(0);
                    return;
            }
        } catch (final ActionException e) {
            new AnnoyingMessage(plugin, action + "." + e.getMessageKey())
                    .replace("%amount%", lives)
                    .replace("%target%", targetName)
                    .replace("%min%", plugin.config.lives.min)
                    .replace("%max%", manager.getMaxLives())
                    .send(sender);
            return;
        }

        // Send message
        new AnnoyingMessage(plugin, action + ".other")
                .replace("%amount%", lives)
                .replace("%target%", target.getName())
                .replace("%lives%", newLives)
                .send(sender);
    }

    @NotNull private static final List<String> NO_ARGS = Arrays.asList("get", "set", "add", "remove", "give", "withdraw", "convert");

    @Override @Nullable
    public Collection<String> onTabComplete(@NotNull AnnoyingSender sender) {
        // Check if commands enabled
        if (!plugin.config.worldsBlacklist.isWorldEnabled(sender.getPlayer().getWorld(), Feature.COMMANDS)) return null;
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
                if (cmdSender.hasPermission("limitedlives.get.other")) return Selector.addKeys(BukkitUtility.getOnlinePlayerNames(), OfflinePlayer.class);
                if (cmdSender.hasPermission("limitedlives.get.self")) return Collections.singleton(cmdSender.getName());
                return null;
            }
            // <action>
            if (sender.argEquals(0, "set", "add", "remove", "withdraw", "give")) return Collections.singleton("[<lives>]");
            return null;
        }

        // <action>
        if (length == 3) {
            final String actionLower = sender.getArgumentOptional(0).map(String::toLowerCase).orElse(null);
            if (actionLower == null || actionLower.equals("get")) return null;
            if (cmdSender.hasPermission("limitedlives." + actionLower + ".other")) return Selector.addKeys(BukkitUtility.getOnlinePlayerNames(), OfflinePlayer.class);
            if (cmdSender.hasPermission("limitedlives." + actionLower + ".self")) return Collections.singleton(cmdSender.getName());
        }

        return null;
    }
}
