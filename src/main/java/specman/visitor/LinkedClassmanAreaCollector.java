package specman.visitor;

import specman.editarea.image.AbstractImageEditArea;
import specman.editarea.image.classman.LinkedClassmanImageEditArea;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Collects the Classman diagrams which link to a file, in document order. */
public class LinkedClassmanAreaCollector implements DiagramVisitor {
  private final List<LinkedClassmanImageEditArea> areas = new ArrayList<>();

  /** @param traversal starts a traversal with the given visitor, e.g. {@code step::traverse}
   * @return the linked diagrams the traversal comes across */
  public static List<LinkedClassmanImageEditArea> areasOf(Consumer<DiagramVisitor> traversal) {
    LinkedClassmanAreaCollector collector = new LinkedClassmanAreaCollector();
    traversal.accept(collector);
    return collector.areas;
  }

  @Override
  public void visitImageArea(AbstractImageEditArea imageArea) {
    if (imageArea instanceof LinkedClassmanImageEditArea linked) {
      areas.add(linked);
    }
  }
}
