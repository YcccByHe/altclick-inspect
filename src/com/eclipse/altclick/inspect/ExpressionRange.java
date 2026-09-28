package com.eclipse.altclick.inspect;

public class ExpressionRange {
    private final int start;
    private final int length;

    public ExpressionRange(int start, int length) {
        this.start = start;
        this.length = length;
    }

    public int getStart() {
        return start;
    }

    public int getLength() {
        return length;
    }

    public int getEnd() {
        return start + length;
    }

    public boolean containsOffset(int offset) {
        return offset >= start && offset < getEnd();
    }

    public boolean isSameRange(ExpressionRange other) {
        if (other == null) {
            return false;
        }
        return start == other.start && length == other.length;
    }
}
