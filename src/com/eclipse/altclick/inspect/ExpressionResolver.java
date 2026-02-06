package com.eclipse.altclick.inspect;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.swt.graphics.Point;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpressionResolver {
    private static final int[] OFFSET_DELTAS = {0, -1, 1, -2, 2};

    private IDocument cachedDocument;
    private long cachedModificationStamp = IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
    private CompilationUnit cachedCompilationUnit;

    public ExpressionRange resolveAtPoint(
            ITextViewer viewer,
            int x,
            int y,
            ExpressionRange activeRange) {
        if (viewer == null || viewer.getTextWidget() == null) {
            return null;
        }
        if (viewer.getTextWidget().isDisposed()) {
            return null;
        }

        ITextViewerExtension5 extension = viewer instanceof ITextViewerExtension5
                ? (ITextViewerExtension5) viewer
                : null;
        if (extension == null) {
            return null;
        }

        final int widgetOffset;
        try {
            widgetOffset = viewer.getTextWidget().getOffsetAtPoint(new Point(x, y));
        } catch (IllegalArgumentException ex) {
            return null;
        }

        int modelOffset = extension.widgetOffset2ModelOffset(widgetOffset);
        if (modelOffset < 0) {
            return null;
        }
        return resolveAtModelOffset(viewer, modelOffset, activeRange);
    }

    public ExpressionRange resolveAtModelOffset(
            ITextViewer viewer,
            int modelOffset,
            ExpressionRange activeRange) {
        IDocument document = viewer != null ? viewer.getDocument() : null;
        if (document == null || document.getLength() == 0) {
            return null;
        }

        int safeOffset = Math.max(0, Math.min(modelOffset, document.getLength() - 1));
        int anchor = findAnchor(document, safeOffset);
        if (anchor < 0) {
            return null;
        }

        List<Candidate> candidates = collectCandidates(document, anchor);
        List<Candidate> containing = filterContaining(candidates, safeOffset);

        ExpressionRange selected = selectPreferredRange(document, safeOffset, containing, candidates);
        if (selected != null) {
            return selected;
        }
        return null;
    }

    private List<Candidate> collectCandidates(IDocument document, int anchor) {
        List<Candidate> candidates = new ArrayList<>();
        CompilationUnit unit = getCompilationUnit(document);
        if (unit == null) {
            return candidates;
        }

        Map<String, Candidate> unique = new LinkedHashMap<>();
        for (int delta : OFFSET_DELTAS) {
            int probe = anchor + delta;
            if (probe < 0 || probe >= document.getLength()) {
                continue;
            }
            ASTNode node = NodeFinder.perform(unit, probe, 0);
            if (node == null) {
                continue;
            }
            collectFromNodePath(node, anchor, unique, document, document.getLength());
        }
        candidates.addAll(unique.values());
        candidates.sort((a, b) -> Integer.compare(a.range.getLength(), b.range.getLength()));
        return candidates;
    }

    private void collectFromNodePath(
            ASTNode node,
            int anchor,
            Map<String, Candidate> unique,
            IDocument document,
            int documentLength) {
        for (ASTNode cursor = node; cursor != null; cursor = cursor.getParent()) {
            if (cursor instanceof Expression) {
                addCandidate(unique, (Expression) cursor, false, documentLength, anchor);
            }
            if (cursor instanceof IfStatement) {
                IfStatement statement = (IfStatement) cursor;
                addControlConditionCandidate(
                        unique,
                        statement,
                        statement.getExpression(),
                        anchor,
                        document,
                        documentLength);
            } else if (cursor instanceof WhileStatement) {
                WhileStatement statement = (WhileStatement) cursor;
                addControlConditionCandidate(
                        unique,
                        statement,
                        statement.getExpression(),
                        anchor,
                        document,
                        documentLength);
            } else if (cursor instanceof DoStatement) {
                DoStatement statement = (DoStatement) cursor;
                addControlConditionCandidate(
                        unique,
                        statement,
                        statement.getExpression(),
                        anchor,
                        document,
                        documentLength);
            } else if (cursor instanceof EnhancedForStatement) {
                EnhancedForStatement statement = (EnhancedForStatement) cursor;
                addControlConditionCandidate(
                        unique,
                        statement,
                        statement.getExpression(),
                        anchor,
                        document,
                        documentLength);
            } else if (cursor instanceof ForStatement) {
                ForStatement statement = (ForStatement) cursor;
                addControlConditionCandidate(
                        unique,
                        statement,
                        statement.getExpression(),
                        anchor,
                        document,
                        documentLength);
            } else if (cursor instanceof SwitchStatement) {
                SwitchStatement statement = (SwitchStatement) cursor;
                addControlConditionCandidate(
                        unique,
                        statement,
                        statement.getExpression(),
                        anchor,
                        document,
                        documentLength);
            }
        }
    }

    private void addControlConditionCandidate(
            Map<String, Candidate> unique,
            Statement statement,
            Expression expression,
            int anchor,
            IDocument document,
            int documentLength) {
        if (expression == null) {
            return;
        }
        int expressionStart = expression.getStartPosition();
        int expressionEnd = expressionStart + expression.getLength();
        boolean inKeyword = isOnControlKeywordToken(document, statement, expressionStart, anchor);
        boolean inExpression = anchor >= expressionStart && anchor < expressionEnd;
        if (inKeyword || inExpression) {
            addCandidate(unique, expression, inKeyword, documentLength, anchor);
        }
    }

    private void addCandidate(
            Map<String, Candidate> unique,
            Expression expression,
            boolean keywordBoost,
            int documentLength,
            int anchor) {
        if (expression == null) {
            return;
        }
        if (!isEvaluableExpression(expression, anchor, keywordBoost)) {
            return;
        }
        int start = expression.getStartPosition();
        int length = expression.getLength();
        if (start < 0 || length <= 0 || start + length > documentLength) {
            return;
        }
        String key = start + ":" + length;
        Candidate existing = unique.get(key);
        if (existing == null) {
            unique.put(
                    key,
                    new Candidate(
                            new ExpressionRange(start, length),
                            expression,
                            keywordBoost));
            return;
        }
        if (keywordBoost) {
            existing.keywordBoost = true;
        }
    }

    private List<Candidate> filterContaining(List<Candidate> candidates, int offset) {
        List<Candidate> containing = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (candidate.range.containsOffset(offset)) {
                containing.add(candidate);
            }
        }
        containing.sort((a, b) -> Integer.compare(a.range.getLength(), b.range.getLength()));
        return containing;
    }

    private ExpressionRange selectPreferredRange(
            IDocument document,
            int offset,
            List<Candidate> containing,
            List<Candidate> allCandidates) {
        if (!containing.isEmpty()) {
            Candidate innermost = containing.get(0);
            char current = charAtSafe(document, offset);
            char leftToken = findNonWhitespaceBackward(document, offset);
            char rightToken = findNonWhitespaceForward(document, offset);
            HitContext hitContext = detectHitContext(current, leftToken, rightToken);

            if (hitContext == HitContext.INDEX_BOUNDARY) {
                Candidate array = firstByType(containing, ArrayAccess.class);
                if (array != null) {
                    return array.range;
                }
            }

            if (hitContext == HitContext.OPERATOR) {
                Candidate infix = firstByType(containing, InfixExpression.class);
                if (infix != null) {
                    return infix.range;
                }
            }

            if (hitContext == HitContext.CALL_DELIMITER) {
                Candidate invocation = firstByType(containing, MethodInvocation.class);
                if (invocation != null) {
                    return invocation.range;
                }
            }

            if (hitContext == HitContext.DOT) {
                Candidate chain = firstChainExpression(containing);
                if (chain != null) {
                    return chain.range;
                }
            }

            if (innermost.expression instanceof org.eclipse.jdt.core.dom.SimpleName) {
                ExpressionRange expanded = resolveSimpleNameChainRange(innermost, containing);
                if (expanded != null) {
                    return expanded;
                }
            }

            return innermost.range;
        }

        Candidate nearest = findNearestBoundaryCandidate(allCandidates, offset);
        if (nearest != null) {
            return nearest.range;
        }

        for (Candidate candidate : allCandidates) {
            if (candidate.keywordBoost) {
                return candidate.range;
            }
        }
        return null;
    }

    private HitContext detectHitContext(char current, char leftToken, char rightToken) {
        if (current == '[' || current == ']' || leftToken == '[' || rightToken == ']') {
            return HitContext.INDEX_BOUNDARY;
        }
        if (current == '.') {
            return HitContext.DOT;
        }
        if (isInfixOperatorChar(current)
                || (Character.isWhitespace(current)
                && (isInfixOperatorChar(leftToken) || isInfixOperatorChar(rightToken)))) {
            return HitContext.OPERATOR;
        }
        if (current == '(' || current == ')' || current == ','
                || (Character.isWhitespace(current)
                && (leftToken == '(' || leftToken == ',' || rightToken == ')' || rightToken == ','))) {
            return HitContext.CALL_DELIMITER;
        }
        if (Character.isJavaIdentifierPart(current)) {
            return HitContext.IDENTIFIER;
        }
        return HitContext.OTHER;
    }

    private boolean isEvaluableExpression(Expression expression, int anchor, boolean keywordBoost) {
        if (expression == null) {
            return false;
        }
        if (expression instanceof org.eclipse.jdt.core.dom.Annotation
                || expression instanceof org.eclipse.jdt.core.dom.LambdaExpression
                || expression instanceof org.eclipse.jdt.core.dom.TypeLiteral
                || expression instanceof org.eclipse.jdt.core.dom.CreationReference
                || expression instanceof org.eclipse.jdt.core.dom.ExpressionMethodReference
                || expression instanceof org.eclipse.jdt.core.dom.SuperMethodReference
                || expression instanceof org.eclipse.jdt.core.dom.TypeMethodReference) {
            return false;
        }
        if (isInsideTypeContext(expression)) {
            return false;
        }
        if (expression instanceof org.eclipse.jdt.core.dom.SimpleName) {
            ASTNode parent = expression.getParent();
            if (parent instanceof QualifiedName
                    && ((QualifiedName) parent).getName() == expression
                    && !keywordBoost) {
                return false;
            }
            if (parent instanceof org.eclipse.jdt.core.dom.MethodDeclaration
                    || parent instanceof org.eclipse.jdt.core.dom.VariableDeclarationFragment
                    || parent instanceof org.eclipse.jdt.core.dom.TypeParameter
                    || parent instanceof org.eclipse.jdt.core.dom.ImportDeclaration
                    || parent instanceof org.eclipse.jdt.core.dom.PackageDeclaration) {
                return false;
            }
            if (!containsOffset(expression, anchor) && !keywordBoost) {
                return false;
            }
        }
        return true;
    }

    private boolean isInsideTypeContext(Expression expression) {
        ASTNode cursor = expression;
        while (cursor != null) {
            if (cursor instanceof org.eclipse.jdt.core.dom.Type
                    || cursor instanceof org.eclipse.jdt.core.dom.TypeParameter
                    || cursor instanceof org.eclipse.jdt.core.dom.ImportDeclaration
                    || cursor instanceof org.eclipse.jdt.core.dom.PackageDeclaration) {
                return true;
            }
            if (cursor instanceof Statement
                    || cursor instanceof org.eclipse.jdt.core.dom.VariableDeclarationFragment
                    || cursor instanceof org.eclipse.jdt.core.dom.MethodDeclaration
                    || cursor instanceof org.eclipse.jdt.core.dom.Initializer
                    || cursor instanceof org.eclipse.jdt.core.dom.LambdaExpression
                    || cursor instanceof org.eclipse.jdt.core.dom.CompilationUnit) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    private boolean isOnControlKeywordToken(
            IDocument document,
            Statement statement,
            int expressionStart,
            int anchor) {
        if (document == null || anchor < 0 || anchor >= document.getLength()) {
            return false;
        }
        String expectedKeyword = expectedKeyword(statement);
        if (expectedKeyword == null) {
            return false;
        }
        int statementStart = statement.getStartPosition();
        if (statementStart < 0 || statementStart >= expressionStart) {
            return false;
        }

        int keywordStart = skipWhitespaceForward(document, statementStart, expressionStart);
        if (keywordStart < 0 || keywordStart >= expressionStart) {
            return false;
        }
        int keywordEnd = readIdentifierEnd(document, keywordStart, expressionStart);
        if (keywordEnd <= keywordStart) {
            return false;
        }
        try {
            String actualKeyword = document.get(keywordStart, keywordEnd - keywordStart);
            if (!expectedKeyword.equals(actualKeyword)) {
                return false;
            }
        } catch (BadLocationException ex) {
            return false;
        }
        return anchor >= keywordStart && anchor < keywordEnd;
    }

    private String expectedKeyword(Statement statement) {
        if (statement instanceof IfStatement) {
            return "if";
        }
        if (statement instanceof WhileStatement) {
            return "while";
        }
        if (statement instanceof ForStatement || statement instanceof EnhancedForStatement) {
            return "for";
        }
        if (statement instanceof SwitchStatement) {
            return "switch";
        }
        return null;
    }

    private int skipWhitespaceForward(IDocument document, int start, int limit) {
        int cursor = Math.max(0, start);
        while (cursor < limit) {
            char current = charAtSafe(document, cursor);
            if (current == '\0') {
                return -1;
            }
            if (!Character.isWhitespace(current)) {
                break;
            }
            cursor++;
        }
        return cursor;
    }

    private int readIdentifierEnd(IDocument document, int start, int limit) {
        int cursor = Math.max(0, start);
        while (cursor < limit) {
            char current = charAtSafe(document, cursor);
            if (current == '\0' || !Character.isJavaIdentifierPart(current)) {
                break;
            }
            cursor++;
        }
        return cursor;
    }

    private Candidate firstByType(List<Candidate> candidates, Class<?> expressionType) {
        for (Candidate candidate : candidates) {
            if (expressionType.isInstance(candidate.expression)) {
                return candidate;
            }
        }
        return null;
    }

    private Candidate firstChainExpression(List<Candidate> candidates) {
        for (Candidate candidate : candidates) {
            if (isChainExpression(candidate.expression)) {
                return candidate;
            }
        }
        return null;
    }

    private Candidate findByRange(List<Candidate> candidates, int start, int length) {
        for (Candidate candidate : candidates) {
            if (candidate.range.getStart() == start && candidate.range.getLength() == length) {
                return candidate;
            }
        }
        return null;
    }

    private ExpressionRange resolveSimpleNameChainRange(
            Candidate innermost,
            List<Candidate> containing) {
        ASTNode current = innermost.expression;
        ExpressionRange best = innermost.range;
        ASTNode parent = current.getParent();
        while (parent != null && isReceiverOrQualifierLink(parent, current)) {
            Candidate candidate = findByRange(containing, parent.getStartPosition(), parent.getLength());
            if (candidate != null) {
                best = candidate.range;
            }
            current = parent;
            parent = current.getParent();
        }
        return best;
    }

    private boolean isReceiverOrQualifierLink(ASTNode parent, ASTNode child) {
        if (parent instanceof MethodInvocation) {
            MethodInvocation invocation = (MethodInvocation) parent;
            return invocation.getExpression() == child || invocation.getName() == child;
        }
        if (parent instanceof QualifiedName) {
            return ((QualifiedName) parent).getQualifier() == child;
        }
        if (parent instanceof org.eclipse.jdt.core.dom.FieldAccess) {
            return ((org.eclipse.jdt.core.dom.FieldAccess) parent).getExpression() == child;
        }
        if (parent instanceof org.eclipse.jdt.core.dom.SuperFieldAccess) {
            return ((org.eclipse.jdt.core.dom.SuperFieldAccess) parent).getQualifier() == child;
        }
        return false;
    }

    private boolean isChainExpression(Expression expression) {
        return expression instanceof MethodInvocation
                || expression instanceof QualifiedName
                || expression instanceof org.eclipse.jdt.core.dom.FieldAccess
                || expression instanceof org.eclipse.jdt.core.dom.SuperFieldAccess;
    }

    private Candidate findNearestBoundaryCandidate(List<Candidate> candidates, int offset) {
        Candidate nearest = null;
        int minDistance = Integer.MAX_VALUE;
        for (Candidate candidate : candidates) {
            int startDistance = Math.abs(offset - candidate.range.getStart());
            int endDistance = Math.abs(offset - candidate.range.getEnd());
            int distance = Math.min(startDistance, endDistance);
            if (distance > 1) {
                continue;
            }
            if (distance < minDistance) {
                minDistance = distance;
                nearest = candidate;
                continue;
            }
            if (distance == minDistance
                    && nearest != null
                    && candidate.range.getLength() < nearest.range.getLength()) {
                nearest = candidate;
            }
        }
        return nearest;
    }

    private char charAtSafe(IDocument document, int offset) {
        if (document == null) {
            return '\0';
        }
        if (offset < 0 || offset >= document.getLength()) {
            return '\0';
        }
        try {
            return document.getChar(offset);
        } catch (BadLocationException ex) {
            return '\0';
        }
    }

    private char findNonWhitespaceBackward(IDocument document, int offset) {
        if (document == null) {
            return '\0';
        }
        int index = Math.min(offset - 1, document.getLength() - 1);
        while (index >= 0) {
            char current = charAtSafe(document, index);
            if (current == '\0') {
                return '\0';
            }
            if (!Character.isWhitespace(current)) {
                return current;
            }
            index--;
        }
        return '\0';
    }

    private char findNonWhitespaceForward(IDocument document, int offset) {
        if (document == null) {
            return '\0';
        }
        int index = Math.max(offset + 1, 0);
        while (index < document.getLength()) {
            char current = charAtSafe(document, index);
            if (current == '\0') {
                return '\0';
            }
            if (!Character.isWhitespace(current)) {
                return current;
            }
            index++;
        }
        return '\0';
    }

    private boolean isInfixOperatorChar(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '%'
                || c == '=' || c == '!' || c == '<' || c == '>' || c == '&' || c == '|'
                || c == '^' || c == '?';
    }

    private boolean containsOffset(Expression expression, int offset) {
        if (expression == null) {
            return false;
        }
        int start = expression.getStartPosition();
        int end = start + expression.getLength();
        return offset >= start && offset < end;
    }

    private int findAnchor(IDocument document, int offset) {
        int len = document.getLength();
        if (len == 0) {
            return -1;
        }
        return Math.max(0, Math.min(offset, len - 1));
    }

    private CompilationUnit getCompilationUnit(IDocument document) {
        if (document == null) {
            return null;
        }
        long stamp = getModificationStamp(document);
        if (cachedCompilationUnit != null
                && cachedDocument == document
                && stamp == cachedModificationStamp) {
            return cachedCompilationUnit;
        }

        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(false);
        parser.setStatementsRecovery(true);
        parser.setBindingsRecovery(false);
        parser.setSource(document.get().toCharArray());

        ASTNode astNode = parser.createAST(null);
        if (!(astNode instanceof CompilationUnit)) {
            return null;
        }
        cachedDocument = document;
        cachedModificationStamp = stamp;
        cachedCompilationUnit = (CompilationUnit) astNode;
        return cachedCompilationUnit;
    }

    private long getModificationStamp(IDocument document) {
        if (!(document instanceof IDocumentExtension4)) {
            String text = document.get();
            return (((long) text.hashCode()) << 32) ^ text.length();
        }
        IDocumentExtension4 extension = (IDocumentExtension4) document;
        long stamp = extension.getModificationStamp();
        return stamp == IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP
                ? ((((long) document.get().hashCode()) << 32) ^ document.getLength())
                : stamp;
    }

    private static final class Candidate {
        private final ExpressionRange range;
        private final Expression expression;
        private boolean keywordBoost;

        private Candidate(ExpressionRange range, Expression expression, boolean keywordBoost) {
            this.range = range;
            this.expression = expression;
            this.keywordBoost = keywordBoost;
        }
    }

    private enum HitContext {
        INDEX_BOUNDARY,
        OPERATOR,
        CALL_DELIMITER,
        DOT,
        IDENTIFIER,
        OTHER
    }
}
