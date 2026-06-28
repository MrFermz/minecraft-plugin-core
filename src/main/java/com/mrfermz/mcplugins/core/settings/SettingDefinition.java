package com.mrfermz.mcplugins.core.settings;

import java.util.List;
import java.util.Objects;

/**
 * Describes one per-player setting that the in-game settings UI can show and
 * edit. Feature plugins register their own definitions on enable (via
 * {@link SettingsRegistry}); the {@code Settings} plugin renders whatever is
 * registered, and the value the player picks is stored per player through
 * {@link PlayerPreferenceService}.
 *
 * <p>This is the only contract a feature plugin needs to expose a setting — it
 * never reaches into the Settings plugin (CLAUDE.md → plugins talk through core).
 *
 * <p>Build instances with the static factories: {@link #toggle}, {@link #choice},
 * {@link #number}, {@link #text}.
 *
 * @param key          unique, namespaced setting key, e.g. {@code "healthbar.display"}
 * @param category     grouping label shown in the UI, e.g. {@code "Healthbar"}
 * @param title        human-readable label for the control
 * @param description  optional one-line help/tooltip ({@code null} for none)
 * @param type         which control to render
 * @param defaultValue the stored-string default when the player hasn't set it
 * @param options      the selectable options (only for {@link SettingType#CHOICE})
 * @param min          lower bound (only for {@link SettingType#NUMBER})
 * @param max          upper bound (only for {@link SettingType#NUMBER})
 * @param step         step size (only for {@link SettingType#NUMBER})
 */
public record SettingDefinition(
        String key,
        String category,
        String title,
        String description,
        SettingType type,
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

    public SettingDefinition {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(defaultValue, "defaultValue");
        options = options == null ? List.of() : List.copyOf(options);
    }

    /** A boolean on/off setting. */
    public static SettingDefinition toggle(String key, String category, String title,
                                           String description, boolean defaultOn) {
        return new SettingDefinition(key, category, title, description, SettingType.TOGGLE,
                Boolean.toString(defaultOn), List.of(), 0, 0, 0);
    }

    /** A pick-one setting; {@code defaultValue} must be one of the option values. */
    public static SettingDefinition choice(String key, String category, String title, String description,
                                           List<Option> options, String defaultValue) {
        return new SettingDefinition(key, category, title, description, SettingType.CHOICE,
                defaultValue, options, 0, 0, 0);
    }

    /** A bounded numeric setting. */
    public static SettingDefinition number(String key, String category, String title, String description,
                                           double min, double max, double step, double defaultValue) {
        return new SettingDefinition(key, category, title, description, SettingType.NUMBER,
                Double.toString(defaultValue), List.of(), min, max, step);
    }

    /** A free-text setting. */
    public static SettingDefinition text(String key, String category, String title,
                                         String description, String defaultValue) {
        return new SettingDefinition(key, category, title, description, SettingType.TEXT,
                defaultValue, List.of(), 0, 0, 0);
    }
}
