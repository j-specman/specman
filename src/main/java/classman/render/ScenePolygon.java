package classman.render;

/** A closed polygon, for example a note with its folded corner. */
public class ScenePolygon extends AbstractSceneElement {
    public final double[] xs;
    public final double[] ys;
    /** A color like #6fcdf2, or null for no fill. */
    public final String fill;
    /** A color like #000000, or null for no outline. */
    public final String stroke;

    public ScenePolygon(double[] xs, double[] ys, String fill, String stroke) {
        this.xs = xs.clone();
        this.ys = ys.clone();
        this.fill = fill;
        this.stroke = stroke;
    }

    @Override
    public ScenePolygon translated(double dx, double dy) {
        double[] movedXs = new double[xs.length];
        double[] movedYs = new double[ys.length];
        for (int i = 0; i < xs.length; i++) {
            movedXs[i] = xs[i] + dx;
            movedYs[i] = ys[i] + dy;
        }
        return new ScenePolygon(movedXs, movedYs, fill, stroke);
    }
}
