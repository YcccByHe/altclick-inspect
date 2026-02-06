package com.eclipse.altclick.inspect;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.osgi.framework.Bundle;

public class InspectCommandExecutor {
    private static final String INSPECT_COMMAND_ID =
            "org.eclipse.jdt.debug.ui.commands.Inspect";

    public static void execute(IWorkbenchPart part) {
        IHandlerService service = null;
        if (part != null) {
            service = part.getSite().getService(IHandlerService.class);
        }
        if (service == null) {
            service = PlatformUI.getWorkbench().getService(IHandlerService.class);
        }
        if (service == null) {
            return;
        }
        try {
            service.executeCommand(INSPECT_COMMAND_ID, null);
        } catch (ExecutionException
                 | NotDefinedException
                 | NotEnabledException
                 | NotHandledException ex) {
            logError("Inspect command failed", ex);
        }
    }

    private static void logError(String message, Throwable error) {
        Bundle bundle = Platform.getBundle(PluginConstants.PLUGIN_ID);
        if (bundle == null) {
            return;
        }
        IStatus status = new Status(IStatus.ERROR, PluginConstants.PLUGIN_ID, message, error);
        Platform.getLog(bundle).log(status);
    }
}
