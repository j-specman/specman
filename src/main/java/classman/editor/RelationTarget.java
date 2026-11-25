package classman.editor;

import classman.render.SceneRelationLine;

import java.awt.geom.Rectangle2D;
import java.util.function.Supplier;

/** A relation as the end of a connection: it is marked by its line, which is drawn thick. */
class RelationTarget extends ConnectionTarget {
  private final SceneRelationLine relationLine;

  RelationTarget(SceneRelationLine relationLine) {
    this.relationLine = relationLine;
  }

  @Override
  Object element() { return relationLine.relation; }

  @Override
  Rectangle2D frame() { return null; }

  @Override
  SceneRelationLine thickLine() { return relationLine; }

  @Override
  String ensureId(Supplier<String> newId) {
    if (relationLine.relation.id == null) {
      relationLine.relation.id = newId.get();
    }
    return relationLine.relation.id;
  }
}
