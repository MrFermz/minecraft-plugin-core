package com.mrfermz.mcplugins.core.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link SettingsRegistry}. Definitions are pure metadata (no state),
 * so a simple insertion-ordered map is enough. Registration happens on the main
 * thread during plugin enable and reads happen when a player opens the menu; the
 * map is synchronized as belt-and-braces.
 */
public final class DefaultSettingsRegistry implements SettingsRegistry {

    private final Map<String, SettingDefinition> byKey =
            Collections.synchronizedMap(new LinkedHashMap<>());

    @Override
    public void register(SettingDefinition definition) {
        byKey.put(definition.key(), definition);
    }

    @Override
    public Optional<SettingDefinition> get(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    @Override
    public List<SettingDefinition> all() {
        synchronized (byKey) {
            return new ArrayList<>(byKey.values());
        }
    }
}
