package specman.metatag;

import javax.swing.JComponent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Paints a component as if it hangs from the top edge of its container: sharp top corners,
 * antialiased rounded bottom corners. Shared by the meta widgets in the EditContainer header
 * strip (step number label, fold/collapse button, future per-step meta widgets) so they all get
 * the same look from one place.
 * <p>
 * Graphics2D.setClip(Shape) always produces a hard-edged (aliased) clip, regardless of the
 * RenderingHints.KEY_ANTIALIASING hint - that hint only affects fill/draw/text, not clipping.
 * True antialiased edges require rendering the component into an offscreen buffer and
 * compositing it against a separately, antialiased-filled mask shape via AlphaComposite.DstIn.
 */
public final class HangingTabRenderer {

  private HangingTabRenderer() {}

  /** Call from a component's own paint(Graphics) override, passing its own super::paint as
   * superPaint so this stays usable for any JComponent without a shared base class. */
  public static void paint(JComponent component, Graphics g, int arc, Consumer<Graphics> superPaint) {
    int width = component.getWidth();
    int height = component.getHeight();
    if (width <= 0 || height <= 0) {
      superPaint.accept(g);
      return;
    }

    BufferedImage content = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D contentGraphics = content.createGraphics();
    contentGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    superPaint.accept(contentGraphics);
    contentGraphics.dispose();

    BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D maskGraphics = mask.createGraphics();
    maskGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    maskGraphics.setColor(Color.WHITE);
    maskGraphics.fill(bottomRoundedShape(width, height, arc));
    maskGraphics.dispose();

    Graphics2D compositeGraphics = content.createGraphics();
    compositeGraphics.setComposite(AlphaComposite.DstIn);
    compositeGraphics.drawImage(mask, 0, 0, null);
    compositeGraphics.dispose();

    Graphics2D g2 = (Graphics2D) g.create();
    g2.drawImage(content, 0, 0, null);
    g2.dispose();
  }

  private static Shape bottomRoundedShape(float x, float y, float width, float height, float arc) {
    Area area = new Area(new RoundRectangle2D.Float(x, y, width, height, arc, arc));
    area.add(new Area(new Rectangle2D.Float(x, y, width, height / 2))); // square the top corners off again
    return area;
  }

  private static Shape bottomRoundedShape(int width, int height, int arc) {
    return bottomRoundedShape(0, 0, width, height, arc);
  }

  /** Open path tracing the hanging-tab shape's bottom/left/right edges only - starting at the
   * top-left corner, down, around the rounded bottom, and back up to the top-right corner,
   * deliberately never closing back across the top. Meant to be stroked (not filled): an open
   * path has free ends at the top, so there is no join/cap there to clean up, unlike a closed
   * shape's stroke which would need the top edge's contribution cut out after the fact.
   * <p>
   * The corner curves use the same quadratic-with-sharp-corner-as-control-point construction as
   * {@link RoundRectangle2D}, with start/end points offset from each corner by arc/2 - the "arc"
   * parameter there is the corner's full diameter, not its radius - so this lines up pixel-for-
   * pixel with {@link #bottomRoundedShape}'s own rounding instead of drawing its own, differently
   * sized curve. */
  public static Shape outline(float width, float height, float arc) {
    float r = arc / 2;
    Path2D.Float path = new Path2D.Float();
    path.moveTo(0, 0);
    path.lineTo(0, height - r);
    path.quadTo(0, height, r, height);
    path.lineTo(width - r, height);
    path.quadTo(width, height, width, height - r);
    path.lineTo(width, 0);
    return path;
  }
}
