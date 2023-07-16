package xyz.srnyx.limitedlives.commands;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.command.AnnoyingCommand;
import xyz.srnyx.annoyingapi.command.AnnoyingSender;
import xyz.srnyx.annoyingapi.message.AnnoyingMessage;

import xyz.srnyx.limitedlives.LimitedLives;


public class ReloadCmd implements AnnoyingCommand {
    @NotNull private final LimitedLives plugin;

    public ReloadCmd(@NotNull LimitedLives plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public LimitedLives getAnnoyingPlugin() {
        return plugin;
    }

    @Override @NotNull
    public String getName() {
        return "lifereload";
    }

    @Override @NotNull
    public String getPermission() {
        return "limitedlives.reload";
    }

    @Override
    public void onCommand(@NotNull AnnoyingSender sender) {
        plugin.reloadPlugin();
        new AnnoyingMessage(plugin, "reload").send(sender);
    }
}
