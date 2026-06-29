package com.mrfermz.mcplugins.core.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link MenuRegistry}. Items are pure metadata (no state), so a simple
 * insertion-ordered map is enough. Registration happens on the main thread during
 * plugin enable and reads happen when a player opens the menu; the map is
 * synchronized as belt-and-braces.
 */
public final class DefaultMenuRegistry implements MenuRegistry {

    private final Map<String, MenuItem> byKey =
            Collections.synchronizedMap(new LinkedHashMap<>());

    @Override
    public void register(MenuItem item) {
        byKey.put(item.key(), item);
    }

    @Override
    public Optional<MenuItem> get(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    @Override
    public List<MenuItem> all() {
        synchronized (byKey) {
            return new ArrayList<>(byKey.values());
        }
    }
}
