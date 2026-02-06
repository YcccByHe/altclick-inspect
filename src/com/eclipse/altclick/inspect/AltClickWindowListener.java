package com.eclipse.altclick.inspect;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWindowListener;

public class AltClickWindowListener implements IWindowListener {
    private final IPartListener2 partListener = new EditorPartListener();
    private final Set<IWorkbenchWindow> windows =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<IWorkbenchPage> pages =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private final IPageListener pageListener = new IPageListener() {
        @Override
        public void pageOpened(IWorkbenchPage page) {
            registerPage(page);
        }

        @Override
        public void pageClosed(IWorkbenchPage page) {
            unregisterPage(page);
        }

        @Override
        public void pageActivated(IWorkbenchPage page) {
            registerPage(page);
        }
    };

    @Override
    public void windowOpened(IWorkbenchWindow window) {
        register(window);
    }

    @Override
    public void windowActivated(IWorkbenchWindow window) {
        register(window);
    }

    @Override
    public void windowClosed(IWorkbenchWindow window) {
        unregister(window);
    }

    @Override
    public void windowDeactivated(IWorkbenchWindow window) {
    }

    public void register(IWorkbenchWindow window) {
        if (window == null || !windows.add(window)) {
            return;
        }
        window.addPageListener(pageListener);
        for (IWorkbenchPage page : window.getPages()) {
            registerPage(page);
        }
    }

    private void unregister(IWorkbenchWindow window) {
        if (window == null || !windows.remove(window)) {
            return;
        }
        window.removePageListener(pageListener);
        for (IWorkbenchPage page : window.getPages()) {
            unregisterPage(page);
        }
    }

    private void registerPage(IWorkbenchPage page) {
        if (page != null && pages.add(page)) {
            page.addPartListener(partListener);
        }
    }

    private void unregisterPage(IWorkbenchPage page) {
        if (page != null && pages.remove(page)) {
            page.removePartListener(partListener);
        }
    }
}
