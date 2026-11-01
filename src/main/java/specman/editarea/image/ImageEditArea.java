package specman.editarea.image;

import specman.ChangeInfo;
import specman.model.v002.ImageEditAreaModel_V002;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** An edit area showing a bitmap, which is persisted as PNG. */
public class ImageEditArea extends AbstractImageEditArea {
  private final BufferedImage fullSizeImage;

  public ImageEditArea(BufferedImage fullSizeImage, ChangeInfo changeInfo) {
    super(changeInfo, 1);
    this.fullSizeImage = fullSizeImage;
  }

  public ImageEditArea(ImageEditAreaModel_V002 model) {
    super(model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED,
      model.individualScalePercent);
    this.fullSizeImage = readImage(model.imageData);
  }

  private static BufferedImage readImage(byte[] imageData) {
    try {
      return ImageIO.read(new ByteArrayInputStream(imageData));
    }
    catch (IOException iox) {
      throw new RuntimeException(iox);
    }
  }

  @Override
  protected int intrinsicWidth() { return fullSizeImage.getWidth(); }

  @Override
  protected Icon createScaledIcon(float totalScalePercent) {
    return new ImageIcon(fullSizeImage
      .getScaledInstance((int)(fullSizeImage.getWidth() * totalScalePercent),
        (int)(fullSizeImage.getHeight() * totalScalePercent), Image.SCALE_SMOOTH));
  }

  @Override
  public BufferedImage getFullSizeImage() { return fullSizeImage; }

  @Override
  public ImageEditAreaModel_V002 toModel(boolean formatierterText) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ImageIO.write(fullSizeImage, PERSISTED_IMAGETYPE, bytes);
      return new ImageEditAreaModel_V002(bytes.toByteArray(), PERSISTED_IMAGETYPE, individualScalePercent, getChangeInfo());
    }
    catch (IOException iox) {
      throw new RuntimeException(iox);
    }
  }
}
