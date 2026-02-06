package com.eclipse.altclick.inspect;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ToggleAltClickHandler extends AbstractHandler implements IElementUpdater {
    private static final ImageDescriptor ENABLED_ICON = AbstractUIPlugin.imageDescriptorFromPlugin(
            PluginConstants.PLUGIN_ID,
            PluginConstants.ICON_TOGGLE_ENABLED);
    private static final ImageDescriptor DISABLED_ICON = AbstractUIPlugin.imageDescriptorFromPlugin(
            PluginConstants.PLUGIN_ID,
            PluginConstants.ICON_TOGGLE_DISABLED);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        boolean enabled = !FeatureTogglePreferences.isEnabled();
        FeatureTogglePreferences.setEnabled(enabled);
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        boolean enabled = FeatureTogglePreferences.isEnabled();
        element.setChecked(enabled);
        element.setIcon(enabled ? ENABLED_ICON : DISABLED_ICON);
    }
}
