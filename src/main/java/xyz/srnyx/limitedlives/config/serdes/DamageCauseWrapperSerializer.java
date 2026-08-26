package xyz.srnyx.limitedlives.config.serdes;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import xyz.srnyx.annoyingapi.libs.javautilities.manipulation.Mapper;
import xyz.srnyx.limitedlives.config.damagecause.CustomDamageCause;
import xyz.srnyx.limitedlives.config.damagecause.DamageCauseWrapper;


public class DamageCauseWrapperSerializer extends BidirectionalTransformer<String, DamageCauseWrapper> {
    @Override @NotNull
    public GenericsPair<String, DamageCauseWrapper> getPair() {
        return genericsPair(String.class, DamageCauseWrapper.class);
    }

    @Override @NotNull
    public DamageCauseWrapper leftToRight(@NotNull String data, @NotNull SerdesContext serdesContext) {
        // CustomDamageCause
        final CustomDamageCause customCause = Mapper.toEnum(data, CustomDamageCause.class).orElse(null);
        if (customCause != null) return new DamageCauseWrapper(customCause);

        // EntityDamageEvent.DamageCause
        final EntityDamageEvent.DamageCause cause = Mapper.toEnum(data, EntityDamageEvent.DamageCause.class).orElse(null);
        if (cause != null) return new DamageCauseWrapper(cause);

        // Invalid
        throw new IllegalArgumentException("Invalid damage cause: " + data);
    }

    @Override @NotNull
    public String rightToLeft(@NotNull DamageCauseWrapper data, @NotNull SerdesContext serdesContext) {
        return data.raw;
    }
}
