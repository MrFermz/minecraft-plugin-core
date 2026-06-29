package com.mrfermz.mcplugins.core.menu;

/**
 * The kind of control a {@link MenuItem} maps to in the in-game menu UI.
 *
 * <p>Values are always stored as text in the central DB regardless of type; the
 * type only tells the UI which input to render and how to parse the result.
 */
public enum MenuItemType {

    /** On/off — stored as {@code "true"}/{@code "false"}. */
    TOGGLE,

    /** One of a fixed set of options — stored as the chosen option's value. */
    CHOICE,

    /** A bounded number — stored as its decimal string. */
    NUMBER,

    /** Free text. */
    TEXT
}
