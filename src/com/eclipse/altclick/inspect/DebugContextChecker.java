package com.eclipse.altclick.inspect;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.ui.DebugUITools;

public class DebugContextChecker {
    public static boolean isSuspended() {
        IAdaptable context = DebugUITools.getDebugContext();
        IStackFrame frame = context != null
                ? context.getAdapter(IStackFrame.class)
                : null;
        return frame != null && frame.isSuspended();
    }
}
