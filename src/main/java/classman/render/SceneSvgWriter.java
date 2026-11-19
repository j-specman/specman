package classman.render;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static classman.render.DiagramStyle.SVG_FONT_FAMILIES;
import static classman.render.DiagramStyle.FONT_SIZE;
import static classman.render.DiagramStyle.STROKE_WIDTH;
import static classman.render.DiagramStyle.SVG_MARGIN;

/**
 * Writes a scene as SVG. It uses only rect, line, polyline, polygon and text, in the order of the scene.
 * The numbers are written with up to two decimals.
 */
public class SceneSvgWriter {
    private static final String DASHES = "5,4";

    public String write(Scene scene) {
        double width = scene.width + 2 * SVG_MARGIN;
        double height = scene.height + 2 * SVG_MARGIN;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(number(width))
            .append("\" height=\"").append(number(height))
            .append("\" viewBox=\"").append(number(scene.x - SVG_MARGIN)).append(' ').append(number(scene.y - SVG_MARGIN))
            .append(' ').append(number(width)).append(' ').append(number(height)).append("\">\n");
        for (AbstractSceneElement element : scene.elements) {
            svg.append("  ").append(elementSvg(element)).append('\n');
        }
        return svg.append("</svg>\n").toString();
    }

    private String elementSvg(AbstractSceneElement element) {
        if (element instanceof SceneRectangle rectangle) {
            return rectangle(rectangle);
        }
        if (element instanceof SceneLine line) {
            return line(line);
        }
        if (element instanceof ScenePolyline polyline) {
            return polyline(polyline);
        }
        if (element instanceof ScenePolygon polygon) {
            return polygon(polygon);
        }
        if (element instanceof SceneText text) {
            return text(text);
        }
        throw new IllegalArgumentException("Unknown scene element: " + element.getClass().getName());
    }

    private String rectangle(SceneRectangle rectangle) {
        return "<rect x=\"" + number(rectangle.x) + "\" y=\"" + number(rectangle.y)
            + "\" width=\"" + number(rectangle.width) + "\" height=\"" + number(rectangle.height)
            + "\" fill=\"" + color(rectangle.fill) + "\" stroke=\"" + color(rectangle.stroke) + "\""
            + strokeWidth(rectangle.strokeWidth) + "/>";
    }

    private String line(SceneLine line) {
        return "<line x1=\"" + number(line.x1) + "\" y1=\"" + number(line.y1)
            + "\" x2=\"" + number(line.x2) + "\" y2=\"" + number(line.y2)
            + "\" stroke=\"" + line.stroke + "\"" + strokeWidth(line.strokeWidth) + dashes(line.dashed) + "/>";
    }

    private String polyline(ScenePolyline polyline) {
        return "<polyline points=\"" + points(polyline.xs, polyline.ys) + "\" fill=\"none\" stroke=\"" + polyline.stroke + "\""
            + strokeWidth(polyline.strokeWidth) + dashes(polyline.dashed) + "/>";
    }

    private String polygon(ScenePolygon polygon) {
        return "<polygon points=\"" + points(polygon.xs, polygon.ys) + "\" fill=\"" + color(polygon.fill)
            + "\" stroke=\"" + color(polygon.stroke) + "\"" + strokeWidth(polygon.strokeWidth) + "/>";
    }

    private String text(SceneText text) {
        return "<text x=\"" + number(text.x) + "\" y=\"" + number(text.y)
            + "\" text-anchor=\"" + anchor(text.anchor) + "\" font-family=\"" + SVG_FONT_FAMILIES
            + "\" font-size=\"" + FONT_SIZE + "\"" + (text.bold ? " font-weight=\"bold\"" : "")
            + (text.fill == null ? "" : " fill=\"" + text.fill + "\"") + ">"
            + escaped(text.text) + "</text>";
    }

    /** Only a stroke which isn't the usual one says its width. */
    private String strokeWidth(double width) {
        return width == STROKE_WIDTH ? "" : " stroke-width=\"" + number(width) + "\"";
    }

    private String anchor(TextAnchor anchor) {
        switch (anchor) {
            case MIDDLE:
                return "middle";
            case END:
                return "end";
            default:
                return "start";
        }
    }

    private String points(double[] xs, double[] ys) {
        StringBuilder points = new StringBuilder();
        for (int i = 0; i < xs.length; i++) {
            if (i > 0) {
                points.append(' ');
            }
            points.append(number(xs[i])).append(',').append(number(ys[i]));
        }
        return points.toString();
    }

    private String dashes(boolean dashed) {
        return dashed ? " stroke-dasharray=\"" + DASHES + "\"" : "";
    }

    private String color(String color) {
        return color == null ? "none" : color;
    }

    private String number(double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String escaped(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
