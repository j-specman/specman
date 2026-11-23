package classman.editor;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Supplier;

/** An attribute of a class as the end of a connection. */
class AttributeTarget extends ConnectionTarget {
  final ClassModel_V001 owner;
  final AttributeModel_V001 attribute;
  private final Rectangle2D line;

  AttributeTarget(ClassModel_V001 owner, AttributeModel_V001 attribute, Rectangle2D line) {
    this.owner = owner;
    this.attribute = attribute;
    this.line = line;
  }

  @Override
  Object element() { return attribute; }

  @Override
  Rectangle2D frame() { return line; }

  @Override
  Point2D lineEnd(Point2D mouse) {
    return new Point2D.Double(line.getCenterX(), line.getCenterY());
  }

  @Override
  String ensureId(Supplier<String> newId) {
    if (attribute.id == null) {
      attribute.id = newId.get();
    }
    return attribute.id;
  }
}
