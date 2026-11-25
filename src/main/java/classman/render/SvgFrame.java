package classman.render;

import static classman.render.DiagramStyle.SVG_MARGIN;

/**
 * The area which the picture of a scene shows: the scene plus an empty margin. The SVG of the scene has this area as
 * its view box, and whoever paints the scene on a Graphics2D translates by minus x and minus y to get the same picture.
 */
public class SvgFrame {
    public final double x;
    public final double y;
    public final double width;
    public final double height;

    private SvgFrame(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static SvgFrame of(Scene scene) {
        return new SvgFrame(scene.x - SVG_MARGIN, scene.y - SVG_MARGIN,
            scene.width + 2 * SVG_MARGIN, scene.height + 2 * SVG_MARGIN);
    }
}
