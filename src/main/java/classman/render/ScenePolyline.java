package classman.render;

import java.awt.geom.Rectangle2D;

/** A line through several points. */
public class ScenePolyline extends AbstractSceneElement {
    public final double[] xs;
    public final double[] ys;
    public final boolean dashed;
    /** A color like #000000. */
    public final String stroke;
    public final double strokeWidth;
    /** See {@link #keepingVisible()}. */
    public final boolean keepsVisible;
    /** True if painters round the corners of the line instead of pointing them, see {@link #withRoundCorners()}. */
    public final boolean roundCorners;

    public ScenePolyline(double[] xs, double[] ys, boolean dashed) {
        this(xs, ys, dashed, DiagramStyle.STROKE, DiagramStyle.STROKE_WIDTH);
    }

    public ScenePolyline(double[] xs, double[] ys, boolean dashed, String stroke, double strokeWidth) {
        this(xs, ys, dashed, stroke, strokeWidth, false, false);
    }

    private ScenePolyline(double[] xs, double[] ys, boolean dashed, String stroke, double strokeWidth,
                          boolean keepsVisible, boolean roundCorners) {
        this.xs = xs.clone();
        this.ys = ys.clone();
        this.dashed = dashed;
        this.stroke = stroke;
        this.strokeWidth = strokeWidth;
        this.keepsVisible = keepsVisible;
        this.roundCorners = roundCorners;
    }

    /** @return the same line which painters don't make thinner than {@link DiagramStyle#MIN_VISIBLE_STROKE} on the
     * screen, however small the diagram is painted */
    public ScenePolyline keepingVisible() {
        return new ScenePolyline(xs, ys, dashed, stroke, strokeWidth, true, roundCorners);
    }

    /** @return the same line with round corners. A thick line with a sharp corner has a spike beyond it, a round
     * corner reaches only half the width beyond. The SVG writer doesn't know this: it is for the display of the
     * editor. */
    public ScenePolyline withRoundCorners() {
        return new ScenePolyline(xs, ys, dashed, stroke, strokeWidth, keepsVisible, true);
    }

    /** @return the same line with another width */
    public ScenePolyline withStrokeWidth(double width) {
        return new ScenePolyline(xs, ys, dashed, stroke, width, keepsVisible, roundCorners);
    }

    /** @return the same line with another course */
    public ScenePolyline withPoints(double[] newXs, double[] newYs) {
        return new ScenePolyline(newXs, newYs, dashed, stroke, strokeWidth, keepsVisible, roundCorners);
    }

    /** The smallest rectangle around all points. */
    public Rectangle2D.Double bounds() {
        double minX = xs[0];
        double maxX = xs[0];
        double minY = ys[0];
        double maxY = ys[0];
        for (int i = 1; i < xs.length; i++) {
            minX = Math.min(minX, xs[i]);
            maxX = Math.max(maxX, xs[i]);
            minY = Math.min(minY, ys[i]);
            maxY = Math.max(maxY, ys[i]);
        }
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public ScenePolyline translated(double dx, double dy) {
        double[] movedXs = new double[xs.length];
        double[] movedYs = new double[ys.length];
        for (int i = 0; i < xs.length; i++) {
            movedXs[i] = xs[i] + dx;
            movedYs[i] = ys[i] + dy;
        }
        return new ScenePolyline(movedXs, movedYs, dashed, stroke, strokeWidth, keepsVisible, roundCorners);
    }
}
