package xyz.srnyx.limitedlives.managers.player.exception;

import org.jetbrains.annotations.NotNull;


public class MoreThanMaxLives extends ActionException {
    @Override @NotNull
    public String getMessageKey() {
        return "max";
    }
}
