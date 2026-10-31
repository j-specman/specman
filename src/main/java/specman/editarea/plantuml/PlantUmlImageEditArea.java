package specman.editarea.plantuml;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import specman.ChangeInfo;
import specman.editarea.AbstractImageEditArea;
import specman.model.v002.ImageEditAreaModel_V002;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** An edit area showing a PlantUML diagram, rendered as SVG so it stays sharp at any zoom level.
 * Proof of concept: the model still persists only the rendered bitmap, not the PlantUML source. */
public class PlantUmlImageEditArea extends AbstractImageEditArea {
  private final String source;
  private final FlatSVGIcon diagram;

  PlantUmlImageEditArea(String source, ChangeInfo changeInfo) {
    super(changeInfo, 1);
    this.source = source;
    this.diagram = loadDiagram(new PlantUmlRenderer().renderSvg(source));
  }

  public String getSource() { return source; }

  private static FlatSVGIcon loadDiagram(byte[] svg) {
    try {
      return new FlatSVGIcon(new ByteArrayInputStream(svg));
    }
    catch (IOException iox) {
      throw new UncheckedIOException(iox);
    }
  }

  @Override
  protected int intrinsicWidth() { return diagram.getIconWidth(); }

  @Override
  protected Icon createScaledIcon(float totalScalePercent) {
    return diagram.derive(
      Math.round(diagram.getIconWidth() * totalScalePercent),
      Math.round(diagram.getIconHeight() * totalScalePercent));
  }

  @Override
  public BufferedImage getFullSizeImage() {
    BufferedImage bitmap = new BufferedImage(diagram.getIconWidth(), diagram.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = bitmap.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      diagram.paintIcon(new JLabel(), g, 0, 0);
    }
    finally {
      g.dispose();
    }
    return bitmap;
  }

  @Override
  public ImageEditAreaModel_V002 toModel(boolean formatierterText) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ImageIO.write(getFullSizeImage(), PERSISTED_IMAGETYPE, bytes);
      return new ImageEditAreaModel_V002(bytes.toByteArray(), PERSISTED_IMAGETYPE, individualScalePercent, getChangeInfo());
    }
    catch (IOException iox) {
      throw new UncheckedIOException(iox);
    }
  }
}
