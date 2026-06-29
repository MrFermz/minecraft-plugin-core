package com.mrfermz.mcplugins.core.menu;

import java.util.List;
import java.util.Objects;

/**
 * Describes one per-player option that the in-game menu can show and edit.
 * Feature plugins register their own items on enable (via {@link MenuRegistry});
 * the {@code Menu} plugin renders whatever is registered, and the value the
 * player picks is stored per player through {@link PlayerPreferenceService}.
 *
 * <p>This is the only contract a feature plugin needs to expose an option — it
 * never reaches into the Menu plugin (CLAUDE.md → plugins talk through core).
 *
 * <p>Build instances with the static factories: {@link #toggle}, {@link #choice},
 * {@link #number}, {@link #text}.
 *
 * @param key          unique, namespaced option key, e.g. {@code "healthbar.display"}
 * @param category     grouping label shown in the UI, e.g. {@code "Healthbar"}
 * @param title        human-readable label for the control
 * @param description  optional one-line help/tooltip ({@code null} for none)
 * @param type         which control to render
 * @param defaultValue the stored-string default when the player hasn't set it
 * @param options      the selectable options (only for {@link MenuItemType#CHOICE})
 * @param min          lower bound (only for {@link MenuItemType#NUMBER})
 * @param max          upper bound (only for {@link MenuItemType#NUMBER})
 * @param step         step size (only for {@link MenuItemType#NUMBER})
 */
public record MenuItem(
        String key,
        String category,
        String title,
        String description,
        MenuItemType type,
        String defaultValue,
        List<Option> options,
        double min,
        double max,
        double step) {

    /** A single selectable choice: the stored {@code value} and its display {@code label}. */
    public record Option(String value, String label) {
        public Option {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(label, "label");
        }
    }

    public MenuItem {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(defaultValue, "defaultValue");
        options = options == null ? List.of() : List.copyOf(options);
    }

    /** A boolean on/off option. */
    public static MenuItem toggle(String key, String category, String title,
                                  String description, boolean defaultOn) {
        return new MenuItem(key, category, title, description, MenuItemType.TOGGLE,
                Boolean.toString(defaultOn), List.of(), 0, 0, 0);
    }

    /** A pick-one option; {@code defaultValue} must be one of the option values. */
    public static MenuItem choice(String key, String category, String title, String description,
                                  List<Option> options, String defaultValue) {
        return new MenuItem(key, category, title, description, MenuItemType.CHOICE,
                defaultValue, options, 0, 0, 0);
    }

    /** A bounded numeric option. */
    public static MenuItem number(String key, String category, String title, String description,
                                  double min, double max, double step, double defaultValue) {
        return new MenuItem(key, category, title, description, MenuItemType.NUMBER,
                Double.toString(defaultValue), List.of(), min, max, step);
    }

    /** A free-text option. */
    public static MenuItem text(String key, String category, String title,
                                String description, String defaultValue) {
        return new MenuItem(key, category, title, description, MenuItemType.TEXT,
                defaultValue, List.of(), 0, 0, 0);
    }
}
