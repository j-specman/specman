package specman.visitor;

import specman.editarea.AbstractListItemEditArea;
import specman.editarea.EditContainer;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.view.AbstractSchrittView;
import specman.view.CatchBereich;
import specman.view.SchrittSequenzView;

import java.util.function.Consumer;

/**
 * Adapts a part of the diagram to a new zoom factor. Each node scales only itself in its {@code skalieren}
 * method, descending into the diagram is the traversal's job.
 * <p>
 * Scaling takes two traversals, because the layout of a step depends on the contents being scaled already:
 * First the {@link ContentScaler} sets the fonts and the step number labels. Then the {@link LayoutScaler}
 * adapts everything which has to be computed from them: step bounds, borders, form layouts. One traversal
 * visiting each node's content and layout together would have to visit the step before its edit container.
 */
public class DiagramScaler {

  /**
   * @param traversal starts a traversal with the given visitor at the part of the diagram to scale, e.g.
   *                  {@code step::traverse}
   * @param percentNew the new zoom factor
   * @param percentCurrent the zoom factor the part of the diagram is scaled to currently, 0 for a part which
   *                       has just been created
   */
  public static void scale(Consumer<DiagramVisitor> traversal, int percentNew, int percentCurrent) {
    traversal.accept(new ContentScaler(percentNew, percentCurrent));
    traversal.accept(new LayoutScaler(percentNew, percentCurrent));
  }

  private static class ContentScaler implements DiagramVisitor {
    private final int percentNew;
    private final int percentCurrent;

    ContentScaler(int percentNew, int percentCurrent) {
      this.percentNew = percentNew;
      this.percentCurrent = percentCurrent;
    }

    @Override
    public void visitEditContainer(EditContainer editContainer) {
      editContainer.skalieren(percentNew, percentCurrent);
    }

    @Override
    public void visitTextArea(TextEditArea textArea) {
      textArea.skalieren(percentNew, percentCurrent);
    }
  }

  private static class LayoutScaler implements DiagramVisitor {
    private final int percentNew;
    private final int percentCurrent;

    LayoutScaler(int percentNew, int percentCurrent) {
      this.percentNew = percentNew;
      this.percentCurrent = percentCurrent;
    }

    @Override
    public void visitStep(AbstractSchrittView step) {
      step.skalieren(percentNew, percentCurrent);
    }

    @Override
    public void visitSequence(SchrittSequenzView sequence) {
      sequence.skalieren(percentNew, percentCurrent);
    }

    @Override
    public void visitCatchArea(CatchBereich catchArea) {
      catchArea.skalieren(percentNew, percentCurrent);
    }

    @Override
    public void visitTable(TableEditArea table) {
      table.skalieren(percentNew, percentCurrent);
    }

    @Override
    public void visitListItem(AbstractListItemEditArea listItem) {
      listItem.skalieren(percentNew, percentCurrent);
    }
  }
}
