package xyz.srnyx.limitedlives.managers.player.exception;

import org.jetbrains.annotations.NotNull;


public abstract class ActionException extends Exception {
    @NotNull
    public abstract String getMessageKey();
}
