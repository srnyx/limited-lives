package xyz.srnyx.limitedlives.managers.player.exception;

import org.jetbrains.annotations.NotNull;


public class RecipeNotSet extends ActionException {
    @Override @NotNull
    public String getMessageKey() {
        return "recipe-not-set";
    }
}
