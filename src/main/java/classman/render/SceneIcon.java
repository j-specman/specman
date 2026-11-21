package classman.render;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * The picture of a scene as a Swing icon of the given size. It paints with {@link SceneGraphicsPainter} on each repaint,
 * as vectors, so it stays sharp at any size and on any display scaling. The picture is the one of the SVG of the scene,
 * stretched to the size of the icon.
 */
public class SceneIcon implements Icon {
    private static final SceneGraphicsPainter PAINTER = new SceneGraphicsPainter();

    private final Scene scene;
    private final SvgFrame frame;
    private final int width;
    private final int height;

    public SceneIcon(Scene scene, int width, int height) {
        this.scene = scene;
        this.frame = SvgFrame.of(scene);
        this.width = width;
        this.height = height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(x, y);
            g2.scale(width / frame.width, height / frame.height);
            g2.translate(-frame.x, -frame.y);
            PAINTER.paint(scene, g2);
        }
        finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() { return width; }

    @Override
    public int getIconHeight() { return height; }
}
