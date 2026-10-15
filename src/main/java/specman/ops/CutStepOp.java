package specman.ops;

import specman.EditException;
import specman.editarea.InteractiveStepFragment;
import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;

import javax.swing.JOptionPane;
import java.util.List;

import static specman.i18n.Messages.i18n;

public class CutStepOp extends AbstractADBLSpecmanOp {

  private final AbstractSchrittView step;
  private final InteractiveStepFragment initiatingFragment;

  public CutStepOp(SpecmanOpContext context, AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    super(context);
    this.step = step;
    this.initiatingFragment = initiatingFragment;
  }

  @Override
  void execute() throws EditException {
    if (userConfirmedCutDespiteExternalCatches()) {
      new CopyStepOp(step).run();
      deleteStepADBL(step, initiatingFragment);
    }
  }

  private boolean userConfirmedCutDespiteExternalCatches() {
    List<BreakSchrittView> externalBreaks = step.queryExternallyLinkedBreakSteps();
    if (externalBreaks.isEmpty()) {
      return true;
    }
    int count = externalBreaks.size();
    int result = showConfirmDialog(
        i18n("msg.cut.external.catches.message", count),
        i18n("msg.cut.external.catches.title"),
        JOptionPane.OK_CANCEL_OPTION);
    return result == JOptionPane.OK_OPTION;
  }

}
