package classman.render;

import java.awt.geom.Rectangle2D;

/** A line through several points. */
public class ScenePolyline extends AbstractSceneElement {
    public final double[] xs;
    public final double[] ys;
    public final boolean dashed;

    public ScenePolyline(double[] xs, double[] ys, boolean dashed) {
        this.xs = xs.clone();
        this.ys = ys.clone();
        this.dashed = dashed;
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
        return new ScenePolyline(movedXs, movedYs, dashed);
    }
}
