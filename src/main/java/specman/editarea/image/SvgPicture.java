package specman.editarea.image;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.Icon;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/** A picture drawn from an SVG graphic. */
public class SvgPicture implements Picture {
  private final byte[] svg;
  private final FlatSVGIcon icon;

  /** @throws SvgNotDisplayableException if the bytes aren't an SVG which can be shown */
  public SvgPicture(byte[] svg) {
    this.svg = svg;
    this.icon = toIcon(svg);
  }

  private static FlatSVGIcon toIcon(byte[] svg) {
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

  @Override
  public int width() { return icon.getIconWidth(); }

  @Override
  public int height() { return icon.getIconHeight(); }

  @Override
  public Icon scaledIcon(float scale) {
    return icon.derive(Math.round(width() * scale), Math.round(height() * scale));
  }

  @Override
  public byte[] svg() { return svg; }
}
