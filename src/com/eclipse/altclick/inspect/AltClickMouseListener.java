package com.eclipse.altclick.inspect;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.texteditor.ITextEditor;

public class AltClickMouseListener extends MouseAdapter {
    private final ITextEditor editor;
    private final ITextViewer viewer;
    private final AltHoverHighlighter highlighter;

    public AltClickMouseListener(
            ITextEditor editor,
            ITextViewer viewer,
            AltHoverHighlighter highlighter) {
        this.editor = editor;
        this.viewer = viewer;
        this.highlighter = highlighter;
    }

    @Override
    public void mouseDown(MouseEvent e) {
        if ((e.stateMask & SWT.ALT) == 0 || e.button != 1) {
            return;
        }
        if (!FeatureTogglePreferences.isEnabled()) {
            return;
        }
        if (!DebugContextChecker.isSuspended()) {
            return;
        }
        if (viewer == null) {
            return;
        }

        ExpressionRange range = null;
        if (highlighter != null) {
            range = highlighter.resolveAtPointForClick(e.x, e.y);
            if (range == null) {
                range = highlighter.getActiveRange();
            }
        }

        ISelectionProvider selectionProvider = viewer.getSelectionProvider();
        ISelection previousSelection = selectionProvider != null ? selectionProvider.getSelection() : null;
        boolean hadManualSelection = previousSelection instanceof ITextSelection
                && ((ITextSelection) previousSelection).getLength() > 0;

        boolean expressionSelected = false;
        if (range != null) {
            if (!SelectionUtil.selectExpression(viewer, range)) {
                return;
            }
            expressionSelected = true;
            highlighter.applyRange(range);
        } else if (!SelectionUtil.hasNonEmptySelection(viewer)) {
            return;
        }
        if (!SelectionUtil.hasNonEmptySelection(viewer)) {
            return;
        }
        if (editor != null) {
            editor.setFocus();
        }
        try {
            InspectCommandExecutor.execute(editor);
        } finally {
            if (!expressionSelected || selectionProvider == null) {
                return;
            }
            if (hadManualSelection && previousSelection != null) {
                selectionProvider.setSelection(previousSelection);
                return;
            }
            selectionProvider.setSelection(new TextSelection(range.getEnd(), 0));
        }
    }
}
