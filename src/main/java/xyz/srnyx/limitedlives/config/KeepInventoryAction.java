package xyz.srnyx.limitedlives.config;

import org.bukkit.event.entity.PlayerDeathEvent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;


public enum KeepInventoryAction {
    KEEP(event -> {
        event.getDrops().clear();
        event.setKeepInventory(true);
    }),
    DROP(event -> event.setKeepInventory(false)),
    DESTROY(event -> {
        event.getDrops().clear();
        event.setKeepInventory(false);
    });

    @NotNull public final Consumer<PlayerDeathEvent> consumer;

    KeepInventoryAction(@NotNull Consumer<PlayerDeathEvent> consumer) {
        this.consumer = consumer;
    }

    @NotNull
    public static Optional<KeepInventoryAction> fromString(@Nullable String string) {
        if (string != null) try {
            return Optional.of(valueOf(string.toUpperCase()));
        } catch (final IllegalArgumentException e) {
            AnnoyingPlugin.log(Level.WARNING, "Invalid value in keep-inventory.actions: " + string);
        }
        return Optional.empty();
    }
}
