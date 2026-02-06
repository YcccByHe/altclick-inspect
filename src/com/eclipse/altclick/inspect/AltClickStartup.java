package com.eclipse.altclick.inspect;

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
