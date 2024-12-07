package xyz.srnyx.limitedlives.managers.player.exception;

import org.jetbrains.annotations.NotNull;


public class LessThanMinLives extends ActionException {
    @Override @NotNull
    public String getMessageKey() {
        return "min";
    }
}
