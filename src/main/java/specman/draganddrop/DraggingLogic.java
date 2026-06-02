package specman.draganddrop;

import specman.EditException;
import specman.Specman;
import specman.undo.manager.UndoRecording;
import specman.view.CatchBereich;
import specman.view.SchrittSequenzView;
import specman.view.ZweigSchrittSequenzView;

import java.awt.*;

public class DraggingLogic {

    private final Specman specman;
    private final DropTargetFinder finder;

    public DraggingLogic(Specman specman) {
        this.specman = specman;
        this.finder = new DropTargetFinder(specman);
    }

    public void onDrag(Point cursor, DragSource dragSource) throws EditException {
        GlassPane glassPane = (GlassPane) specman.getGlassPane();
        glassPane.setVisible(false);
        DropTarget target = finder.find(cursor, dragSource);
        if (target != null) {
            showIndicator(glassPane, target, dragSource);
        }
    }

    public void onDrop(Point cursor, DragSource dragSource) throws EditException {
        DropTarget target = finder.find(cursor, dragSource);
        if (target == null) {
            return;
        }

        DragOperation operation = operationFor(dragSource);
        if (operation != null && operation.canDrop(target)) {
            try (UndoRecording ur = specman.composeUndo()) {
                operation.execute(target, specman);
            }
        }
        else if (dragSource instanceof DragSource.StepCreation stepCreation) {
            handleEmptySequenceDrop(stepCreation);
        }
    }

    private DragOperation operationFor(DragSource dragSource) {
        return switch (dragSource) {
            case DragSource.StepMove e -> new MoveStepOperation(e.label(), e.step(), specman);
            case DragSource.CaseBranchCreation ignored -> new InsertCaseBranchOperation(specman);
            case DragSource.StepCreation n -> new InsertStepOperation(n.stepClass(), specman);
            case DragSource.CatchSequenceCreation ignored -> new InsertCatchSequenceOperation(specman);
        };
    }

    private void handleEmptySequenceDrop(DragSource.StepCreation stepCreation) {
        SchrittSequenzView root = specman.getHauptSequenz();
        if (root.schritte.isEmpty() && stepCreation.stepClass() != CatchBereich.class) {
            specman.dropWelcomeMessage();
            new InsertStepOperation(stepCreation.stepClass(), specman).executeAppend(root);
        }
    }

    private void showIndicator(GlassPane glassPane, DropTarget target, DragSource dragSource) {
        if (target.referenceStep() == null) {
            return;
        }
        if (dragSource.isCaseBranchCreation()) {
            showCaseBranchIndicator(glassPane, (ZweigSchrittSequenzView) target.sequence());
            return;
        }
        Component panel = target.referenceCatch() != null
          ? target.referenceCatch()
          : target.referenceStep().getPanel();
        Point origin = javax.swing.SwingUtilities.convertPoint(panel, new Point(0, 0), specman);
        Rectangle bounds = panel.getBounds();
        bounds.setLocation(origin);

        int y = switch (target.position()) {
            case Before -> bounds.y;
            case After  -> bounds.y + bounds.height - DropTargetFinder.DROP_ZONE_HEIGHT;
        };
        glassPane.setInputRecBounds(bounds.x, y, bounds.width, DropTargetFinder.DROP_ZONE_HEIGHT);
        glassPane.setVisible(true);
    }

    private void showCaseBranchIndicator(GlassPane glassPane, ZweigSchrittSequenzView branch) {
        Component heading = branch.getUeberschrift();
        Component container = branch.getContainer();
        Point headingOrigin = javax.swing.SwingUtilities.convertPoint(heading, new Point(0, 0), specman);
        Point containerOrigin = javax.swing.SwingUtilities.convertPoint(container, new Point(0, 0), specman);
        int x = containerOrigin.x + container.getWidth() - DropTargetFinder.DROP_ZONE_HEIGHT;
        int topY = headingOrigin.y + branch.dragIndicatorTopOffset();
        int bottomY = containerOrigin.y + container.getHeight();
        glassPane.setInputRecBounds(x, topY, DropTargetFinder.DROP_ZONE_HEIGHT, bottomY - topY);
        glassPane.setVisible(true);
    }
}
