package classman.editor;

import classman.render.SceneRelationLine;

import java.awt.geom.Rectangle2D;
import java.util.function.Supplier;

/** Something which a connection which the user draws can end at: a class, an attribute or a relation. */
abstract class ConnectionTarget {

  /** @return the class, attribute or relation which this target stands for */
  abstract Object element();

  /** @return the area which is framed while the connection would end here, null if the target has none */
  abstract Rectangle2D frame();

  /** @return the line which is drawn thick while the connection would end here, null if the target is no relation */
  SceneRelationLine thickLine() {
    return null;
  }

  /** @return the ID of the target, which it gets from the supplier first if it has none */
  abstract String ensureId(Supplier<String> newId);
}
