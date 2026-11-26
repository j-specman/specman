package specman.editarea.image.classman;

import specman.ChangeInfo;
import specman.link.classman.ClassmanLinkResolution;
import specman.link.classman.ClassmanLinkResolver;
import specman.model.v002.AbstractImageEditAreaModel_V002;
import specman.model.v002.LinkedClassmanImageEditAreaModel_V002;

/** A Classman diagram whose source is in a file of its own, which the model only links to. The user edits the diagram
 * like an embedded one. The link is what the model keeps, also if it leads nowhere. */
public class LinkedClassmanImageEditArea extends ClassmanImageEditArea {
  private final String link;

  public LinkedClassmanImageEditArea(LinkedClassmanImageEditAreaModel_V002 model) {
    this(model.link, new ClassmanLinkResolver().resolve(model.link), model.individualScalePercent,
      model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
  }

  /** For a diagram which has just been linked, with the result of reading the file. */
  public LinkedClassmanImageEditArea(String link, ClassmanLinkResolution resolution, ChangeInfo changeInfo) {
    this(link, resolution, 1, changeInfo);
  }

  private LinkedClassmanImageEditArea(String link, ClassmanLinkResolution resolution, float individualScalePercent,
                                      ChangeInfo changeInfo) {
    super(resolution.source() != null ? resolution.source() : "", individualScalePercent, changeInfo);
    this.link = link;
    displayInitially();
  }

  @Override
  public AbstractImageEditAreaModel_V002 toModel(boolean formatierterText) {
    return new LinkedClassmanImageEditAreaModel_V002(link, individualScalePercent, getChangeInfo());
  }
}
