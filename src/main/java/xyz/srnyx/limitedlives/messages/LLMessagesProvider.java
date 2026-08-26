package xyz.srnyx.limitedlives.messages;

import org.jetbrains.annotations.NotNull;
import xyz.srnyx.annoyingapi.file.okaeri.ConfigBuilder;
import xyz.srnyx.annoyingapi.message.MessagesProvider;
import xyz.srnyx.limitedlives.LimitedLives;


public class LLMessagesProvider extends MessagesProvider {
    @NotNull private final LimitedLives plugin;

    public LLMessagesProvider(@NotNull LimitedLives plugin) {
        this.plugin = plugin;

        defaults
                .prefix("&6&lLIVES &8&l| &e")
                .p("&e")
                .s("&6");
    }

    @Override @NotNull
    public LimitedLives getAnnoyingPlugin() {
        return plugin;
    }

    @Override
    public void mutateBuilder(@NotNull ConfigBuilder builder) {
        builder.config(new LimitedMessages(plugin));
    }

    @Override @NotNull
    public LimitedMessages get() {
        return (LimitedMessages) messages;
    }
}
