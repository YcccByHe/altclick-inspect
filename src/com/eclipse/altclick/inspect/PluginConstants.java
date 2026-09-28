package com.eclipse.altclick.inspect;

public final class PluginConstants {
    public static final String PLUGIN_ID = "com.eclipse.altclick.inspect";
    public static final String TOGGLE_COMMAND_ID = PLUGIN_ID + ".commands.toggleEnabled";
    public static final String PREF_ENABLED = "enabled";
    public static final String TOGGLE_STATE_ID = "org.eclipse.ui.commands.toggleState";
    public static final String ICON_TOGGLE_ENABLED = "icons/inspect-toggle.svg";
    public static final String ICON_TOGGLE_DISABLED = "icons/inspect-toggle-disabled.svg";
    public static final String EDITOR_HOOK_KEY = PLUGIN_ID + ".listenerHook";

    private PluginConstants() {
    }
}
