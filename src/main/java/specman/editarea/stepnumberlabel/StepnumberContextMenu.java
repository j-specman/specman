package specman.editarea.stepnumberlabel;

import specman.graphics.IconReader;
import specman.Specman;
import specman.undo.UndoableFlatNumberingToggled;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;
import static specman.Specman.editor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class StepnumberContextMenu implements MouseListener {
  private final JPopupMenu popup;
  private final JMenuItem delete;
  private final JMenuItem left;
  private final JMenuItem right;
  private final JMenuItem up;
  private final JMenuItem down;
  private final JCheckBoxMenuItem toggleFlatNumbering;
  private AbstractSchrittView currentStep;
  private StepnumberLabel initiatingLabel;

  static StepnumberContextMenu instance = new StepnumberContextMenu();

  private StepnumberContextMenu() {
    popup = new JPopupMenu();
    delete = createDeleteItem();
    left = createLeftItem();
    right = createRightItem();
    up = createUpItem();
    down = createDownItem();
    toggleFlatNumbering = createSubnumberingItem();
  }

  private JMenuItem createUpItem() {
    return createItem("Move up", "arrow-up", e -> {
      try(UndoRecording ur = editor().composeUndo()) {
        currentStep.moveCoCatchUpUDBL(initiatingLabel);
      }
    });
  }

  private JMenuItem createDownItem() {
    return createItem("Move down", "arrow-down", e -> {
      try(UndoRecording ur = editor().composeUndo()) {
        currentStep.moveCoCatchDownUDBL(initiatingLabel);
      }
    });
  }

  private JCheckBoxMenuItem createSubnumberingItem() {
    JCheckBoxMenuItem item = new JCheckBoxMenuItem("Flache Nummerierung");
    item.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try (UndoRecording ur = editor().composeUndo()) {
          currentStep.toggleFlatNumbering(toggleFlatNumbering.getState());
          editor().addEdit(new UndoableFlatNumberingToggled(currentStep, toggleFlatNumbering.getState()));
        }
      }
    });
    popup.add(item);
    return item;
  }

  private JMenuItem createItem(String label, String iconBasename, ActionListener actionListener) {
    ImageIcon icon = IconReader.readImageIcon(iconBasename);
    JMenuItem item = new JMenuItem(label, icon);
    item.addActionListener(actionListener);
    popup.add(item);
    return item;
  }

  private JMenuItem createDeleteItem() {
    return createItem("Löschen", "loeschen",
      e -> editor().deleteStepADBL(currentStep, initiatingLabel));
  }

  private JMenuItem createLeftItem() {
    return createItem("Move left", "arrow-left",
      e -> editor().moveBranchSequenceLeftADBL(currentStep, initiatingLabel));
  }

  private JMenuItem createRightItem() {
    return createItem("Move right", "arrow-right",
      e -> editor().moveBranchSequenceRightADBL(currentStep, initiatingLabel));
  }

  private StepnumberLabel label(MouseEvent e) {
    return (StepnumberLabel) e.getComponent();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (SwingUtilities.isRightMouseButton(e)) {
      AbstractSchrittView step = editor().findStep(label(e));
      initMenuForStep(step, label(e));
      popup.show(e.getComponent(), e.getX(), e.getY());
    }
  }

  private void initMenuForStep(AbstractSchrittView currentStep, StepnumberLabel initiatingLabel) {
    this.currentStep = currentStep;
    this.initiatingLabel = initiatingLabel;
    this.delete.setEnabled(currentStep.allowsDeletion(initiatingLabel));
    this.toggleFlatNumbering.setVisible(currentStep.getFlatNumbering() != null);
    if (toggleFlatNumbering.isVisible()) {
      toggleFlatNumbering.setState(currentStep.getFlatNumbering());
    }
    initLeftRightMenuItems();
    initUpDownMenuItems();
  }

  private void initLeftRightMenuItems() {
    boolean leftAllowed = currentStep.allowsBranchSequenceMoveLeft(initiatingLabel);
    boolean rightAllowed = currentStep.allowsBranchSequenceMoveRight(initiatingLabel);
    boolean leftOrRight = leftAllowed || rightAllowed;
    this.left.setVisible(leftOrRight);
    this.right.setVisible(leftOrRight);
    if (leftOrRight) {
      this.left.setEnabled(leftAllowed);
      this.right.setEnabled(rightAllowed);
    }
  }

  private void initUpDownMenuItems() {
    boolean upAllowed = currentStep.allowsCoCatchMoveUp(initiatingLabel);
    boolean downAllowed = currentStep.allowsCoCatchMoveDown(initiatingLabel);
    boolean upOrDown = upAllowed || downAllowed;
    this.up.setVisible(upOrDown);
    this.down.setVisible(upOrDown);
    if (upOrDown) {
      this.up.setEnabled(upAllowed);
      this.down.setEnabled(downAllowed);
    }
  }

  @Override public void mousePressed(MouseEvent e) {}
  @Override public void mouseReleased(MouseEvent e) {}
  @Override public void mouseEntered(MouseEvent e) {}
  @Override public void mouseExited(MouseEvent e) {}
}
