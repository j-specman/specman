package classman.editor;

import classman.model.v001.ClassModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Supplier;

/** A class as the end of a connection. */
class ClassTarget extends ConnectionTarget {
  final ClassModel_V001 box;
  private final Rectangle2D area;

  ClassTarget(ClassModel_V001 box, Rectangle2D area) {
    this.box = box;
    this.area = area;
  }

  @Override
  Object element() { return box; }

  @Override
  Rectangle2D frame() { return area; }

  @Override
  Point2D lineEnd(Point2D mouse) {
    return new Point2D.Double(area.getCenterX(), area.getCenterY());
  }

  @Override
  String ensureId(Supplier<String> newId) {
    if (box.id == null) {
      box.id = newId.get();
    }
    return box.id;
  }
}
