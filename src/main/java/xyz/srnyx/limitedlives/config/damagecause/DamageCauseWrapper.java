package xyz.srnyx.limitedlives.config.damagecause;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.annoyingapi.stats.Statable;

import java.util.Objects;


public class DamageCauseWrapper implements Statable {
    @NotNull public final String raw;

    public DamageCauseWrapper(@NotNull EntityDamageEvent.DamageCause cause) {
        this.raw = cause.name();
    }

    public DamageCauseWrapper(@NotNull CustomDamageCause cause) {
        this.raw = cause.name();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        if (obj instanceof DamageCauseWrapper wrapper) return raw.equals(wrapper.raw);
        if (obj instanceof EntityDamageEvent.DamageCause cause) return raw.equals(cause.name());
        if (obj instanceof CustomDamageCause cause) return raw.equals(cause.name());
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw);
    }

    @Override @NotNull
    public String toString() {
        return raw;
    }

    @Override @NotNull
    public JsonElement toStat() {
        return new JsonPrimitive(raw);
    }
}
