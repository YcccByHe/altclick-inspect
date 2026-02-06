package com.eclipse.altclick.inspect;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

public class SelectionUtil {
    public static boolean hasNonEmptySelection(ITextViewer viewer) {
        if (viewer == null) {
            return false;
        }
        ISelectionProvider provider = viewer.getSelectionProvider();
        if (provider == null) {
            return false;
        }
        ISelection selection = provider.getSelection();
        if (!(selection instanceof ITextSelection)) {
            return false;
        }
        return ((ITextSelection) selection).getLength() > 0;
    }

    public static void selectWordAtPoint(ITextViewer viewer, int x, int y) {
        WordSelector.selectWordAt(viewer, x, y);
    }

    public static boolean selectExpression(ITextViewer viewer, ExpressionRange range) {
        if (viewer == null || range == null) {
            return false;
        }
        ISelectionProvider provider = viewer.getSelectionProvider();
        if (provider == null) {
            return false;
        }
        provider.setSelection(new TextSelection(range.getStart(), range.getLength()));
        return true;
    }
}
