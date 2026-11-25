package classman.render;

import classman.model.v001.FontFamily_V001;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static classman.render.DiagramStyle.DASH_GAP;
import static classman.render.DiagramStyle.DASH_LENGTH;
import static classman.render.DiagramStyle.STROKE_WIDTH;

/**
 * Writes a scene as SVG. It uses only rect, line, polyline, polygon, text and image, in the order of the scene.
 * The numbers are written with up to two decimals. A vector graphic is written as an image with a PNG in a high
 * resolution: every program which reads the SVG, among them the PDF export, can show that.
 */
public class SceneSvgWriter {
    /** How many pixels of the PNG of a graphic cover one unit of the diagram. */
    private static final int IMAGE_OVERSAMPLING = 4;

    public String write(Scene scene) {
        SvgFrame frame = SvgFrame.of(scene);
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(number(frame.width))
            .append("\" height=\"").append(number(frame.height))
            .append("\" viewBox=\"").append(number(frame.x)).append(' ').append(number(frame.y))
            .append(' ').append(number(frame.width)).append(' ').append(number(frame.height)).append("\">\n");
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
        if (element instanceof SceneVectorImage vector) {
            return vectorImage(vector);
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

    private String vectorImage(SceneVectorImage vector) {
        int pixelWidth = (int) Math.ceil(vector.width * IMAGE_OVERSAMPLING);
        int pixelHeight = (int) Math.ceil(vector.height * IMAGE_OVERSAMPLING);
        return "<image x=\"" + number(vector.x) + "\" y=\"" + number(vector.y)
            + "\" width=\"" + number(vector.width) + "\" height=\"" + number(vector.height)
            + "\" href=\"" + vector.image.pngDataUri(pixelWidth, pixelHeight) + "\"/>";
    }

    /** The text is anchored at its start, at the x where the layout puts its left end, whatever its anchor is. */
    private String text(SceneText text) {
        return "<text x=\"" + number(text.left()) + "\" y=\"" + number(text.y)
            + "\" font-family=\"" + fontFamilies(text.font.family)
            + "\" font-size=\"" + text.font.size + "\"" + (text.bold ? " font-weight=\"bold\"" : "")
            + (text.fill == null ? "" : " fill=\"" + text.fill + "\"") + ">"
            + escaped(text.text) + "</text>";
    }

    /** The shipped font, then what a viewer without it falls back to. */
    private String fontFamilies(FontFamily_V001 family) {
        switch (family) {
            case SITKA:
                return family.sourceName + ", serif";
            case COURIER:
                return family.sourceName + ", monospace";
            default:
                return family.sourceName + ", Arial, sans-serif";
        }
    }

    /** Only a stroke which isn't the usual one says its width. */
    private String strokeWidth(double width) {
        return width == STROKE_WIDTH ? "" : " stroke-width=\"" + number(width) + "\"";
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
        return dashed ? " stroke-dasharray=\"" + DASH_LENGTH + "," + DASH_GAP + "\"" : "";
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
