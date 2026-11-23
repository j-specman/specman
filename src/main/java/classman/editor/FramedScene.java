package classman.editor;

import classman.render.AbstractSceneElement;
import classman.render.DiagramStyle;
import classman.render.Scene;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/** A scene with the frame of an added element below it, for the previews of the style dialogs. */
final class FramedScene {

  private FramedScene() {
  }

  /** @param frameAtOrigin the frame, which lies at the origin and is moved to the area of the element
   * @return the scene, grown by what the frame reaches out of it */
  static Scene of(Scene laidOut, AbstractSceneElement frameAtOrigin, Rectangle2D.Double area) {
    List<AbstractSceneElement> elements = new ArrayList<>();
    elements.add(frameAtOrigin.translated(area.x, area.y));
    elements.addAll(laidOut.elements);
    double reach = DiagramStyle.FRAME_STROKE_WIDTH / 2;
    return new Scene(elements, laidOut.x - reach, laidOut.y - reach, laidOut.width + 2 * reach,
      laidOut.height + 2 * reach);
  }
}
