package xyz.srnyx.limitedlives.config;

import org.jetbrains.annotations.NotNull;
import xyz.srnyx.annoyingapi.data.ItemData;
import xyz.srnyx.annoyingapi.file.okaeri.serdes.recipe.transformer.result.DataResultTransformer;
import xyz.srnyx.limitedlives.managers.player.PlayerManager;


public class ObtainingRecipeTransformer extends DataResultTransformer<LimitedConfig> {
    @Override
    public void transform(@NotNull ItemData data, @NotNull Context<LimitedConfig> context) {
        data.set(PlayerManager.ITEM_KEY, true);
    }
}
