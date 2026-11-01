package specman.editarea.image;

import specman.ChangeInfo;
import specman.model.v002.SvgImageEditAreaModel_V002;
import specman.pdf.ShapeImage;

import java.nio.charset.StandardCharsets;

/** An edit area showing an imported SVG graphic. The graphic isn't edited in Specman, so the model persists
 * its source as it was imported. */
public class SvgImageEditArea extends AbstractSvgImageEditArea {
  private final String source;
  private final byte[] svg;

  public SvgImageEditArea(String source, ChangeInfo changeInfo) {
    this(source, 1, changeInfo);
  }

  public SvgImageEditArea(SvgImageEditAreaModel_V002 model) {
    this(model.source, model.individualScalePercent,
      model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
  }

  private SvgImageEditArea(String source, float individualScalePercent, ChangeInfo changeInfo) {
    super(changeInfo, individualScalePercent);
    this.source = source;
    this.svg = source.getBytes(StandardCharsets.UTF_8);
    showSvg(svg);
  }

  @Override
  protected ShapeImage createShapeImage() { return vectorShapeImage(svg); }

  @Override
  public SvgImageEditAreaModel_V002 toModel(boolean formatierterText) {
    return new SvgImageEditAreaModel_V002(source, individualScalePercent, getChangeInfo());
  }
}
