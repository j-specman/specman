package specman.visitor;

import specman.view.AbstractSchrittView;
import specman.view.CatchBereich;

/**
 * Folds or unfolds every foldable step and catch area regardless of change marks. Each node decides for itself
 * whether it is foldable, see {@link AbstractSchrittView#alleKlappen(boolean)}. The visitor only reports
 * the nodes to them.
 */
public class AllStepsFolder implements DiagramVisitor {

  private final boolean fold;

  public AllStepsFolder(boolean fold) {
    this.fold = fold;
  }

  @Override
  public void visitStep(AbstractSchrittView step) {
    step.alleKlappen(fold);
  }

  @Override
  public void visitCatchArea(CatchBereich catchArea) {
    catchArea.alleKlappen(fold);
  }
}
