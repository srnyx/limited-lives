package xyz.srnyx.limitedlives.commands;

import org.jetbrains.annotations.NotNull;
import xyz.srnyx.annoyingapi.command.AnnoyingSender;
import xyz.srnyx.limitedlives.LimitedLives;


public class ReloadCmd extends xyz.srnyx.limitedlives.commands.generated.LifereloadCmdGen {
    public ReloadCmd(@NotNull LimitedLives plugin) {
        super(plugin);
    }

    @Override
    public void onCommand(@NotNull AnnoyingSender sender) {
        plugin.reloadPlugin();
        plugin.getMessages().get().reload.newMessage().send(sender);
    }
}
