package com.eclipse.altclick.inspect;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.graphics.Point;

public class WordSelector {
    public static void selectWordAt(ITextViewer viewer, int x, int y) {
        if (viewer == null || viewer.getTextWidget() == null) {
            return;
        }
        if (viewer.getTextWidget().isDisposed()) {
            return;
        }
        ITextViewerExtension5 ext = viewer instanceof ITextViewerExtension5
                ? (ITextViewerExtension5) viewer
                : null;
        if (ext == null) {
            return;
        }

        int widgetOffset;
        try {
            widgetOffset = viewer.getTextWidget().getOffsetAtPoint(new Point(x, y));
        } catch (IllegalArgumentException ex) {
            return;
        }

        int modelOffset = ext.widgetOffset2ModelOffset(widgetOffset);
        if (modelOffset < 0) {
            return;
        }

        IDocument document = viewer.getDocument();
        if (document == null) {
            return;
        }

        try {
            int length = document.getLength();
            if (length == 0) {
                return;
            }

            int anchor = modelOffset;
            if (anchor >= length) {
                anchor = length - 1;
            }

            if (!isWordChar(document.getChar(anchor))) {
                if (anchor > 0 && isWordChar(document.getChar(anchor - 1))) {
                    anchor = anchor - 1;
                } else {
                    return;
                }
            }

            int start = anchor;
            int end = anchor + 1;
            while (start > 0 && isWordChar(document.getChar(start - 1))) {
                start--;
            }
            while (end < length && isWordChar(document.getChar(end))) {
                end++;
            }

            if (end <= start) {
                return;
            }

            ISelectionProvider provider = viewer.getSelectionProvider();
            if (provider == null) {
                return;
            }
            ITextSelection selection = new TextSelection(start, end - start);
            provider.setSelection(selection);
        } catch (BadLocationException ex) {
            return;
        }
    }

    private static boolean isWordChar(char c) {
        return Character.isJavaIdentifierPart(c);
    }
}
