package classman.render;

/** A closed polygon, for example a note with its folded corner. */
public class ScenePolygon extends AbstractSceneElement {
    public final double[] xs;
    public final double[] ys;
    /** A color like #f2e9c4, or null for no fill. */
    public final String fill;
    /** A color like #000000, or null for no outline. */
    public final String stroke;
    public final double strokeWidth;
    /** See {@link #keepingVisible()}. */
    public final boolean keepsVisible;
    /** True if painters round the corners of the outline instead of pointing them, see {@link #withRoundCorners()}. */
    public final boolean roundCorners;

    public ScenePolygon(double[] xs, double[] ys, String fill, String stroke) {
        this(xs, ys, fill, stroke, DiagramStyle.STROKE_WIDTH);
    }

    public ScenePolygon(double[] xs, double[] ys, String fill, String stroke, double strokeWidth) {
        this(xs, ys, fill, stroke, strokeWidth, false, false);
    }

    private ScenePolygon(double[] xs, double[] ys, String fill, String stroke, double strokeWidth,
                         boolean keepsVisible, boolean roundCorners) {
        this.xs = xs.clone();
        this.ys = ys.clone();
        this.fill = fill;
        this.stroke = stroke;
        this.strokeWidth = strokeWidth;
        this.keepsVisible = keepsVisible;
        this.roundCorners = roundCorners;
    }

    /** @return the same polygon whose outline painters don't make thinner than {@link DiagramStyle#MIN_VISIBLE_STROKE}
     * on the screen, however small the diagram is painted */
    public ScenePolygon keepingVisible() {
        return new ScenePolygon(xs, ys, fill, stroke, strokeWidth, true, roundCorners);
    }

    /** @return the same polygon with round corners. A thick outline has a spike beyond a sharp corner, a round corner
     * reaches only half the width beyond. The SVG writer doesn't know this: it is for the display of the editor. */
    public ScenePolygon withRoundCorners() {
        return new ScenePolygon(xs, ys, fill, stroke, strokeWidth, keepsVisible, true);
    }

    /** @return the same polygon with an outline of another width */
    public ScenePolygon withStrokeWidth(double width) {
        return new ScenePolygon(xs, ys, fill, stroke, width, keepsVisible, roundCorners);
    }

    @Override
    public ScenePolygon translated(double dx, double dy) {
        double[] movedXs = new double[xs.length];
        double[] movedYs = new double[ys.length];
        for (int i = 0; i < xs.length; i++) {
            movedXs[i] = xs[i] + dx;
            movedYs[i] = ys[i] + dy;
        }
        return new ScenePolygon(movedXs, movedYs, fill, stroke, strokeWidth, keepsVisible, roundCorners);
    }
}
