package com.eclipse.altclick.inspect;

public final class PluginConstants {
    public static final String PLUGIN_ID = "com.eclipse.altclick.inspect";
    public static final String COMMAND_CATEGORY_ID = PLUGIN_ID + ".commands.category";
    public static final String TOGGLE_COMMAND_ID = PLUGIN_ID + ".commands.toggleEnabled";
    public static final String PREFERENCE_PAGE_ID = PLUGIN_ID + ".preferences.main";
    public static final String PREF_ENABLED = "enabled";
    public static final String TOGGLE_STATE_ID = "org.eclipse.ui.commands.toggleState";
    public static final String ICON_TOGGLE_ENABLED = "icons/inspect-toggle.svg";
    public static final String ICON_TOGGLE_DISABLED = "icons/inspect-toggle-disabled.svg";

    private PluginConstants() {
    }
}
