package classman.render;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.parser.SVGLoader;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** A vector graphic which the host gives for a stereotype. It is parsed once and then painted at any size, so it stays
 * sharp at every zoom. For an SVG file of the diagram it is written as a PNG in a high resolution instead. */
public final class VectorImage {
    private final SVGDocument document;

    private VectorImage(SVGDocument document) {
        this.document = document;
    }

    /** @param svg the markup of the graphic
     * @throws IllegalArgumentException if it isn't an SVG which can be shown */
    public static VectorImage of(String svg) {
        SVGDocument document = new SVGLoader().load(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
        if (document == null) {
            throw new IllegalArgumentException("Not a valid SVG graphic");
        }
        return new VectorImage(document);
    }

    /** @param path where the SVG file is in the class path, e.g. images/svg/material/priority_high.svg
     * @throws IllegalArgumentException if there is no such file or it isn't an SVG which can be shown */
    public static VectorImage ofResource(String path) {
        try (InputStream in = VectorImage.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("No graphic " + path);
            }
            return of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Paints the graphic into the area, fitted into it as the graphic says itself. */
    public void paint(Graphics2D g, double x, double y, double width, double height) {
        Graphics2D target = (Graphics2D) g.create();
        try {
            target.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            document.render(null, target, new ViewBox((float) x, (float) y, (float) width, (float) height));
        }
        finally {
            target.dispose();
        }
    }

    /** @return the graphic as a transparent PNG of the given size in pixels, as the data URI for an SVG image */
    public String pngDataUri(int pixelWidth, int pixelHeight) {
        BufferedImage image = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            paint(g, 0, 0, pixelWidth, pixelHeight);
        }
        finally {
            g.dispose();
        }
        try {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", png);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
