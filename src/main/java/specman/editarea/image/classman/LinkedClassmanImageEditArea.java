package specman.editarea.image.classman;

import specman.ChangeInfo;
import specman.ChangeSet;
import specman.editarea.ImageClipboardSelection;
import specman.editarea.image.Picture;
import specman.editarea.image.SourceImageContextMenu;
import specman.link.classman.ClassmanLinkResolution;
import specman.link.classman.ClassmanLinkResolver;
import specman.model.v002.AbstractImageEditAreaModel_V002;
import specman.model.v002.LinkedClassmanImageEditAreaModel_V002;
import specman.util.TextFiles;
import specman.visitor.LinkedClassmanAreaCollector;

import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static specman.Specman.editor;

/** A Classman diagram whose source is in a file of its own, which the model only links to. The user edits the diagram
 * like an embedded one. The file is written when the model is saved, if the user changed the diagram, see
 * {@link #saveLinkedFile()}. So whoever saves last wins, also over the other documents which link the file. The link
 * is what the model keeps, also if it leads nowhere. */
public class LinkedClassmanImageEditArea extends ClassmanImageEditArea {
  private ClassmanLinkTarget target;
  private boolean leftAlone;

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
    updateCursor();
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

  /** @return true if the area shows a placeholder with the link instead of the diagram, because the link leads nowhere
   * or the file is linked by another diagram as well */
  public boolean isPlaceholder() {
    return target.placeholderProblem() != null;
  }

  /** @return the alias which the user can say a file for, null if the diagram can't be repaired that way: Only a link by
   * alias which leads nowhere can. The alias of a duplicate is fine, and standing for another file would break the diagram
   * which links the file. */
  public String relinkableAlias() {
    return isPlaceholder() && !isDuplicate() ? target.alias() : null;
  }

  public String getLink() {
    return target.link();
  }

  /** Follows the link again, after the alias it names its file by was assigned to a file, and shows the diagram of the
   * file. Not undoable. */
  public void resolveAgain() {
    setLink(target.link());
    setSource(target.savedSource());
  }

  /** Follows the link and shows what it leads to, the diagram of the file or a placeholder. The source stays what it
   * is, which {@link specman.undo.props.UDBL#setLinkUDBL} relies on to undo exactly; the one who links another file also
   * sets the source to the one of the file. Not undoable on its own. */
  public void setLink(String link) {
    target = ClassmanLinkTarget.of(link, new ClassmanLinkResolver(editor().getDiagrammDatei()).resolve(link));
    if (isLinkedElsewhere(target.file())) {
      target = target.asDuplicate();
    }
    setToolTipText(link);
    updateCursor();
    show(getSource());
  }

  private boolean isLinkedElsewhere(File file) {
    if (file == null) {
      return false;
    }
    List<LinkedClassmanImageEditArea> others = new ArrayList<>(LinkedClassmanAreaCollector.areasOf(editor()::traverseDiagram));
    others.remove(this);
    return LinkedClassmanDuplicates.isLinkedBy(file, others);
  }

  /** The diagram is not the first in the document to link its file, so it never writes the file and shows a placeholder
   * instead of the diagram. */
  public void markDuplicate() {
    target = target.asDuplicate();
    updateCursor();
    show(getSource());
  }

  /** The hand tells that a click on the placeholder does something. */
  private void updateCursor() {
    setCursor(Cursor.getPredefinedCursor(isPlaceholder() ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    super.mouseClicked(e);
    if (isPlaceholder() && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
      editor().relinkClassman(this);
    }
  }

  /** A placeholder tells why there is no diagram. */
  @Override
  protected Picture pictureOf(String source) {
    String problem = target.placeholderProblem();
    return problem != null ? new ScenePicture(ClassmanRenderer.renderLinkPlaceholder(target.link(), problem))
      : super.pictureOf(source);
  }

  /** A diagram which can't be written is not edited: its source is nobody's diagram, and nothing the user did to it
   * would reach the file. The user can link another file instead. */
  @Override
  public boolean isSourceEditable() {
    return target.file() != null;
  }

  @Override
  protected String importMenuItemKey() {
    return SourceImageContextMenu.RELINK_ITEM;
  }

  /** The user links another file instead of importing a source into the diagram. */
  @Override
  public void importSource() {
    editor().relinkClassman(this);
  }

  @Override
  protected void doubleClicked() {
    if (isSourceEditable()) {
      super.doubleClicked();
    }
  }

  /** The source of a diagram which can't be written is not the diagram of anybody, so only the picture is copied. */
  @Override
  protected Transferable clipboardContent() {
    return target.file() != null ? super.clipboardContent() : new ImageClipboardSelection(getFullSizeImage());
  }

  /** Marks and configurations are in the file, which a diagram that can't be written must not change, and neither one
   * which the user wants left alone, see {@link #setLeftAlone}. */
  @Override
  protected int changeSourceUDBL(String newSource) {
    return target.file() != null && !leftAlone ? super.changeSourceUDBL(newSource) : 0;
  }

  /** While an operation of change mode runs, the diagram keeps its source, so the marks stay in the file. The change
   * of the area itself, being added or deleted in the document, is not affected. */
  public void setLeftAlone(boolean leftAlone) {
    this.leftAlone = leftAlone;
  }

  /** @return true if accepting, rejecting or merging the change set changes the diagram, and so the file */
  public boolean hasMarksToChange(ChangeSet changeSet) {
    return target.file() != null && hasMarksOf(changeSet);
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
