package xyz.srnyx.limitedlives.config;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import xyz.srnyx.annoyingapi.stats.Statable;

import java.util.Map;


public record KeepInventoryActions(@NotNull KeepInventoryAction def, @NotNull Map<Integer, KeepInventoryAction> exact) implements Statable {
    public KeepInventoryActions() {
        this(KeepInventoryAction.KEEP, Map.of(
                1, KeepInventoryAction.DROP,
                5, KeepInventoryAction.DROP,
                10, KeepInventoryAction.DESTROY));
    }

    @NotNull
    public KeepInventoryAction getAction(int deaths) {
        final KeepInventoryAction action = exact.get(deaths);
        return action != null ? action : def;
    }

   @Override @NotNull @Unmodifiable @Contract(" -> new")
   public JsonObject toStat() {
       final JsonObject json = new JsonObject();
       json.addProperty("default", def.name());
       json.addProperty("exact_size", exact.size());
       return json;
    }
}
