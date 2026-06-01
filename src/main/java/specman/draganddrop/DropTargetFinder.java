package specman.draganddrop;

import specman.Specman;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;
import specman.view.WhileWhileSchrittView;
import specman.view.ZweigSchrittSequenzView;

import java.util.List;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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
        return findInSequence(cursor, specman.getHauptSequenz().schritte, dragSource);
    }

    private boolean isBlockedBySelf(Point cursor, DragSource dragSource) {
        if (!(dragSource instanceof DragSource.ExistingStep existingStep)) {
            return false;
        }
        AbstractSchrittView step = existingStep.step();
        if (step.getParent().schritte.size() <= 1) {
            return true;
        }
        Rectangle bounds = boundsInSpecman(step.getPanel());
        return bounds.contains(cursor);
    }

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
        if (!(dragSource instanceof DragSource.NewCaseBranch) && step == firstVisibleStep(siblings)) {
            DropTarget t = checkInsertBefore(cursor, step);
            if (t != null) {
                return t;
            }
        }

        Point localCursor = SwingUtilities.convertPoint(specman, cursor, step.getPanel());
        DropTarget t = !(dragSource instanceof DragSource.NewCaseBranch)
            ? step.findDropTarget(localCursor, dragSource)
            : null;
        if (t != null) {
            return t;
        }

        List<BranchHeadingZone> zones = step.getBranchHeadingZones(dragSource);
        for (BranchHeadingZone zone : zones) {
            t = (dragSource instanceof DragSource.NewCaseBranch)
                ? checkBranchHeadingForNewCaseBranch(cursor, zone.branch, zone.xOffset, step)
                : checkBranchHeading(cursor, zone.branch, zone.xOffset);
            if (t != null) {
                return t;
            }
        }

        if (dragSource instanceof DragSource.NewCaseBranch) {
            return null;
        }

        for (SchrittSequenzView seq : step.unterSequenzen()) {
            t = findInSequenceWithEscapeUp(cursor, seq.schritte, step, siblings, dragSource);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

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
        Rectangle bounds = boundsInSpecman(step.getPanel());
        if (bounds.contains(cursor) && cursor.y < bounds.y + DROP_ZONE_HEIGHT) {
            return new DropTarget(step.getParent(), step, Before);
        }
        return null;
    }

    private DropTarget checkBranchHeadingForNewCaseBranch(Point cursor, ZweigSchrittSequenzView branch, int xOffset, AbstractSchrittView parentStep) {
        Rectangle bounds = headingBoundsWithOffset(branch, xOffset);
        if (!bounds.contains(cursor)) {
            return null;
        }
        return new DropTarget(branch, parentStep, After);
    }

    /** Hit-tests a branch heading. Returns a DropTarget for inserting Before the first step in the branch. */
    private DropTarget checkBranchHeading(Point cursor, ZweigSchrittSequenzView branch, int xOffset) {
        if (!headingBoundsWithOffset(branch, xOffset).contains(cursor)) {
            return null;
        }
        return new DropTarget(branch, branch.schritte.get(0), Before);
    }

    private Rectangle headingBoundsWithOffset(ZweigSchrittSequenzView branch, int xOffset) {
        Point origin = SwingUtilities.convertPoint(branch.getUeberschrift(), 0, 0, specman);
        if (xOffset < 0) {
            origin.x += xOffset;
        }
        Rectangle bounds = branch.getUeberschrift().getVisibleRect();
        bounds.setLocation(origin);
        bounds.width += Math.abs(xOffset);
        return bounds;
    }

    /** Used by step views in checkHeadings to convert global cursor into component-local coords. */
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
