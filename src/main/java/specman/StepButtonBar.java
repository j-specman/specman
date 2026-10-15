package specman;

import specman.draganddrop.DragMouseAdapter;
import specman.draganddrop.DragSource;
import specman.graphics.IconReader;
import specman.ops.buttons.CreateBreakStepOpButton;
import specman.ops.buttons.CreateCaseBranchADBLOpButton;
import specman.ops.buttons.CreateCaseStepOpButton;
import specman.ops.buttons.CreateCatchSequenceADBLOpButton;
import specman.ops.buttons.CreateIfElseStepOpButton;
import specman.ops.buttons.CreateIfStepOpButton;
import specman.ops.buttons.CreateSimpleStepOpButton;
import specman.ops.buttons.CreateSubsequenceStepOpButton;
import specman.ops.buttons.CreateWhileStepOpButton;
import specman.ops.buttons.CreateWhileWhileStepOpButton;
import specman.ops.buttons.DragSourceProvider;

import javax.swing.*;
import java.awt.*;

import static specman.i18n.Messages.i18n;

public class StepButtonBar extends JToolBar {

  StepButtonBar(Specman specman) {
    super(JToolBar.VERTICAL);

    addDragButton(new CreateSimpleStepOpButton(specman), "einfacher-schritt", "palette.create.simple", specman);
    addDragButton(new CreateWhileStepOpButton(specman), "while-schritt", "palette.create.while", specman);
    addDragButton(new CreateWhileWhileStepOpButton(specman), "whilewhile-schritt", "palette.create.while.while", specman);
    addDragButton(new CreateIfElseStepOpButton(specman), "ifelse-schritt", "palette.create.if.else", specman);
    addDragButton(new CreateIfStepOpButton(specman), "if-schritt", "palette.create.if", specman);
    addDragButton(new CreateCaseStepOpButton(specman), "case-schritt", "palette.create.case", specman);
    addDragButton(new CreateSubsequenceStepOpButton(specman), "subsequenz-schritt", "palette.create.subsequence", specman);
    addDragButton(new CreateBreakStepOpButton(specman), "break-schritt", "palette.create.break", specman);
    addDragButton(new CreateCatchSequenceADBLOpButton(specman), "catch-schritt", "palette.create.catch", specman);
    addDragButton(new CreateCaseBranchADBLOpButton(specman), "zweig", "palette.create.case.branch", specman);
  }

  private void addDragButton(JButton button, String iconBasename, String tooltipKey, Specman specman) {
    button.setIcon(IconReader.readImageIcon(iconBasename));
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setToolTipText(i18n(tooltipKey));
    add(button);
    DragMouseAdapter dragAdapter = new DragMouseAdapter(specman, this);
    button.addMouseListener(dragAdapter);
    button.addMouseMotionListener(dragAdapter);
  }

  public DragSource dragSourceFor(JButton button) {
    return ((DragSourceProvider) button).dragSource();
  }

}
