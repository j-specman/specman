package specman.visitor;

import specman.editarea.AbstractListItemEditArea;
import specman.editarea.EditContainer;
import specman.editarea.ImageEditArea;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.view.AbstractSchrittView;
import specman.view.CatchBereich;

import java.util.function.Consumer;

/**
 * Completes the views of a part of the diagram after it has been built from a model, i.e. after loading
 * a diagram or pasting steps. Each node initializes only itself in its {@code viewsNachinitialisieren} method,
 * descending into the diagram is the traversal's job.
 * <p>
 * Initializing takes two traversals: First the {@link StyleInitializer} applies the styles of deleted, source and
 * target steps and areas. Then the {@link StepnumberLinkRegistrar} registers the step number links of the steps'
 * texts, which includes marking the broken ones in the styled texts.
 */
public class DiagramPostInitializer {

  /** @param traversal starts a traversal with the given visitor at the part of the diagram to initialize,
   *                   e.g. {@code step::traverse} */
  public static void initialize(Consumer<DiagramVisitor> traversal) {
    traversal.accept(new StyleInitializer());
    traversal.accept(new StepnumberLinkRegistrar());
  }

  private static class StyleInitializer implements DiagramVisitor {
    @Override
    public void visitStep(AbstractSchrittView step) {
      step.viewsNachinitialisieren();
    }

    @Override
    public void visitCatchArea(CatchBereich catchArea) {
      catchArea.viewsNachinitialisieren();
    }

    @Override
    public void visitEditContainer(EditContainer editContainer) {
      editContainer.viewsNachinitialisieren();
    }

    @Override
    public void visitTextArea(TextEditArea textArea) {
      textArea.viewsNachinitialisieren();
    }

    @Override
    public void visitImageArea(ImageEditArea imageArea) {
      imageArea.viewsNachinitialisieren();
    }

    @Override
    public void visitTable(TableEditArea table) {
      table.viewsNachinitialisieren();
    }

    @Override
    public void visitListItem(AbstractListItemEditArea listItem) {
      listItem.viewsNachinitialisieren();
    }
  }

  private static class StepnumberLinkRegistrar implements DiagramVisitor {
    @Override
    public void visitStep(AbstractSchrittView step) {
      step.registerAllExistingStepnumbers();
    }
  }
}
