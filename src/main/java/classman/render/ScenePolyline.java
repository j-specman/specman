package classman.render;

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
