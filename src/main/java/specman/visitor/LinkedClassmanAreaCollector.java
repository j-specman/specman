package specman.visitor;

import specman.editarea.image.AbstractImageEditArea;
import specman.editarea.image.classman.LinkedClassmanImageEditArea;

import java.util.ArrayList;
import java.util.List;

/** Collects the Classman diagrams which link to a file, in document order. */
public class LinkedClassmanAreaCollector implements DiagramVisitor {
  private final List<LinkedClassmanImageEditArea> areas = new ArrayList<>();

  @Override
  public void visitImageArea(AbstractImageEditArea imageArea) {
    if (imageArea instanceof LinkedClassmanImageEditArea linked) {
      areas.add(linked);
    }
  }

  public List<LinkedClassmanImageEditArea> areas() {
    return areas;
  }
}
