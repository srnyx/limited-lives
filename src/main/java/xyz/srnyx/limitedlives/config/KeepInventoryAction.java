package xyz.srnyx.limitedlives.config;

import org.bukkit.event.entity.PlayerDeathEvent;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;


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
}
