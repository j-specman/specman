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

public class StepButtonBar extends JToolBar {

  StepButtonBar(Specman specman) {
    super(JToolBar.VERTICAL);

    addDragButton(new CreateSimpleStepOpButton(specman), "einfacher-schritt", "Create simple step", specman);
    addDragButton(new CreateWhileStepOpButton(specman), "while-schritt", "Create while step", specman);
    addDragButton(new CreateWhileWhileStepOpButton(specman), "whilewhile-schritt", "Create while-while step", specman);
    addDragButton(new CreateIfElseStepOpButton(specman), "ifelse-schritt", "Create if-else step", specman);
    addDragButton(new CreateIfStepOpButton(specman), "if-schritt", "Create if step", specman);
    addDragButton(new CreateCaseStepOpButton(specman), "case-schritt", "Create case step", specman);
    addDragButton(new CreateSubsequenceStepOpButton(specman), "subsequenz-schritt", "Create subsequence step", specman);
    addDragButton(new CreateBreakStepOpButton(specman), "break-schritt", "Create break step", specman);
    addDragButton(new CreateCatchSequenceADBLOpButton(specman), "catch-schritt", "Create catch block", specman);
    addDragButton(new CreateCaseBranchADBLOpButton(specman), "zweig", "Create case branch", specman);
  }

  private void addDragButton(JButton button, String iconBasename, String tooltip, Specman specman) {
    button.setIcon(IconReader.readImageIcon(iconBasename));
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setToolTipText(tooltip);
    add(button);
    DragMouseAdapter dragAdapter = new DragMouseAdapter(specman, this);
    button.addMouseListener(dragAdapter);
    button.addMouseMotionListener(dragAdapter);
  }

  public DragSource dragSourceFor(JButton button) {
    return ((DragSourceProvider) button).dragSource();
  }

}
