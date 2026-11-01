package specman.editarea.image.plantuml;

import org.jetbrains.annotations.NotNull;
import specman.ChangeInfo;
import specman.ChangeSet;
import specman.EditorI;
import specman.editarea.image.AbstractSvgImageEditArea;
import specman.model.v002.PlantUmlImageEditAreaModel_V002;
import specman.pdf.ShapeImage;
import specman.undo.manager.UndoRecording;
import specman.undo.props.UDBL;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import static specman.ChangeSet.changeset;
import static specman.Specman.editor;
import static specman.util.ObjectUtils.nvl;

/** An edit area showing a PlantUML diagram, rendered as SVG so it stays sharp at any zoom level.
 * A double click opens a note-like editor for the source to the right of the diagram. The diagram is only updated
 * on F5 and when the editor is left, because most intermediate states while typing are not sensible diagrams.
 * The model persists the source only, the diagram is rendered from it when loading. */
public class PlantUmlImageEditArea extends AbstractSvgImageEditArea {
  private String source;
  /** The source from before a change set changed it, null as long as the source is unchanged. Rejecting the
   * change brings it back, and changing the source back to it takes the change mark away. */
  private PlantUmlSourceBackup backup;
  /** Differs from the source while the editor shows the preview the user asked for by F5. */
  private String displayedSource;
  /** True from the insertion of a new diagram until the user confirmed some content. Leaving the editor
   * before that takes the diagram out again, as if it had never been inserted. */
  private boolean awaitingFirstContent;
  public PlantUmlImageEditArea(String source, ChangeInfo changeInfo) {
    this(source, null, 1, changeInfo);
  }

  public PlantUmlImageEditArea(PlantUmlImageEditAreaModel_V002 model) {
    this(model.source, backupOf(model), model.individualScalePercent,
      model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
  }

  private PlantUmlImageEditArea(String source, PlantUmlSourceBackup backup, float individualScalePercent, ChangeInfo changeInfo) {
    super(changeInfo, individualScalePercent);
    this.source = source;
    this.backup = backup;
    this.displayedSource = source;
    render(source);
    setBackground(panelColor());
  }

  private static PlantUmlSourceBackup backupOf(PlantUmlImageEditAreaModel_V002 model) {
    if (!model.hasBackup()) {
      return null;
    }
    return new PlantUmlSourceBackup(model.backupSource, nvl(ChangeSet.fromName(model.backupChangeset), changeset()));
  }

  private void render(String sourceToRender) {
    showSvg(PlantUmlRenderer.renderSvg(sourceToRender));
  }

  public String getSource() { return source; }

  /** Not undoable on its own, see {@link #changeSource(String)}. */
  public void setSource(String source) {
    this.source = source;
    display(source);
  }

  public PlantUmlSourceBackup getBackup() { return backup; }

  /** Not undoable on its own, see {@link #changeSource(String)}. */
  public void setBackup(PlantUmlSourceBackup backup) { this.backup = backup; }

  @Override
  protected Color panelColor() {
    return backup != null ? backup.changeSet().panelColor() : super.panelColor();
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
        setBackupUDBL(new PlantUmlSourceBackup(source, changeset()));
      }
      UDBL.setPlantUmlSourceUDBL(this, newSource);
      if (backup != null && newSource.equals(backup.source())) {
        setBackupUDBL(null);
      }
    }
  }

  private void setBackupUDBL(PlantUmlSourceBackup newBackup) {
    UDBL.setPlantUmlBackupUDBL(this, newBackup);
    setEditBackgroundUDBL(null);
  }

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
    UDBL.setPlantUmlSourceUDBL(this, backup.source());
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

  /** For a diagram that has just been inserted with empty content: lets the user enter the content right away.
   * The relayout which the insertion requested may still be pending, and the editor is placed by the diagram's
   * position on screen at the time it is shown. So the layout is completed and the diagram scrolled into view
   * first, otherwise the editor can end up far from the diagram, e.g. for a diagram in the outro. */
  public void startFirstEdit() {
    awaitingFirstContent = true;
    if (isShowing()) {
      SwingUtilities.getRoot(this).validate();
      scrollRectToVisible(new Rectangle(getSize()));
      editSource();
    }
  }

  /** The first content of a new diagram belongs to its insertion, so a single undo takes the whole diagram out. */
  public void commitSource(String newSource) {
    if (awaitingFirstContent) {
      awaitingFirstContent = false;
      setSource(newSource);
    }
    else {
      changeSource(newSource);
    }
  }

  /** Called when the editor was closed, no matter if the source was committed or discarded. */
  public void editorClosed() {
    if (awaitingFirstContent) {
      awaitingFirstContent = false;
      editor().undoLastEditAndForget();
    }
    else {
      display(source);
    }
  }

  private void display(String sourceToShow) {
    if (!sourceToShow.equals(displayedSource)) {
      show(sourceToShow);
    }
  }

  private void show(String sourceToShow) {
    displayedSource = sourceToShow;
    render(sourceToShow);
    imageChanged();
  }

  /** Renders the diagram anew even if the source is unchanged, because the result can also depend on files
   * included by the source. */
  private void reload() {
    show(source);
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_F5) {
      reload();
      e.consume();
    }
    else {
      super.keyPressed(e);
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    super.mouseClicked(e);
    if (SwingUtilities.isRightMouseButton(e)) {
      PlantUmlContextMenu.open(this, e);
    }
    else if (e.getClickCount() == 2 && !getChangeInfo().isDeleted()) {
      editSource();
    }
  }

  public void editSource() {
    PlantUmlSourceEditorPopup popup = new PlantUmlSourceEditorPopup(source, this::commitSource, this::editorClosed, this::show);
    if (awaitingFirstContent) {
      int emptyLineStart = source.indexOf('\n') + 1;
      popup.select(emptyLineStart, emptyLineStart);
    }
    popup.showRightOf(this);
  }

  /** Rendered anew, as PDF export is rare and rendering is cheap, so there is no need to keep the SVG in memory. */
  @Override
  protected ShapeImage createShapeImage() {
    return vectorShapeImage(PlantUmlRenderer.renderSvg(displayedSource));
  }

  @Override
  public PlantUmlImageEditAreaModel_V002 toModel(boolean formatierterText) {
    return new PlantUmlImageEditAreaModel_V002(source,
      backup != null ? backup.source() : null,
      backup != null ? backup.changeSet().name : null,
      individualScalePercent, getChangeInfo());
  }
}
