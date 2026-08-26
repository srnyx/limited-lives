package xyz.srnyx.limitedlives.config.serdes;

import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.util.EnumMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.annoyingapi.libs.javautilities.manipulation.Mapper;
import xyz.srnyx.limitedlives.config.KeepInventoryAction;
import xyz.srnyx.limitedlives.config.KeepInventoryActions;

import java.util.LinkedHashMap;
import java.util.Map;


public class KeepInventoryActionsSerializer implements ObjectSerializer<KeepInventoryActions> {
    @Override
    public boolean supports(@NotNull Class<?> type) {
        return KeepInventoryActions.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NotNull KeepInventoryActions object, @NotNull SerializationData data, @NotNull GenericsDeclaration generics) {
        final Map<Object, KeepInventoryAction> map = new LinkedHashMap<>();
        map.put("default", object.def());
        map.putAll(object.exact());
        data.setValue(map);
    }

    @Override @Nullable
    public KeepInventoryActions deserialize(@NotNull DeserializationData data, @NotNull GenericsDeclaration generics) {
        final Map<String, Object> map = new LinkedHashMap<>(data.asMap());

        // Get default action
        final String defaultString = (String) map.remove("default");
        final KeepInventoryAction def = Mapper
                .toEnum(defaultString, KeepInventoryAction.class)
                .orElseThrow(() -> new OkaeriException(EnumMatcher.suggest(defaultString, KeepInventoryAction.class)));

        // Get exact actions
        final Map<Integer, KeepInventoryAction> exact = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            // Get count
            final int count = Mapper
                    .toInt(entry.getKey())
                    .orElseThrow(() -> new OkaeriException("Invalid keep inventory action count: " + entry.getKey()));

            // Get action
            final String actionString = (String) entry.getValue();
            final KeepInventoryAction action = Mapper
                    .toEnum(actionString, KeepInventoryAction.class)
                    .orElseThrow(() -> new OkaeriException(EnumMatcher.suggest(actionString, KeepInventoryAction.class)));

            exact.put(count, action);
        }

        return new KeepInventoryActions(def, exact);
    }
}
