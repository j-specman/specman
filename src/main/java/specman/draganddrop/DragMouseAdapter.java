package specman.draganddrop;

import specman.EditException;
import specman.Specman;
import specman.StepButtonBar;
import specman.editarea.InteractiveStepFragment;
import specman.editarea.stepnumberlabel.BreakCatchScrollMouseAdapter;
import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.view.AbstractSchrittView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class DragMouseAdapter extends MouseAdapter {

    private final Specman specman;
    private final StepButtonBar stepButtonBar;
    private final DraggingLogic draggingLogic;
    private final JWindow floatingStepWindow = new JWindow();
    private JTextField floatingLabel;

    public DragMouseAdapter(Specman specman, StepButtonBar stepButtonBar) {
        this.specman = specman;
        this.stepButtonBar = stepButtonBar;
        this.draggingLogic = new DraggingLogic(specman);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (BreakCatchScrollMouseAdapter.userWantsToScroll(e)) {
            return;
        }
        floatingLabel = new JTextField(floatingLabelText(e));
        floatingLabel.setBounds(new Rectangle(150, 15));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (BreakCatchScrollMouseAdapter.userWantsToScroll(e)) {
            return;
        }
        if (isDragGuarded(e)) {
            specman.setCursor(Cursor.getDefaultCursor());
            return;
        }
        try {
            Point cursor = toCursorInSpecman(e);
            floatingStepWindow.add(floatingLabel);
            floatingStepWindow.pack();
            floatingStepWindow.setLocation(toScreenPoint(e));
            floatingStepWindow.setVisible(true);
            draggingLogic.onDrag(cursor, toDragSource(e));
        }
        catch (EditException ex) {
            specman.showError(ex);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (BreakCatchScrollMouseAdapter.userWantsToScroll(e)) {
            return;
        }
        if (isDragGuarded(e)) {
            specman.setCursor(Cursor.getDefaultCursor());
            return;
        }
        try {
            floatingStepWindow.setVisible(false);
            floatingStepWindow.remove(floatingLabel);
            Point cursor = toCursorInSpecman(e, -2);
            draggingLogic.onDrop(cursor, toDragSource(e));
        }
        catch (EditException ex) {
            specman.showError(ex);
        }
        finally {
            specman.getGlassPane().setVisible(false);
            specman.setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (BreakCatchScrollMouseAdapter.userWantsToScroll(e)) {
            return;
        }
        if (e.getSource() instanceof StepnumberLabel && isDragGuarded(e)) {
            specman.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (BreakCatchScrollMouseAdapter.userWantsToScroll(e)) {
            return;
        }
        specman.setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        specman.getGlassPane().setVisible(false);
    }

    private DragSource toDragSource(MouseEvent e) {
        if (e.getSource() instanceof StepnumberLabel label) {
            AbstractSchrittView step = specman.findStep(label);
            return new DragSource.ExistingStep(label, step);
        }
        return stepButtonBar.dragSourceFor((JButton) e.getSource());
    }

    /** Guard: returns true when dragging should be blocked (sole step, deleted, source marker). */
    private boolean isDragGuarded(MouseEvent e) {
        if (!(e.getSource() instanceof InteractiveStepFragment)) {
            return false;
        }
        AbstractSchrittView step = specman.findStep((InteractiveStepFragment) e.getSource());
        if (step.getChangeInfo().isDeleted() || step.getChangeInfo().isSourceStep()) {
            return true;
        }
        long movableCount = step.getParent().schritte.stream()
            .filter(s -> !s.getChangeInfo().isDeleted() && !s.getChangeInfo().isSourceStep())
            .count();
        return movableCount <= 1;
    }

    private String floatingLabelText(MouseEvent e) {
        if (e.getSource() instanceof InteractiveStepFragment) {
            AbstractSchrittView step = specman.findStep((InteractiveStepFragment) e.getSource());
            var id = step.getId();
            return id != null ? "Schritt " + id : " ";
        }
        return "Neuer Schritt";
    }

    private Point toCursorInSpecman(MouseEvent e) {
        return toCursorInSpecman(e, 0);
    }

    private Point toCursorInSpecman(MouseEvent e, int yOffset) {
        return SwingUtilities.convertPoint(
            (Component) e.getSource(),
            (int) e.getPoint().getX(),
            (int) e.getPoint().getY() + yOffset,
            specman
        );
    }

    private Point toScreenPoint(MouseEvent e) {
        Point p = new Point((int) e.getPoint().getX() + 3, (int) e.getPoint().getY() + 3);
        SwingUtilities.convertPointToScreen(p, (Component) e.getSource());
        return p;
    }
}
