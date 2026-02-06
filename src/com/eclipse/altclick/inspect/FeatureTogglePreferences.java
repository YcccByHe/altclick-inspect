package com.eclipse.altclick.inspect;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

public final class FeatureTogglePreferences {
    private static final boolean DEFAULT_ENABLED = true;

    private FeatureTogglePreferences() {
    }

    public static boolean isEnabled() {
        return preferences().getBoolean(PluginConstants.PREF_ENABLED, DEFAULT_ENABLED);
    }

    public static void setEnabled(boolean enabled) {
        IEclipsePreferences preferences = preferences();
        preferences.putBoolean(PluginConstants.PREF_ENABLED, enabled);
        try {
            preferences.flush();
        } catch (BackingStoreException ignored) {
        }
        FeatureToggleCommandState.syncFromPreferences();
    }

    private static IEclipsePreferences preferences() {
        return InstanceScope.INSTANCE.getNode(PluginConstants.PLUGIN_ID);
    }
}
