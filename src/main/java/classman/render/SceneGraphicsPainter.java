package classman.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import static classman.render.DiagramStyle.DASH_GAP;
import static classman.render.DiagramStyle.DASH_LENGTH;
import static classman.render.DiagramStyle.FONT_SIZE;
import static classman.render.DiagramStyle.MIN_VISIBLE_HIGHLIGHT;
import static classman.render.DiagramStyle.MIN_VISIBLE_STROKE;
import static classman.render.DiagramStyle.MITER_LIMIT;

/**
 * Paints a scene on a Graphics2D, so that the editor shows what the SVG shows. It paints in the coordinates of the scene,
 * in the order of the scene, so later elements lie above earlier ones. Zoom and offset are the business of the caller:
 * it sets them as transform of the Graphics2D. To get the picture of the SVG, translate by minus x and minus y of the
 * {@link SvgFrame} of the scene. The shape of the picture doesn't depend on the display: strokes are not normalized
 * and text uses fractional metrics, so everything scales smoothly.
 */
public class SceneGraphicsPainter {
    private final Font plainFont = DiagramFonts.plain(FONT_SIZE);
    private final Font boldFont = DiagramFonts.bold(FONT_SIZE);

    /** Leaves the rendering hints of the Graphics2D as they were. */
    public void paint(Scene scene, Graphics2D g) {
        RenderingHints savedHints = g.getRenderingHints();
        Stroke savedStroke = g.getStroke();
        Color savedColor = g.getColor();
        Font savedFont = g.getFont();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        for (AbstractSceneElement element : scene.elements) {
            paintElement(element, g);
        }
        g.setRenderingHints(savedHints);
        g.setStroke(savedStroke);
        g.setColor(savedColor);
        g.setFont(savedFont);
    }

    private void paintElement(AbstractSceneElement element, Graphics2D g) {
        if (element instanceof SceneRectangle rectangle) {
            fillAndStroke(g, rectangleShape(g, rectangle), rectangle.fill, rectangle.stroke, strokeWidthOf(g, rectangle.strokeWidth, rectangle.keepsVisible));
        } else if (element instanceof SceneLine line) {
            stroke(g, new Line2D.Double(line.x1, line.y1, line.x2, line.y2), line.stroke, line.strokeWidth, line.dashed);
        } else if (element instanceof ScenePolyline polyline) {
            stroke(g, path(polyline.xs, polyline.ys, false), polyline.stroke,
                strokeWidthOf(g, polyline.strokeWidth, polyline.keepsVisible), polyline.dashed);
        } else if (element instanceof ScenePolygon polygon) {
            fillAndStroke(g, path(polygon.xs, polygon.ys, true), polygon.fill, polygon.stroke,
                strokeWidthOf(g, polygon.strokeWidth, polygon.keepsVisible));
        } else if (element instanceof SceneText text) {
            paintText(g, text);
        } else {
            throw new IllegalArgumentException("Unknown scene element: " + element.getClass().getName());
        }
    }

    /**
     * The marks of changes must stay recognizable when the diagram is painted small, so their strokes don't get thinner
     * than {@link DiagramStyle#MIN_VISIBLE_STROKE} on the screen. Everything else scales with the diagram.
     */
    private double strokeWidthOf(Graphics2D g, double sceneWidth, boolean keepsVisible) {
        if (!keepsVisible) {
            return sceneWidth;
        }
        return Math.max(sceneWidth, MIN_VISIBLE_STROKE / deviceScale(g));
    }

    /** A filled rectangle which keeps itself visible grows around its center to the minimum size on the screen. */
    private Rectangle2D.Double rectangleShape(Graphics2D g, SceneRectangle rectangle) {
        if (!rectangle.keepsVisible || rectangle.fill == null) {
            return new Rectangle2D.Double(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        }
        double minimumSize = MIN_VISIBLE_HIGHLIGHT / deviceScale(g);
        double width = Math.max(rectangle.width, minimumSize);
        double height = Math.max(rectangle.height, minimumSize);
        return new Rectangle2D.Double(rectangle.x - (width - rectangle.width) / 2,
            rectangle.y - (height - rectangle.height) / 2, width, height);
    }

    /** How many pixels of the screen a unit of the scene covers. */
    private double deviceScale(Graphics2D g) {
        AffineTransform transform = g.getTransform();
        return Math.hypot(transform.getScaleX(), transform.getShearY());
    }

    private void fillAndStroke(Graphics2D g, Shape shape, String fill, String stroke, double strokeWidth) {
        if (fill != null) {
            g.setColor(Color.decode(fill));
            g.fill(shape);
        }
        if (stroke != null) {
            stroke(g, shape, stroke, strokeWidth, false);
        }
    }

    private void stroke(Graphics2D g, Shape shape, String color, double width, boolean dashed) {
        g.setColor(Color.decode(color));
        g.setStroke(strokeOf(width, dashed));
        g.draw(shape);
    }

    /** Butt ends and mitered corners, like the default of an SVG stroke. */
    private Stroke strokeOf(double width, boolean dashed) {
        float[] dashes = dashed ? new float[] {DASH_LENGTH, DASH_GAP} : null;
        return new BasicStroke((float) width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, MITER_LIMIT, dashes, 0);
    }

    private Path2D.Double path(double[] xs, double[] ys, boolean closed) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) {
            path.lineTo(xs[i], ys[i]);
        }
        if (closed) {
            path.closePath();
        }
        return path;
    }

    /**
     * The scene knows where the text starts, so the painter doesn't measure it again. The glyphs are filled as shapes
     * and not drawn as a string: that is how SVG renderers draw text, and it keeps the letters free of the hinting
     * which would change their shape at each zoom level.
     */
    private void paintText(Graphics2D g, SceneText text) {
        g.setFont(text.bold ? boldFont : plainFont);
        g.setColor(text.fill == null ? Color.BLACK : Color.decode(text.fill));
        g.fill(g.getFont().createGlyphVector(g.getFontRenderContext(), text.text)
            .getOutline((float) text.left(), (float) text.y));
    }
}
