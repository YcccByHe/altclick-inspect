package com.eclipse.altclick.inspect;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Point;

public class AltHoverHighlighter
        implements MouseMoveListener, MouseTrackListener, KeyListener, ITextPresentationListener {
    private final TextViewer viewer;
    private final StyledText text;
    private final ExpressionResolver resolver = new ExpressionResolver();

    private ExpressionRange activeRange;
    private boolean handCursorActive;

    public AltHoverHighlighter(TextViewer viewer) {
        this.viewer = viewer;
        this.text = viewer.getTextWidget();
    }

    @Override
    public void mouseMove(MouseEvent e) {
        if ((e.stateMask & SWT.ALT) == 0
                || !FeatureTogglePreferences.isEnabled()
                || !DebugContextChecker.isSuspended()) {
            clear();
            return;
        }
        applyRange(resolveAtPoint(e.x, e.y));
    }

    @Override
    public void mouseEnter(MouseEvent e) {
    }

    @Override
    public void mouseExit(MouseEvent e) {
        clear();
    }

    @Override
    public void mouseHover(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.ALT) {
            refreshAtCursorPosition();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.keyCode == SWT.ALT) {
            clear();
        }
    }

    @Override
    public void applyTextPresentation(TextPresentation presentation) {
        if (activeRange == null) {
            return;
        }
        IRegion extent = presentation.getExtent();
        if (extent == null
                || activeRange.getEnd() <= extent.getOffset()
                || extent.getOffset() + extent.getLength() <= activeRange.getStart()) {
            return;
        }
        StyleRange styleRange = new StyleRange(
                activeRange.getStart(),
                activeRange.getLength(),
                text.getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND),
                null);
        styleRange.underline = true;
        styleRange.underlineStyle = SWT.UNDERLINE_LINK;
        presentation.mergeStyleRange(styleRange);
    }

    public ExpressionRange resolveAtPoint(int x, int y) {
        int widgetOffset = text.getOffsetAtPoint(new Point(x, y));
        if (widgetOffset > 0 && text.getLocationAtOffset(widgetOffset).x > x) {
            widgetOffset--;
        }
        if (widgetOffset < 0) {
            return null;
        }
        int modelOffset = viewer.widgetOffset2ModelOffset(widgetOffset);
        if (modelOffset < 0) {
            return null;
        }
        return resolver.resolve(viewer.getDocument(), modelOffset);
    }

    public void applyRange(ExpressionRange range) {
        if (range == null) {
            clear();
            return;
        }
        if (!range.isSameRange(activeRange)) {
            ExpressionRange previous = activeRange;
            activeRange = range;
            invalidate(previous);
            invalidate(range);
        }
        if (!handCursorActive) {
            text.setCursor(text.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
            handCursorActive = true;
        }
    }

    public void clear() {
        if (activeRange != null) {
            ExpressionRange previous = activeRange;
            activeRange = null;
            invalidate(previous);
        }
        if (handCursorActive) {
            text.setCursor(null);
            handCursorActive = false;
        }
    }

    private void refreshAtCursorPosition() {
        if (!FeatureTogglePreferences.isEnabled() || !DebugContextChecker.isSuspended()) {
            clear();
            return;
        }
        Point cursor = text.toControl(text.getDisplay().getCursorLocation());
        if (!text.getClientArea().contains(cursor)) {
            clear();
            return;
        }
        applyRange(resolveAtPoint(cursor.x, cursor.y));
    }

    private void invalidate(ExpressionRange range) {
        if (range != null) {
            viewer.invalidateTextPresentation(range.getStart(), range.getLength());
        }
    }
}
