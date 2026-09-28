package com.eclipse.altclick.inspect;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.texteditor.ITextEditor;

public class EditorPartListener implements IPartListener2 {
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
        if (!isJavaEditor(partRef.getId())) {
            return;
        }
        IWorkbenchPart part = partRef.getPart(false);
        if (!(part instanceof ITextEditor)) {
            return;
        }
        ITextEditor editor = (ITextEditor) part;
        ITextViewer adapted = editor.getAdapter(ITextViewer.class);
        if (!(adapted instanceof TextViewer)) {
            return;
        }
        TextViewer viewer = (TextViewer) adapted;
        StyledText text = viewer.getTextWidget();
        if (text == null || text.isDisposed() || text.getData(PluginConstants.EDITOR_HOOK_KEY) != null) {
            return;
        }

        AltHoverHighlighter highlighter = new AltHoverHighlighter(viewer);
        text.setData(PluginConstants.EDITOR_HOOK_KEY, new AltClickMouseListener(editor, viewer, highlighter));
        viewer.addTextPresentationListener(highlighter);
        text.addMouseMoveListener(highlighter);
        text.addMouseTrackListener(highlighter);
        text.addKeyListener(highlighter);
        text.addDisposeListener(e -> viewer.removeTextPresentationListener(highlighter));
    }

    private boolean isJavaEditor(String editorId) {
        return JavaUI.ID_CU_EDITOR.equals(editorId) || JavaUI.ID_CF_EDITOR.equals(editorId);
    }
}
