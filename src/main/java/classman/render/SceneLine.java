package classman.render;

public class SceneLine extends AbstractSceneElement {
    public final double x1;
    public final double y1;
    public final double x2;
    public final double y2;
    public final boolean dashed;

    public SceneLine(double x1, double y1, double x2, double y2, boolean dashed) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.dashed = dashed;
    }

    @Override
    public SceneLine translated(double dx, double dy) {
        return new SceneLine(x1 + dx, y1 + dy, x2 + dx, y2 + dy, dashed);
    }
}
