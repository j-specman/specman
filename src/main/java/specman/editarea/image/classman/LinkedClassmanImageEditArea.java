package specman.editarea.image.classman;

import specman.ChangeInfo;
import specman.link.classman.ClassmanLinkResolution;
import specman.link.classman.ClassmanLinkResolver;
import specman.model.v002.AbstractImageEditAreaModel_V002;
import specman.model.v002.LinkedClassmanImageEditAreaModel_V002;
import specman.util.TextFiles;

import java.io.File;
import java.io.IOException;

import static specman.Specman.editor;

/** A Classman diagram whose source is in a file of its own, which the model only links to. The user edits the diagram
 * like an embedded one. The file is written when the model is saved, if the user changed the diagram, see
 * {@link #saveLinkedFile()}. So whoever saves last wins, also over the other documents which link the file. The link
 * is what the model keeps, also if it leads nowhere. */
public class LinkedClassmanImageEditArea extends ClassmanImageEditArea {
  private ClassmanLinkTarget target;

  public LinkedClassmanImageEditArea(LinkedClassmanImageEditAreaModel_V002 model) {
    this(model.link, new ClassmanLinkResolver(editor().getDiagrammDatei()).resolve(model.link),
      model.individualScalePercent,
      model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
  }

  /** For a diagram which has just been linked, with the result of reading the file. */
  public LinkedClassmanImageEditArea(String link, ClassmanLinkResolution resolution, ChangeInfo changeInfo) {
    this(link, resolution, 1, changeInfo);
  }

  private LinkedClassmanImageEditArea(String link, ClassmanLinkResolution resolution, float individualScalePercent,
                                      ChangeInfo changeInfo) {
    super(resolution.source() != null ? resolution.source() : "", individualScalePercent, changeInfo);
    this.target = ClassmanLinkTarget.of(link, resolution);
    setToolTipText(link);
    displayInitially();
  }

  /** @return the file which is written when the diagram is saved, null if there is none, see
   * {@link ClassmanLinkTarget#file()} */
  public File linkedFile() {
    return target.file();
  }

  public boolean isDuplicate() {
    return target.isDuplicate();
  }

  /** The diagram is not the first in the document to link its file, so it never writes the file. */
  public void markDuplicate() {
    target = target.asDuplicate();
  }

  /** @return true if the user changed the diagram since its file was read or written, and the file is one which can be
   * written */
  public boolean needsSaving() {
    return target.file() != null && !getSource().equals(target.savedSource());
  }

  /** Writes the source to the file, exactly as it is. */
  public void saveLinkedFile() throws IOException {
    TextFiles.write(target.file(), getSource());
    target = target.saved(getSource());
  }

  @Override
  public AbstractImageEditAreaModel_V002 toModel(boolean formatierterText) {
    return new LinkedClassmanImageEditAreaModel_V002(target.link(), individualScalePercent, getChangeInfo());
  }
}
