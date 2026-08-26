package xyz.srnyx.limitedlives.commands;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.command.AnnoyingSender;
import xyz.srnyx.annoyingapi.data.EntityData;
import xyz.srnyx.annoyingapi.data.StringData;
import xyz.srnyx.annoyingapi.libs.javautilities.FileUtility;
import xyz.srnyx.annoyingapi.libs.javautilities.manipulation.Mapper;
import xyz.srnyx.annoyingapi.message.json.message.JsonChatMessage;
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


public class LivesCmd extends xyz.srnyx.limitedlives.commands.generated.LivesCmdGen {
    @NotNull private static final Gson GSON = new Gson();

    public LivesCmd(@NotNull LimitedLives plugin) {
        super(plugin);
    }

    @Override
    public void onCommand(@NotNull AnnoyingSender sender) {
        // Check if commands enabled
        if (sender.isPlayer) {
            final World world = sender.getPlayer().getWorld();
            if (!plugin.config.worlds_blacklist.isWorldEnabled(world, Feature.COMMANDS)) {
                plugin.getMessages().get().feature_disabled.newMessage()
                        .replace("%feature%", Feature.COMMANDS)
                        .replace("%world%", world.getName())
                        .send(sender);
                return;
            }
        }
        final int length = sender.args.length;

        // No arguments, get
        if (length == 0 || (length == 1 && sender.argEquals(0, "get"))) {
            if (sender.checkPlayer() && sender.checkPermission("limitedlives.get.self")) plugin.getMessages().get().get.self.newMessage()
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

            plugin.getMessages().get().convert.newMessage()
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
                plugin.getMessages().get().get.other.newMessage()
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
                            plugin.getMessages().get().withdraw.negative.newMessage().send(sender);
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
                plugin.getMessages().get().getAs(action + "." + e.getMessageKey(), JsonChatMessage.class).newMessage()
                        .replace("%amount%", lives)
                        .replace("%target%", playerName)
                        .replace("%min%", plugin.config.lives.min)
                        .replace("%max%", manager.getMaxLives())
                        .send(sender);
                return;
            }

            // Send message
            plugin.getMessages().get().getAs(action + "." + "self", JsonChatMessage.class).newMessage()
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
                plugin.getMessages().get().give.negative.newMessage().send(sender);
                return;
            }

            // Get target and player
            final List<OfflinePlayer> selectorTargets = sender.getSelector(2, OfflinePlayer.class)
                    .orElseFlatSingle(BukkitUtility::getOfflinePlayer);
            if (selectorTargets == null) return;
            final List<OfflinePlayer> targets = new ArrayList<>(selectorTargets);

            // Remove player from targets if present, can't give lives to self
            final Player player = sender.getPlayer();
            final UUID playerUuid = player.getUniqueId();
            targets.removeIf(target -> target.getUniqueId().equals(playerUuid));

            // No valid targets
            if (targets.isEmpty()) {
                plugin.getMessages().get().give.self.newMessage().send(sender);
                return;
            }
            final PlayerManager playerManager = new PlayerManager(plugin, player);

            // Check if player has +1 than min lives
            final int playerLives = playerManager.getLives();
            if (playerLives <= plugin.config.lives.min + 1) {
                plugin.getMessages().get().give.last_life.newMessage().send(sender);
                return;
            }

            // CLAMPING: Player doesn't have enough lives, give as many as possible
            final int maxTotalToGive = playerLives - (plugin.config.lives.min + 1);
            if (lives * targets.size() > maxTotalToGive) lives = maxTotalToGive / targets.size();
            if (lives <= 0) {
                plugin.getMessages().get().give.last_life.newMessage().send(sender);
                return;
            }

            // Loop through targets and send lives
            final String playerName = player.getName();
            for (final OfflinePlayer target : targets) {
                // CLAMPING: Target can't receive that many lives, give as many as possible
                final PlayerManager targetManager = new PlayerManager(plugin, target);
                int targetLivesToGive = lives;
                final int targetLives = targetManager.getLives();
                if (targetLives + targetLivesToGive > targetManager.getMaxLives()) targetLivesToGive = targetManager.getMaxLives() - targetLives;
                if (targetLivesToGive <= 0) continue;

                // Take lives from player and give to target
                final int newPlayerLives;
                final int newTargetLives;
                try {
                    newPlayerLives = playerManager.removeLives(targetLivesToGive, null);
                    newTargetLives = targetManager.addLives(targetLivesToGive);
                } catch (final ActionException e) {
                    // Shouldn't happen
                    sender.invalidArguments();
                    return;
                }

                // Send messages
                final String targetName = target.getName();
                plugin.getMessages().get().give.player.newMessage()
                        .replace("%player%", playerName)
                        .replace("%target%", targetName)
                        .replace("%playerlives%", newPlayerLives)
                        .replace("%targetlives%", newTargetLives)
                        .replace("%amount%", targetLivesToGive)
                        .send(sender);
                if (target instanceof Player) plugin.getMessages().get().give.target.newMessage()
                        .replace("%player%", playerName)
                        .replace("%target%", targetName)
                        .replace("%playerlives%", newPlayerLives)
                        .replace("%targetlives%", newTargetLives)
                        .replace("%amount%", targetLivesToGive)
                        .send((Player) target);
            }
            return;
        }

        // Get action
        final String action = sender.getArgument(0, String::toLowerCase);
        if (action == null || !sender.checkPermission("limitedlives." + action + ".other")) return;

        // Get targets and loop through
        final List<OfflinePlayer> targets = sender.getSelector(2, OfflinePlayer.class)
                .orElseFlatSingle(BukkitUtility::getOfflinePlayer);
        if (targets != null) for (final OfflinePlayer target : targets) {
            final String targetName = target.getName();

            // Get new lives after action
            final int newLives;
            int amount = lives;
            final PlayerManager manager = new PlayerManager(plugin, target);
            try {
                switch (action) {
                    // set <lives> <player>
                    case "set":
                        newLives = manager.setLives(amount);
                        break;
                    // add <lives> <player>
                    case "add":
                        newLives = manager.addLives(amount);
                        break;
                    // remove <lives> <player>
                    case "remove":
                        newLives = manager.removeLives(amount, null);
                        break;
                    // withdraw <lives> <player>
                    case "withdraw":
                        if (!sender.checkPlayer()) return;
                        if (amount <= 0) {
                            plugin.getMessages().get().withdraw.negative.newMessage().send(sender);
                            return;
                        }
                        final int currentLives = manager.getLives();
                        if (currentLives <= amount) amount = currentLives - 1; // Withdraw as many possible
                        if (amount <= plugin.config.lives.min) throw new LessThanMinLives();
                        newLives = manager.withdrawLives(sender.getPlayer(), amount);
                        break;
                    default:
                        sender.invalidArgumentByIndex(0);
                        return;
                }
            } catch (final ActionException e) {
                plugin.getMessages().get().getAs(action + "." + e.getMessageKey(), JsonChatMessage.class).newMessage()
                        .replace("%amount%", amount)
                        .replace("%target%", targetName)
                        .replace("%min%", plugin.config.lives.min)
                        .replace("%max%", manager.getMaxLives())
                        .send(sender);
                return;
            }

            // Send message
            plugin.getMessages().get().getAs(action + ".other", JsonChatMessage.class).newMessage()
                    .replace("%amount%", amount)
                    .replace("%target%", targetName)
                    .replace("%lives%", newLives)
                    .send(sender);
        }
    }

    @NotNull private static final List<String> NO_ARGS = Arrays.asList("get", "set", "add", "remove", "give", "withdraw", "convert");

    @Override @Nullable
    public Collection<String> onTabComplete(@NotNull AnnoyingSender sender) {
        // Check if commands enabled
        final Location location = sender.getLocationOfSender();
        if (location != null && !plugin.config.worlds_blacklist.isWorldEnabled(location.getWorld(), Feature.COMMANDS)) return null;
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
                if (cmdSender.hasPermission("limitedlives.get.other")) return sender.withSelectorKeys(BukkitUtility.getOnlinePlayerNames(), OfflinePlayer.class);
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
            if (cmdSender.hasPermission("limitedlives." + actionLower + ".other")) return sender.withSelectorKeys(BukkitUtility.getOnlinePlayerNames(), OfflinePlayer.class);
            if (cmdSender.hasPermission("limitedlives." + actionLower + ".self")) return Collections.singleton(cmdSender.getName());
        }

        return null;
    }
}
