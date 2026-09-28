package com.eclipse.altclick.inspect;

import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.texteditor.ITextEditor;

public class AltClickMouseListener implements Listener {
    private final ITextEditor editor;
    private final TextViewer viewer;
    private final AltHoverHighlighter highlighter;

    public AltClickMouseListener(
            ITextEditor editor,
            TextViewer viewer,
            AltHoverHighlighter highlighter) {
        this.editor = editor;
        this.viewer = viewer;
        this.highlighter = highlighter;
    }

    @Override
    public void handleEvent(Event event) {
        if ((event.stateMask & SWT.ALT) == 0 || event.button != 1) {
            return;
        }
        if (!FeatureTogglePreferences.isEnabled() || !DebugContextChecker.isSuspended()) {
            return;
        }
        ExpressionRange range = highlighter.resolveAtPoint(event.x, event.y);
        if (range == null) {
            return;
        }
        event.type = SWT.None;
        highlighter.applyRange(range);
        viewer.setSelection(new TextSelection(range.getStart(), range.getLength()));
        editor.setFocus();
        InspectCommandExecutor.execute(editor);
        viewer.setSelection(new TextSelection(range.getEnd(), 0));
    }
}
