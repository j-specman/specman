package specman.draganddrop;

import specman.Specman;
import specman.view.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

import static specman.view.RelativeStepPosition.After;
import static specman.view.RelativeStepPosition.Before;

/**
 * Finds the drop target for a drag operation by hit-testing the step tree against
 * a cursor position. Completely side-effect-free: no GlassPane updates, no insertions.
 * Returns null when no valid drop target is found.
 */
public class DropTargetFinder {

    static final int DROP_ZONE_HEIGHT = 5;

    private final Specman specman;

    public DropTargetFinder(Specman specman) {
        this.specman = specman;
    }

    public DropTarget find(Point cursor, DragSource dragSource) {
        SchrittSequenzView root = specman.getHauptSequenz();
        if (isBlockedByself(cursor, dragSource)) {
            return null;
        }
        return findInSequence(cursor, root.schritte, dragSource);
    }

    /**
     * Prevents dropping an existing step onto itself or into its own panel.
     */
    private boolean isBlockedByself(Point cursor, DragSource dragSource) {
        if (!(dragSource instanceof DragSource.ExistingStep existingStep)) {
            return false;
        }
        AbstractSchrittView step = existingStep.step();
        if (step.getParent().schritte.size() <= 1) {
            return true;
        }
        Point origin = SwingUtilities.convertPoint(step.getPanel(), 0, 0, specman);
        Rectangle bounds = step.getPanel().getVisibleRect();
        bounds.setLocation(origin);
        return bounds.contains(cursor);
    }

    /**
     * Recursively searches a list of steps for a drop target.
     * Returns null if the cursor is not within this sequence.
     */
    private DropTarget findInSequence(Point cursor, List<AbstractSchrittView> steps, DragSource dragSource) {
        if (steps.isEmpty()) {
            return null;
        }

        for (AbstractSchrittView step : steps) {
            if (step.getChangeInfo().isDeleted()) {
                continue;
            }

            DropTarget result = findInStep(cursor, step, steps, dragSource);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private DropTarget findInStep(Point cursor, AbstractSchrittView step, List<AbstractSchrittView> siblings, DragSource dragSource) {
        if (step == firstVisibleStep(siblings)) {
            DropTarget beforeFirst = checkInsertBefore(cursor, step);
            if (beforeFirst != null) {
                return beforeFirst;
            }
        }

        if (dragSource instanceof DragSource.NewCaseBranch) {
            return findCaseBranchTarget(cursor, step);
        }

        return switch (step) {
            case EinfacherSchrittView simple -> checkSimpleStep(cursor, simple);
            case IfSchrittView ifs -> checkIfStep(cursor, ifs, siblings, dragSource);
            case IfElseSchrittView ifel -> checkIfElseStep(cursor, ifel, siblings, dragSource);
            case SchleifenSchrittView loop -> checkLoopStep(cursor, loop, siblings, dragSource);
            case CaseSchrittView cas -> checkCaseStep(cursor, cas, siblings, dragSource);
            case SubsequenzSchrittView sub -> checkSubsequenzStep(cursor, sub, siblings, dragSource);
            case BreakSchrittView brk -> checkBreakStep(cursor, brk);
            default -> null;
        };
    }

    private DropTarget checkSimpleStep(Point cursor, EinfacherSchrittView step) {
        if (!boundsInSpecman(step.getPanel()).contains(cursor)) {
            return null;
        }
        return new DropTarget(step.getParent(), step, After);
    }

    private DropTarget checkIfElseStep(Point cursor, IfElseSchrittView step, List<AbstractSchrittView> siblings, DragSource dragSource) {
        int offset = (int) step.breiteLayoutspalteBerechnen();
        return firstOf(
            () -> checkBranchHeading(cursor, step.getIfSequenz(), offset),
            () -> findInSequenceWithEscapeUp(cursor, step.getIfSequenz().schritte, step, siblings, dragSource),
            () -> checkBranchHeading(cursor, step.getElseSequenz(), -offset),
            () -> findInSequenceWithEscapeUp(cursor, step.getElseSequenz().schritte, step, siblings, dragSource)
        );
    }

    private DropTarget checkIfStep(Point cursor, IfSchrittView step, List<AbstractSchrittView> siblings, DragSource dragSource) {
        return firstOf(
            () -> checkBranchHeading(cursor, step.getElseSequenz(), -(int) step.breiteLayoutspalteBerechnen()),
            () -> findInSequenceWithEscapeUp(cursor, step.getElseSequenz().schritte, step, siblings, dragSource)
        );
    }

    private DropTarget checkLoopStep(Point cursor, SchleifenSchrittView step, List<AbstractSchrittView> siblings, DragSource dragSource) {
        return firstOf(
            () -> checkLoopBars(cursor, step),
            () -> findInSequenceWithEscapeUp(cursor, step.getWiederholSequenz().schritte, step, siblings, dragSource),
            () -> boundsInSpecman(step.getTextShef()).contains(cursor)
                    ? new DropTarget(step.getWiederholSequenz(), step.getWiederholSequenz().schritte.get(0), Before)
                    : null
        );
    }

    private DropTarget checkCaseStep(Point cursor, CaseSchrittView step, List<AbstractSchrittView> siblings, DragSource dragSource) {
        DropTarget t = firstOf(
            () -> checkBranchHeading(cursor, step.getSonstSequenz(), (int) step.breiteLayoutspalteBerechnen()),
            () -> findInSequenceWithEscapeUp(cursor, step.getSonstSequenz().schritte, step, siblings, dragSource)
        );
        if (t != null) {
            return t;
        }
        List<ZweigSchrittSequenzView> caseSeqs = step.getCaseSequenzen();
        for (int i = 0; i < caseSeqs.size(); i++) {
            ZweigSchrittSequenzView caseSeq = caseSeqs.get(i);
            int offset = (i == 0) ? -(int) step.breiteLayoutspalteBerechnen() : 0;
            t = firstOf(
                () -> checkBranchHeading(cursor, caseSeq, offset),
                () -> findInSequenceWithEscapeUp(cursor, caseSeq.schritte, step, siblings, dragSource)
            );
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    private DropTarget checkSubsequenzStep(Point cursor, SubsequenzSchrittView step, List<AbstractSchrittView> siblings, DragSource dragSource) {
        return firstOf(
            () -> checkSubsequenzBar(cursor, step),
            () -> findInSequenceWithEscapeUp(cursor, step.getSequenz().schritte, step, siblings, dragSource),
            () -> boundsInSpecman(step.getTextShef()).contains(cursor)
                    ? new DropTarget(step.getSubsequenz(), step.getSubsequenz().schritte.get(0), Before)
                    : null
        );
    }

    private DropTarget checkBreakStep(Point cursor, BreakSchrittView step) {
        return checkBreakBar(cursor, step);
    }

    /**
     * Searches a nested sequence. If the cursor is in the "last pixels" of the last step,
     * the drop belongs AFTER the parent step in the outer sequence.
     */
    private DropTarget findInSequenceWithEscapeUp(
            Point cursor, List<AbstractSchrittView> steps,
            AbstractSchrittView parentStep, List<AbstractSchrittView> parentSiblings,
            DragSource dragSource) {
        if (steps.isEmpty()) {
            return null;
        }
        if (!(parentStep.getParent() != null &&
              parentStep.getParent().getParent() instanceof WhileWhileSchrittView)) {
            AbstractSchrittView lastStep = steps.get(steps.size() - 1);
            DropTarget escapeUp = checkLastPixelsEscape(cursor, lastStep, parentStep);
            if (escapeUp != null) {
                return escapeUp;
            }
        }

        return findInSequence(cursor, steps, dragSource);
    }

    /**
     * When cursor is in the bottom DROP_ZONE_HEIGHT pixels of the last step in a nested
     * sequence, the drop target should be after the parent step in the enclosing sequence.
     */
    private DropTarget checkLastPixelsEscape(Point cursor, AbstractSchrittView lastInSeq, AbstractSchrittView parentStep) {
        Rectangle bounds = boundsInSpecman(lastInSeq.getPanel());
        if (!bounds.contains(cursor)) {
            return null;
        }
        if (cursor.y <= bounds.y + bounds.height - DROP_ZONE_HEIGHT) {
            return null;
        }
        return new DropTarget(parentStep.getParent(), parentStep, After);
    }

    private DropTarget checkInsertBefore(Point cursor, AbstractSchrittView step) {
        Point origin = SwingUtilities.convertPoint(step.getPanel(), 0, 0, specman);
        Rectangle bounds = step.getPanel().getBounds();
        bounds.setLocation(origin);
        if (bounds.contains(cursor) && cursor.y < bounds.y + DROP_ZONE_HEIGHT) {
            return new DropTarget(step.getParent(), step, Before);
        }
        return null;
    }

    /**
     * Cursor on branch heading = insert Before the first step in that branch.
     */
    private DropTarget checkBranchHeading(Point cursor, ZweigSchrittSequenzView branch, int xOffset) {
        Point origin = SwingUtilities.convertPoint(branch.getUeberschrift(), 0, 0, specman);
        if (xOffset < 0) {
            origin.x += xOffset;
        }
        Rectangle bounds = branch.getUeberschrift().getVisibleRect();
        bounds.setLocation(origin);
        bounds.width += Math.abs(xOffset);
        if (!bounds.contains(cursor)) {
            return null;
        }
        AbstractSchrittView firstInBranch = branch.schritte.get(0);
        return new DropTarget(branch, firstInBranch, Before);
    }

    private DropTarget checkLoopBars(Point cursor, SchleifenSchrittView step) {
        JPanel leftBar = step.getLinkerBalken();
        JPanel bottomBar = step.getUntererBalken();
        if (leftBar != null && boundsInSpecman(leftBar).contains(cursor)) {
            return new DropTarget(step.getParent(), step, After);
        }
        if (bottomBar != null && boundsInSpecman(bottomBar).contains(cursor)) {
            return new DropTarget(step.getParent(), step, After);
        }
        return null;
    }

    private DropTarget checkSubsequenzBar(Point cursor, SubsequenzSchrittView step) {
        if (boundsInSpecman(step.getTextShef()).contains(cursor)) {
            return new DropTarget(step.getParent(), step, After);
        }
        return null;
    }

    private DropTarget checkBreakBar(Point cursor, BreakSchrittView step) {
        JPanel panel = step.getPanel();
        if (panel != null && boundsInSpecman(panel).contains(cursor)) {
            return new DropTarget(step.getParent(), step, After);
        }
        return null;
    }

    /**
     * For CASE_BRANCH drags: find which case heading the cursor is over.
     */
    private DropTarget findCaseBranchTarget(Point cursor, AbstractSchrittView step) {
        if (!(step instanceof CaseSchrittView caseStep)) {
            return null;
        }
        ZweigSchrittSequenzView sonstSeq = caseStep.getSonstSequenz();
        if (headingBoundsWithOffset(sonstSeq, (int) caseStep.breiteLayoutspalteBerechnen()).contains(cursor)) {
            return new DropTarget(sonstSeq, step, After);
        }
        List<ZweigSchrittSequenzView> cases = caseStep.getCaseSequenzen();
        for (int i = 0; i < cases.size(); i++) {
            ZweigSchrittSequenzView seq = cases.get(i);
            int offset = (i == 0) ? -(int) caseStep.breiteLayoutspalteBerechnen() : 0;
            if (headingBoundsWithOffset(seq, offset).contains(cursor)) {
                return new DropTarget(seq, step, After);
            }
        }
        return null;
    }

    private Rectangle headingBoundsWithOffset(ZweigSchrittSequenzView branch, int xOffset) {
        Point origin = SwingUtilities.convertPoint(branch.getUeberschrift(), 0, 0, specman);
        Rectangle bounds = branch.getUeberschrift().getVisibleRect();
        bounds.setLocation(origin);
        if (xOffset < 0) {
            origin.x += xOffset;
            bounds.width += Math.abs(xOffset);
        }
        return bounds;
    }

    @SafeVarargs
    private DropTarget firstOf(Supplier<DropTarget>... candidates) {
        for (Supplier<DropTarget> candidate : candidates) {
            DropTarget t = candidate.get();
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    private Rectangle boundsInSpecman(JComponent comp) {
        Point origin = SwingUtilities.convertPoint(comp, 0, 0, specman);
        Rectangle bounds = comp.getVisibleRect();
        bounds.setLocation(origin);
        return bounds;
    }

    private AbstractSchrittView firstVisibleStep(List<AbstractSchrittView> steps) {
        return steps.stream()
            .filter(s -> !s.getChangeInfo().isDeleted() && !s.getChangeInfo().isSourceStep())
            .findFirst()
            .orElse(null);
    }
}
