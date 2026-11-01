package specman.editarea.image;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import specman.ChangeInfo;
import specman.pdf.ShapeImage;
import specman.pdf.ShapeSvgImage;

import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/** An edit area showing an SVG graphic, which stays sharp at any zoom level and is a vector graphic in the PDF.
 * Subclasses say where the SVG comes from, see {@link #showSvg(byte[])} and {@link #createShapeImage()}. */
public abstract class AbstractSvgImageEditArea extends AbstractImageEditArea {
  private FlatSVGIcon svgIcon;

  protected AbstractSvgImageEditArea(ChangeInfo changeInfo, float individualScalePercent) {
    super(changeInfo, individualScalePercent);
  }

  /** @throws SvgNotDisplayableException if the bytes aren't an SVG which can be shown */
  public static FlatSVGIcon toIcon(byte[] svg) {
    try (SvgLoadProblemCatcher loadProblem = new SvgLoadProblemCatcher()) {
      FlatSVGIcon icon = new FlatSVGIcon(new ByteArrayInputStream(svg));
      if (!icon.hasFound()) {
        throw new SvgNotDisplayableException(loadProblem.problem());
      }
      return icon;
    }
    catch (IOException e) {
      throw new SvgNotDisplayableException(e.getMessage());
    }
  }

  /** Subclasses call this to replace the SVG. It is not shown before {@link #imageChanged()}. */
  protected void showSvg(byte[] svg) {
    svgIcon = toIcon(svg);
  }

  /** @return how the PDF export gets the given SVG, which is the one currently shown */
  protected ShapeImage vectorShapeImage(byte[] svg) {
    return new ShapeSvgImage(this, svg, svgIcon.getIconWidth(), svgIcon.getIconHeight());
  }

  @Override
  protected abstract ShapeImage createShapeImage();

  @Override
  protected int intrinsicWidth() { return svgIcon.getIconWidth(); }

  @Override
  protected Icon createScaledIcon(float totalScalePercent) {
    return svgIcon.derive(
      Math.round(svgIcon.getIconWidth() * totalScalePercent),
      Math.round(svgIcon.getIconHeight() * totalScalePercent));
  }

  @Override
  public BufferedImage getFullSizeImage() {
    BufferedImage bitmap = new BufferedImage(svgIcon.getIconWidth(), svgIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = bitmap.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      svgIcon.paintIcon(new JLabel(), g, 0, 0);
    }
    finally {
      g.dispose();
    }
    return bitmap;
  }
}
