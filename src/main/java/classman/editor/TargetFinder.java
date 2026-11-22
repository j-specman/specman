package classman.editor;

import java.awt.geom.Point2D;

/** Finds what a connection which is drawn would end at, if the mouse is at a point. */
interface TargetFinder {

  /** @param tolerance how far the point may be from a line or a class and still be taken for it
   * @return the target at the point, null if there is none */
  ConnectionTarget closestTo(Point2D point, double tolerance);
}
