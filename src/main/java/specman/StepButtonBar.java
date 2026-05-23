package specman;

import specman.draganddrop.DragMouseAdapter;
import specman.graphics.IconReader;

import javax.swing.*;
import java.awt.*;

public class StepButtonBar extends JToolBar {

  private final CreateSimpleStepOpButton createSimpleStep;
  private final CreateWhileStepOpButton createWhileStep;
  private final CreateWhileWhileStepOpButton createWhileWhileStep;
  private final CreateIfElseStepOpButton createIfElseStep;
  private final CreateIfStepOpButton createIfStep;
  private final CreateCaseStepOpButton createCaseStep;
  private final CreateSubsequenceStepOpButton createSubsequenceStep;
  private final CreateBreakStepOpButton createBreakStep;
  private final CreateCatchStepADBLOpButton createCatchStep;
  private final CreateCaseBranchADBLOpButton createCaseBranch;

  StepButtonBar(Specman specman) {
    super(JToolBar.VERTICAL);

    createSimpleStep = new CreateSimpleStepOpButton(specman);
    createWhileStep = new CreateWhileStepOpButton(specman);
    createWhileWhileStep = new CreateWhileWhileStepOpButton(specman);
    createIfElseStep = new CreateIfElseStepOpButton(specman);
    createIfStep = new CreateIfStepOpButton(specman);
    createCaseStep = new CreateCaseStepOpButton(specman);
    createSubsequenceStep = new CreateSubsequenceStepOpButton(specman);
    createBreakStep = new CreateBreakStepOpButton(specman);
    createCatchStep = new CreateCatchStepADBLOpButton(specman);
    createCaseBranch = new CreateCaseBranchADBLOpButton(specman);

    addButton(createSimpleStep, "einfacher-schritt", "Create simple step");
    addButton(createWhileStep, "while-schritt", "Create while step");
    addButton(createWhileWhileStep, "whilewhile-schritt", "Create while-while step");
    addButton(createIfElseStep, "ifelse-schritt", "Create if-else step");
    addButton(createIfStep, "if-schritt", "Create if step");
    addButton(createCaseStep, "case-schritt", "Create case step");
    addButton(createSubsequenceStep, "subsequenz-schritt", "Create subsequence step");
    addButton(createBreakStep, "break-schritt", "Create break step");
    addButton(createCatchStep, "catch-schritt", "Create catch block");
    addButton(createCaseBranch, "zweig", "Create case branch");

    DragMouseAdapter dragAdapter = new DragMouseAdapter(specman, this);
    wireUpDragAdapter(createSimpleStep, dragAdapter);
    wireUpDragAdapter(createWhileStep, dragAdapter);
    wireUpDragAdapter(createWhileWhileStep, dragAdapter);
    wireUpDragAdapter(createIfElseStep, dragAdapter);
    wireUpDragAdapter(createIfStep, dragAdapter);
    wireUpDragAdapter(createCaseStep, dragAdapter);
    wireUpDragAdapter(createSubsequenceStep, dragAdapter);
    wireUpDragAdapter(createBreakStep, dragAdapter);
    wireUpDragAdapter(createCatchStep, dragAdapter);
    wireUpDragAdapter(createCaseBranch, dragAdapter);
  }

  private void addButton(AbstractButton button, String iconBasename, String tooltip) {
    button.setIcon(IconReader.readImageIcon(iconBasename));
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setToolTipText(tooltip);
    add(button);
  }

  private void wireUpDragAdapter(JButton button, DragMouseAdapter adapter) {
    button.addMouseListener(adapter);
    button.addMouseMotionListener(adapter);
  }

  public JButton getCreateSimpleStep() { return createSimpleStep; }
  public JButton getCreateWhileStep() { return createWhileStep; }
  public JButton getCreateWhileWhileStep() { return createWhileWhileStep; }
  public JButton getCreateIfElseStep() { return createIfElseStep; }
  public JButton getCreateIfStep() { return createIfStep; }
  public JButton getCreateCaseStep() { return createCaseStep; }
  public JButton getCreateSubsequenceStep() { return createSubsequenceStep; }
  public JButton getCreateBreakStep() { return createBreakStep; }
  public JButton getCreateCatchStep() { return createCatchStep; }
  public JButton getCreateCaseBranch() { return createCaseBranch; }

}
