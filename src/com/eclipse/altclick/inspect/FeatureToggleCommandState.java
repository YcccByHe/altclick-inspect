package com.eclipse.altclick.inspect;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

public final class FeatureToggleCommandState {
    private FeatureToggleCommandState() {
    }

    public static void syncFromPreferences() {
        updateCommandState(FeatureTogglePreferences.isEnabled());
    }

    private static void updateCommandState(boolean enabled) {
        if (!PlatformUI.isWorkbenchRunning()) {
            return;
        }
        Display display = PlatformUI.getWorkbench().getDisplay();
        Runnable updateTask = () -> {
            ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
            if (commandService == null) {
                return;
            }
            Command command = commandService.getCommand(PluginConstants.TOGGLE_COMMAND_ID);
            if (command == null || !command.isDefined()) {
                return;
            }
            State state = command.getState(PluginConstants.TOGGLE_STATE_ID);
            if (state != null) {
                state.setValue(Boolean.valueOf(enabled));
            }
            commandService.refreshElements(PluginConstants.TOGGLE_COMMAND_ID, null);
        };
        if (Display.getCurrent() == display) {
            updateTask.run();
            return;
        }
        display.asyncExec(updateTask);
    }
}
