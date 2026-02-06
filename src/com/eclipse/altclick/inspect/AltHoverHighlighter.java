package com.eclipse.altclick.inspect;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;

public class AltHoverHighlighter implements MouseMoveListener, MouseTrackListener, KeyListener {
    private final ITextViewer viewer;
    private final StyledText text;
    private final ExpressionResolver resolver = new ExpressionResolver();

    private ExpressionRange activeRange;
    private int activeWidgetStart = -1;
    private int activeWidgetLength = 0;
    private StyleRange[] replacedStyleRanges;

    public AltHoverHighlighter(ITextViewer viewer) {
        this.viewer = viewer;
        this.text = viewer != null ? viewer.getTextWidget() : null;
    }

    @Override
    public void mouseMove(MouseEvent e) {
        if (!FeatureTogglePreferences.isEnabled()
                || !isAltPressed(e.stateMask)
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
            if (!FeatureTogglePreferences.isEnabled()) {
                clear();
                return;
            }
            refreshAtCursorPosition();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.keyCode == SWT.ALT) {
            clear();
        }
    }

    public ExpressionRange getActiveRange() {
        return activeRange;
    }

    public ExpressionRange resolveAtPoint(int x, int y) {
        if (viewer == null || text == null || text.isDisposed()) {
            return null;
        }
        return resolver.resolveAtPoint(viewer, x, y, activeRange);
    }

    public ExpressionRange resolveAtPointForClick(int x, int y) {
        if (viewer == null || text == null || text.isDisposed()) {
            return null;
        }
        return resolver.resolveAtPoint(viewer, x, y, null);
    }

    public void applyRange(ExpressionRange range) {
        if (viewer == null || text == null || text.isDisposed()) {
            return;
        }
        if (range == null) {
            clear();
            return;
        }

        ITextViewerExtension5 extension = viewer instanceof ITextViewerExtension5
                ? (ITextViewerExtension5) viewer
                : null;
        if (extension == null) {
            clear();
            return;
        }

        int widgetStart = extension.modelOffset2WidgetOffset(range.getStart());
        int widgetEnd = extension.modelOffset2WidgetOffset(range.getEnd());
        if (widgetStart < 0 || widgetEnd < 0 || widgetEnd <= widgetStart) {
            clear();
            return;
        }

        int widgetLength = widgetEnd - widgetStart;
        if (activeRange != null
                && activeRange.isSameRange(range)
                && activeWidgetStart == widgetStart
                && activeWidgetLength == widgetLength) {
            text.setCursor(text.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
            return;
        }

        clearStyleOnly();
        replacedStyleRanges = text.getStyleRanges(widgetStart, widgetLength, true);
        StyleRange styleRange = new StyleRange(widgetStart, widgetLength, null, null);
        styleRange.background = resolveBackground(widgetStart, replacedStyleRanges);
        styleRange.foreground = text.getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND);
        styleRange.underline = true;
        styleRange.underlineStyle = SWT.UNDERLINE_LINK;
        text.setStyleRange(styleRange);
        text.setCursor(text.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        activeRange = range;
        activeWidgetStart = widgetStart;
        activeWidgetLength = widgetLength;
    }

    public void clear() {
        if (text == null || text.isDisposed()) {
            return;
        }
        clearStyleOnly();
        text.setCursor(null);
        activeRange = null;
    }

    private void clearStyleOnly() {
        if (text == null || text.isDisposed()) {
            return;
        }
        if (activeWidgetStart < 0 || activeWidgetLength <= 0) {
            activeWidgetStart = -1;
            activeWidgetLength = 0;
            replacedStyleRanges = null;
            return;
        }
        if (replacedStyleRanges != null) {
            text.replaceStyleRanges(activeWidgetStart, activeWidgetLength, cloneRanges(replacedStyleRanges));
        } else {
            StyleRange clearRange = new StyleRange(activeWidgetStart, activeWidgetLength, null, null);
            clearRange.foreground = null;
            clearRange.background = null;
            clearRange.underline = false;
            text.setStyleRange(clearRange);
        }
        viewer.invalidateTextPresentation();
        activeWidgetStart = -1;
        activeWidgetLength = 0;
        replacedStyleRanges = null;
    }

    private void refreshAtCursorPosition() {
        if (text == null
                || text.isDisposed()
                || !FeatureTogglePreferences.isEnabled()
                || !DebugContextChecker.isSuspended()) {
            clear();
            return;
        }
        Point cursor = text.toControl(text.getDisplay().getCursorLocation());
        if (cursor.x < 0 || cursor.y < 0 || cursor.x > text.getClientArea().width
                || cursor.y > text.getClientArea().height) {
            clear();
            return;
        }
        applyRange(resolveAtPoint(cursor.x, cursor.y));
    }

    private boolean isAltPressed(int stateMask) {
        return (stateMask & SWT.ALT) != 0;
    }

    private Color resolveBackground(int widgetStart, StyleRange[] styles) {
        if (styles != null) {
            for (StyleRange style : styles) {
                if (style != null && style.background != null) {
                    return style.background;
                }
            }
        }
        if (text == null || text.isDisposed()) {
            return null;
        }
        try {
            int line = text.getLineAtOffset(widgetStart);
            return text.getLineBackground(line);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private StyleRange[] cloneRanges(StyleRange[] ranges) {
        if (ranges == null) {
            return null;
        }
        StyleRange[] cloned = new StyleRange[ranges.length];
        for (int i = 0; i < ranges.length; i++) {
            cloned[i] = new StyleRange(ranges[i]);
        }
        return cloned;
    }
}
