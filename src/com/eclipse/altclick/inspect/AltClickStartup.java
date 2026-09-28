package com.eclipse.altclick.inspect;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class AltClickStartup implements IStartup {
    @Override
    public void earlyStartup() {
        Display display = PlatformUI.getWorkbench().getDisplay();
        display.asyncExec(() -> {
            display.addFilter(SWT.MouseDown, event -> {
                Object hook = event.widget.getData(PluginConstants.EDITOR_HOOK_KEY);
                if (hook instanceof AltClickMouseListener) {
                    ((AltClickMouseListener) hook).handleEvent(event);
                }
            });
            IWorkbench workbench = PlatformUI.getWorkbench();
            AltClickWindowListener listener = new AltClickWindowListener();
            workbench.addWindowListener(listener);
            for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
                listener.register(window);
            }
            FeatureToggleCommandState.syncFromPreferences();
        });
    }
}
