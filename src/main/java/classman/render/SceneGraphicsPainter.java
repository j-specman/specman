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
import static classman.render.DiagramStyle.MIN_VISIBLE_HIGHLIGHT;
import static classman.render.DiagramStyle.MIN_VISIBLE_STROKE;
import static classman.render.DiagramStyle.MITER_LIMIT;
import static classman.render.DiagramStyle.TEXT_COLOR;

/**
 * Paints a scene on a Graphics2D, so that the editor shows what the SVG shows. It paints in the coordinates of the scene,
 * in the order of the scene, so later elements lie above earlier ones. Zoom and offset are the business of the caller:
 * it sets them as transform of the Graphics2D. To get the picture of the SVG, translate by minus x and minus y of the
 * {@link SvgFrame} of the scene. The shape of the picture doesn't depend on the display: strokes are not normalized
 * and text uses fractional metrics, so everything scales smoothly.
 */
public class SceneGraphicsPainter {

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
            fillAndStroke(g, rectangleShape(g, rectangle), rectangle.fill, rectangle.stroke, strokeWidthOf(g, rectangle.strokeWidth, rectangle.keepsVisible), false);
        } else if (element instanceof SceneLine line) {
            stroke(g, new Line2D.Double(line.x1, line.y1, line.x2, line.y2), line.stroke, line.strokeWidth, line.dashed, false);
        } else if (element instanceof ScenePolyline polyline) {
            stroke(g, path(polyline.xs, polyline.ys, false), polyline.stroke,
                strokeWidthOf(g, polyline.strokeWidth, polyline.keepsVisible), polyline.dashed, polyline.roundCorners);
        } else if (element instanceof ScenePolygon polygon) {
            fillAndStroke(g, path(polygon.xs, polygon.ys, true), polygon.fill, polygon.stroke,
                strokeWidthOf(g, polygon.strokeWidth, polygon.keepsVisible), polygon.roundCorners);
        } else if (element instanceof SceneText text) {
            paintText(g, text);
        } else if (element instanceof SceneVectorImage vector) {
            vector.image.paint(g, vector.x, vector.y, vector.width, vector.height);
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

    private void fillAndStroke(Graphics2D g, Shape shape, String fill, String stroke, double strokeWidth,
                               boolean roundCorners) {
        if (fill != null) {
            g.setColor(SceneColor.parse(fill));
            g.fill(shape);
        }
        if (stroke != null) {
            stroke(g, shape, stroke, strokeWidth, false, roundCorners);
        }
    }

    private void stroke(Graphics2D g, Shape shape, String color, double width, boolean dashed, boolean roundCorners) {
        g.setColor(SceneColor.parse(color));
        g.setStroke(strokeOf(width, dashed, roundCorners));
        g.draw(shape);
    }

    /** Butt ends and mitered corners, like the default of an SVG stroke, unless the corners are to be round. */
    private Stroke strokeOf(double width, boolean dashed, boolean roundCorners) {
        float[] dashes = dashed ? new float[] {DASH_LENGTH, DASH_GAP} : null;
        int join = roundCorners ? BasicStroke.JOIN_ROUND : BasicStroke.JOIN_MITER;
        return new BasicStroke((float) width, BasicStroke.CAP_BUTT, join, MITER_LIMIT, dashes, 0);
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
        g.setFont(text.font.awtFont(text.bold));
        g.setColor(SceneColor.parse(text.fill == null ? TEXT_COLOR : text.fill));
        g.fill(g.getFont().createGlyphVector(g.getFontRenderContext(), text.text)
            .getOutline((float) text.left(), (float) text.y));
    }
}
