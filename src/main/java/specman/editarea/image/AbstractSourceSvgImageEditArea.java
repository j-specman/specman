package specman.editarea.image;

import org.jetbrains.annotations.NotNull;
import specman.ChangeInfo;
import specman.ChangeSet;
import specman.EditorI;
import specman.pdf.ShapeImage;
import specman.undo.manager.UndoRecording;
import specman.undo.props.UDBL;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import static specman.ChangeSet.changeset;
import static specman.Specman.editor;
import static specman.util.ObjectUtils.nvl;

/** An SVG edit area which shows a diagram rendered from its source. A double click opens an editor for the source to
 * the right of the diagram, a right click its context menu. The diagram is only updated when the editor is left or the
 * user asks for a preview, because most intermediate states while typing are not sensible diagrams.
 * <p>
 * Changes of the source are tracked as a whole: The first change of the source in change mode keeps the source from
 * before as backup, which marks the diagram as changed and allows to reject the change. Changing the source back to
 * the backup takes the mark away again.
 * <p>
 * Subclasses say how the source is rendered and checked, see {@link #renderSvg(String)} and {@link #sourceValidator()}. */
public abstract class AbstractSourceSvgImageEditArea extends AbstractSvgImageEditArea {
  private String source;
  /** Differs from the source while the editor shows the preview the user asked for. */
  private String displayedSource;
  /** The source from before a change set changed it, null as long as the source is unchanged. Rejecting the
   * change brings it back, and changing the source back to it takes the change mark away. */
  private SourceBackup backup;
  /** True from the insertion of a new diagram until the user confirmed some content. Leaving the editor
   * before that takes the diagram out again, as if it had never been inserted. */
  private boolean awaitingFirstContent;

  protected AbstractSourceSvgImageEditArea(String source, SourceBackup backup, float individualScalePercent,
                                           ChangeInfo changeInfo) {
    super(changeInfo, individualScalePercent);
    this.source = source;
    this.backup = backup;
  }

  /** @return the backup which the file holds for the given values, null if the file holds none */
  protected static SourceBackup backupOf(String backupSource, String backupChangesetName) {
    if (backupSource == null) {
      return null;
    }
    return new SourceBackup(backupSource, nvl(ChangeSet.fromName(backupChangesetName), changeset()));
  }

  /** @return the diagram for the given source as SVG */
  protected abstract byte[] renderSvg(String source);

  protected abstract SourceValidator sourceValidator();

  /** @return the message key of the hint which the editor shows below the source as long as it has no problem */
  protected abstract String hintMessageKey();

  /** Asks the user for a file and replaces the source by its content. */
  public abstract void importSource();

  /** Subclasses call this once they are constructed, to show the diagram for the source. */
  protected void displayInitially() {
    displayedSource = source;
    showSvg(renderSvg(source));
  }

  public String getSource() { return source; }

  /** Not undoable on its own, see {@link #changeSource(String)}. */
  public void setSource(String source) {
    this.source = source;
    display(source);
  }

  public SourceBackup getBackup() { return backup; }

  /** Not undoable on its own, see {@link #changeSource(String)}. */
  public void setBackup(SourceBackup backup) { this.backup = backup; }

  /** @return the source from before the change for the model, null if the source is unchanged */
  public String backupSourceForModel() { return backup != null ? backup.source() : null; }

  /** @return the name of the change set which changed the source for the model, null if the source is unchanged */
  public String backupChangesetForModel() { return backup != null ? backup.changeSet().name : null; }

  @Override
  protected Color panelColor() {
    return backup != null ? backup.changeSet().panelColor() : super.panelColor();
  }

  // ---- display ----

  private void display(String sourceToShow) {
    if (!sourceToShow.equals(displayedSource)) {
      show(sourceToShow);
    }
  }

  /** Shows the diagram for the given source, which doesn't have to be the source of the area: The editor uses this
   * for its preview. */
  protected void show(String sourceToShow) {
    displayedSource = sourceToShow;
    showSvg(renderSvg(sourceToShow));
    imageChanged();
  }

  /** Rendered anew, as PDF export is rare and rendering is cheap, so there is no need to keep the SVG in memory. */
  @Override
  protected ShapeImage createShapeImage() {
    return vectorShapeImage(renderSvg(displayedSource));
  }

  // ---- editing ----

  @Override
  public void mouseClicked(MouseEvent e) {
    super.mouseClicked(e);
    if (SwingUtilities.isRightMouseButton(e)) {
      SourceImageContextMenu.open(this, e);
    }
    else if (e.getClickCount() == 2 && !getChangeInfo().isDeleted()) {
      editSource();
    }
  }

  public void editSource() {
    SourceEditorPopup popup = newEditorPopup();
    if (awaitingFirstContent) {
      int emptyLineStart = source.indexOf('\n') + 1;
      popup.select(emptyLineStart, emptyLineStart);
    }
    popup.showRightOf(this);
  }

  protected SourceEditorPopup newEditorPopup() {
    return new SourceEditorPopup(source, this::commitSource, this::editorClosed, this::show, sourceValidator(),
      hintMessageKey());
  }

  /** For a diagram that has just been inserted with an empty template as source, whose second line is empty: lets
   * the user enter the content right away. The relayout which the insertion requested may still be pending, and the
   * editor is placed by the diagram's position on screen at the time it is shown. So the layout is completed and the
   * diagram scrolled into view first, otherwise the editor can end up far from the diagram, e.g. for a diagram in
   * the outro. */
  public void startFirstEdit() {
    awaitingFirstContent = true;
    if (isShowing()) {
      SwingUtilities.getRoot(this).validate();
      scrollRectToVisible(new Rectangle(getSize()));
      editSource();
    }
  }

  /** Called when the editor was closed with source it accepted. The first content of a new diagram belongs to its
   * insertion, so a single undo takes the whole diagram out. */
  public void commitSource(String newSource) {
    if (awaitingFirstContent) {
      awaitingFirstContent = false;
      setSource(newSource);
    }
    else {
      changeSource(newSource);
    }
  }

  /** Called when the editor was closed, no matter if the source was committed or discarded. A new diagram which got
   * no content is taken out again, without trace. */
  public void editorClosed() {
    if (awaitingFirstContent) {
      awaitingFirstContent = false;
      editor().undoLastEditAndForget();
    }
    else {
      display(source);
    }
  }

  /** In change mode, the first change of the source keeps the source from before as backup, which marks the diagram
   * as changed. Changing the source back to the backup takes the mark away again. A change by another change set
   * doesn't take over the backup of the first one. */
  public void changeSource(String newSource) {
    if (newSource.equals(source)) {
      return;
    }
    EditorI editor = editor();
    try (UndoRecording ur = editor.composeUndo()) {
      if (backup == null && editor.aenderungenVerfolgen() && getChangeInfo().isUntracked()) {
        setBackupUDBL(new SourceBackup(source, changeset()));
      }
      UDBL.setSourceUDBL(this, newSource);
      if (backup != null && newSource.equals(backup.source())) {
        setBackupUDBL(null);
      }
    }
  }

  private void setBackupUDBL(SourceBackup newBackup) {
    UDBL.setSourceBackupUDBL(this, newBackup);
    setEditBackgroundUDBL(null);
  }

  // ---- change mode ----

  private boolean isChangedBy(ChangeSet changeSet) {
    return backup != null && backup.changedBy(changeSet);
  }

  @Override
  public int aenderungenUebernehmen() {
    if (isChangedBy(changeset())) {
      setBackupUDBL(null);
      return 1;
    }
    return super.aenderungenUebernehmen();
  }

  @Override
  public int aenderungenVerwerfen() {
    if (isChangedBy(changeset())) {
      restoreBackupUDBL();
      return 1;
    }
    return super.aenderungenVerwerfen();
  }

  /** A deleted diagram shows the source from before the change, like the passages of a text which existed before. */
  @Override
  public void setGeloeschtMarkiertStilUDBL(ChangeSet triggerSet) {
    if (backup != null) {
      restoreBackupUDBL();
    }
    super.setGeloeschtMarkiertStilUDBL(triggerSet);
  }

  private void restoreBackupUDBL() {
    UDBL.setSourceUDBL(this, backup.source());
    setBackupUDBL(null);
  }

  @Override
  public void mergeChangeSetUDBL(@NotNull ChangeSet target, @NotNull ChangeSet source, boolean withMarkups) {
    if (isChangedBy(source)) {
      setBackupUDBL(backup.reassign(target));
    }
    super.mergeChangeSetUDBL(target, source, withMarkups);
  }

  @Override
  public void aenderungsmarkierungenEntfernen() {
    if (isChangedBy(changeset())) {
      backup = null;
    }
    super.aenderungsmarkierungenEntfernen();
  }
}
