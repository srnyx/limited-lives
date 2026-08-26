package xyz.srnyx.limitedlives.config.damagecause;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


public enum CustomDamageCause {
    PLAYER_ATTACK;

    @NotNull @Contract(" -> new")
    public DamageCauseWrapper wrap() {
        return new DamageCauseWrapper(this);
    }
}
