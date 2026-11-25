package specman.editarea.image;

import specman.ChangeInfo;
import specman.model.v002.SvgImageEditAreaModel_V002;

import java.nio.charset.StandardCharsets;

/** An edit area showing an imported SVG graphic. The graphic isn't edited in Specman, so the model persists
 * its source as it was imported. */
public class SvgImageEditArea extends AbstractVectorImageEditArea {
  private final String source;

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
    showPicture(new SvgPicture(source.getBytes(StandardCharsets.UTF_8)));
  }

  @Override
  public SvgImageEditAreaModel_V002 toModel(boolean formatierterText) {
    return new SvgImageEditAreaModel_V002(source, individualScalePercent, getChangeInfo());
  }
}
