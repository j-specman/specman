package classman.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** The picture of a scene as pixels: the area of its {@link SvgFrame} on white, painted like the editor paints it at
 * the same zoom, so a change mark has the size on the picture which it has on the screen. */
public final class SceneBitmap {

    private SceneBitmap() {
    }

    /** @param zoom how many pixels a unit of the scene covers */
    public static BufferedImage of(Scene scene, double zoom) {
        SvgFrame frame = SvgFrame.of(scene);
        int width = Math.max(1, (int) Math.round(frame.width * zoom));
        int height = Math.max(1, (int) Math.round(frame.height * zoom));
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.scale(zoom, zoom);
            g.translate(-frame.x, -frame.y);
            new SceneGraphicsPainter().paint(scene, g);
        }
        finally {
            g.dispose();
        }
        return image;
    }
}
