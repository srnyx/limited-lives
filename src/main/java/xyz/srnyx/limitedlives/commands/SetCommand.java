package xyz.srnyx.limitedlives.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingMessage;
import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.AnnoyingUtility;
import xyz.srnyx.annoyingapi.command.AnnoyingCommand;
import xyz.srnyx.annoyingapi.command.AnnoyingSender;

import xyz.srnyx.limitedlives.LimitedLives;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;


public class SetCommand implements AnnoyingCommand {
    @NotNull private final LimitedLives plugin;

    public SetCommand(@NotNull LimitedLives plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public AnnoyingPlugin getPlugin() {
        return plugin;
    }

    @Override @NotNull
    public String getName() {
        return "setlives";
    }

    @Override @NotNull
    public String getPermission() {
        return "limitedlives.set";
    }

    @Override @NotNull
    public Predicate<String[]> getArgsPredicate() {
        return args -> args.length > 0;
    }

    @Override
    public void onCommand(@NotNull AnnoyingSender sender) {
        final String[] args = sender.args;

        // Get lives
        final int lives;
        try {
            lives = Integer.parseInt(args[0]);
        } catch (final NumberFormatException e) {
            new AnnoyingMessage(plugin, "error.invalid-argument")
                    .replace("%argument%", args[0])
                    .send(sender);
            return;
        }

        // Check if lives is in range
        if (lives < plugin.config.livesMin || lives > plugin.config.livesMax) {
            new AnnoyingMessage(plugin, "error.invalid-argument")
                    .replace("%argument%", args[0])
                    .send(sender);
            return;
        }

        // No arguments
        if (args.length == 1 && sender.checkPlayer()) {
            plugin.lives.put(sender.getPlayer().getUniqueId(), lives);
            new AnnoyingMessage(plugin, "set.self")
                    .replace("%lives%", lives)
                    .send(sender);
            return;
        }

        // [<player>]
        if (args.length == 2) {
            final OfflinePlayer target = AnnoyingUtility.getOfflinePlayer(args[1]);
            if (target == null) {
                new AnnoyingMessage(plugin, "error.invalid-argument")
                        .replace("%argument%", args[1])
                        .send(sender);
                return;
            }
            plugin.lives.put(target.getUniqueId(), lives);

            // Send messages
            new AnnoyingMessage(plugin, "set.other")
                    .replace("%target%", target.getName())
                    .replace("%lives%", lives)
                    .send(sender);
            if (target.isOnline()) {
                new AnnoyingMessage(plugin, "set.self")
                        .replace("%lives%", lives)
                        .send((Player) target);
            }
            return;
        }

        new AnnoyingMessage(plugin, "error.invalid-arguments").send(sender);
    }

    @Override @Nullable
    public Collection<String> onTabComplete(@NotNull AnnoyingSender sender) {
        final int length = sender.args.length;
        if (length == 1) return Collections.singletonList("[<int>]");
        if (length == 2) return AnnoyingUtility.getOnlinePlayerNames();
        return null;
    }
}
