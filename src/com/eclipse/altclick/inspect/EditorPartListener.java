package com.eclipse.altclick.inspect;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.texteditor.ITextEditor;

public class EditorPartListener implements IPartListener2 {
    private static final String DATA_KEY =
            "com.eclipse.altclick.inspect.listenerHook";

    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
        register(partRef);
    }

    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
        register(partRef);
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
        register(partRef);
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {
        register(partRef);
    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
    }

    private void register(IWorkbenchPartReference partRef) {
        if (partRef == null) {
            return;
        }
        if (!(partRef.getPart(false) instanceof ITextEditor)) {
            return;
        }
        ITextEditor editor = (ITextEditor) partRef.getPart(false);
        ITextViewer viewer = editor.getAdapter(ITextViewer.class);
        if (viewer == null || viewer.getTextWidget() == null) {
            return;
        }

        StyledText text = viewer.getTextWidget();
        if (text.isDisposed() || text.getData(DATA_KEY) != null) {
            return;
        }

        AltHoverHighlighter highlighter = new AltHoverHighlighter(viewer);
        AltClickMouseListener clickListener = new AltClickMouseListener(editor, viewer, highlighter);
        text.setData(DATA_KEY, clickListener);
        text.addMouseListener(clickListener);
        text.addMouseMoveListener(highlighter);
        text.addMouseTrackListener(highlighter);
        text.addKeyListener(highlighter);
        DisposeListener disposeListener = e -> {
            highlighter.clear();
            text.removeMouseListener(clickListener);
            text.removeMouseMoveListener(highlighter);
            text.removeMouseTrackListener(highlighter);
            text.removeKeyListener(highlighter);
            text.setData(DATA_KEY, null);
        };
        text.addDisposeListener(disposeListener);
    }
}
