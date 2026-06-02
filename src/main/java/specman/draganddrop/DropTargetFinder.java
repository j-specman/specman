package specman.draganddrop;

import specman.Specman;
import specman.view.AbstractSchrittView;
import specman.view.CatchBereich;
import specman.view.SchrittSequenzView;

import java.util.List;

import javax.swing.*;
import java.awt.*;

import static specman.draganddrop.DragSource.Type.CaseBranchCreation;
import static specman.draganddrop.DragSource.Type.StepMove;
import static specman.view.RelativeStepPosition.After;
import static specman.view.RelativeStepPosition.Before;

/**
 * Finds the drop target for a drag operation by hit-testing the step tree against
 * a cursor position. Completely side-effect-free: no GlassPane updates, no insertions.
 * Returns null when no valid drop target is found.
 *
 * The traversal strategy is:
 *   findInSequence → findInStep → step.findDropTarget (local coords)
 *                              → step.checkHeadings   (global coords)
 *                              → unterSequenzen() recursion
 */
public class DropTargetFinder {

    static final int DROP_ZONE_HEIGHT = 5;

    private final Specman specman;

    public DropTargetFinder(Specman specman) {
        this.specman = specman;
    }

    public DropTarget find(Point cursor, DragSource dragSource) {
        if (isBlockedBySelf(cursor, dragSource)) {
            return null;
        }
        return findInSequence(cursor, specman.getHauptSequenz(), dragSource);
    }

    private boolean isBlockedBySelf(Point cursor, DragSource dragSource) {
        if (!dragSource.isStepMove()) {
            return false;
        }
        AbstractSchrittView step = ((DragSource.StepMove) dragSource).step();
        if (step.getParent().schritte.size() <= 1) {
            return true;
        }
        Rectangle bounds = boundsInSpecman(step.getPanel());
        return bounds.contains(cursor);
    }

    private DropTarget findInSequence(Point cursor, SchrittSequenzView seq, DragSource dragSource) {
        List<AbstractSchrittView> steps = seq.schritte;
        for (AbstractSchrittView step : steps) {
            DropTarget result = findInStep(cursor, step, steps, dragSource);
            if (result != null) {
                return result;
            }
        }
        CatchBereich catchBereich = seq.getCatchBereich();
        if (catchBereich != null) {
            // The CatchBereich is a solitary step that doesn't have siblings, so we pass an empty list here.
            DropTarget result = findInStep(cursor, catchBereich, List.of(), dragSource);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private DropTarget findInStep(Point cursor, AbstractSchrittView step, List<AbstractSchrittView> siblings, DragSource dragSource) {
        DropTarget t = checkInsertBefore(cursor, step, siblings, dragSource);
        if (t != null) {
            return t;
        }

        LocalCursor localCursor = new LocalCursor(
            SwingUtilities.convertPoint(specman, cursor, step.getPanel()), step.getPanel());
        try {
            t = dragSource.type() != CaseBranchCreation
              ? step.findDropTarget(localCursor, dragSource)
              : null;
            if (t != null) {
                return t;
            }
        }
        catch(UnsupportedDragSourceException udso) {
            // That's perfectly fine, it just means that the step doesn't support dropping this type of thing on it, so we can continue searching.
        }

        t = step.findHeadingDropTarget(localCursor, dragSource);
        if (t != null) {
            return t;
        }

        if (dragSource.isCaseBranchCreation()) {
            return null;
        }

        for (SchrittSequenzView seq : step.unterSequenzen()) {
            t = findInSequenceWithAscentToParent(cursor, seq, step, dragSource);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    private DropTarget findInSequenceWithAscentToParent(
            Point cursor, SchrittSequenzView seq,
            AbstractSchrittView parentStep, DragSource dragSource) {
        List<AbstractSchrittView> steps = seq.schritte;
        if (!steps.isEmpty()) {
            AbstractSchrittView lastStep = steps.get(steps.size() - 1);
            DropTarget ascentToParent = checkLastPixelsAscentToParent(cursor, lastStep, parentStep);
            if (ascentToParent != null) {
                return ascentToParent;
            }
        }
        return findInSequence(cursor, seq, dragSource);
    }

    private DropTarget checkLastPixelsAscentToParent(Point cursor, AbstractSchrittView lastInSeq, AbstractSchrittView parentStep) {
        if (parentStep.dropTargetSuppressesAscentToParent()) {
            return null;
        }
        Rectangle bounds = boundsInSpecman(lastInSeq.getPanel());
        if (!bounds.contains(cursor)) {
            return null;
        }
        if (cursor.y <= bounds.y + bounds.height - DROP_ZONE_HEIGHT) {
            return null;
        }
        return new DropTarget(parentStep.getParent(), parentStep, After);
    }

    private DropTarget checkInsertBefore(Point cursor, AbstractSchrittView step, List<AbstractSchrittView> siblings, DragSource dragSource) {
        if (dragSource.isCaseBranchCreation() || step != firstVisibleStep(siblings)) {
            return null;
        }
        Rectangle bounds = boundsInSpecman(step.getPanel());
        if (bounds.contains(cursor) && cursor.y < bounds.y + DROP_ZONE_HEIGHT) {
            return new DropTarget(step.getParent(), step, Before);
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
