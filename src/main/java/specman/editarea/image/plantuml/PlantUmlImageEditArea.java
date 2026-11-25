package specman.editarea.image.plantuml;

import org.jetbrains.annotations.NotNull;
import specman.ChangeInfo;
import specman.ChangeSet;
import specman.editarea.image.AbstractSourceImageEditArea;
import specman.editarea.image.Picture;
import specman.editarea.image.SvgPicture;
import specman.editarea.image.SourceBackup;
import specman.editarea.image.SourceFileKind;
import specman.editarea.image.SourceValidator;
import specman.model.v002.PlantUmlImageEditAreaModel_V002;
import specman.undo.props.UDBL;

import java.awt.Color;
import java.awt.event.KeyEvent;

import static specman.ChangeSet.changeset;
import static specman.Specman.editor;
import static specman.util.ObjectUtils.nvl;

/** An edit area showing a PlantUML diagram, rendered as SVG so it stays sharp at any zoom level.
 * The diagram is only updated on F5 and when the editor is left, because most intermediate states while typing are
 * not sensible diagrams. The model persists the source only, the diagram is rendered from it when loading.
 * <p>
 * Changes of the source are tracked as a whole: The first change of the source in change mode keeps the source from
 * before as backup, which marks the diagram as changed and allows to reject the change. Changing the source back to
 * the backup takes the mark away again. */
public class PlantUmlImageEditArea extends AbstractSourceImageEditArea {
  /** The extensions PlantUML files commonly have. "wsd" is a heritage of WebSequenceDiagrams, which PlantUML tools
   * took over. "iuml" is missing on purpose: Include files have no @startuml and can't be a diagram of their own. */
  public static final SourceFileKind FILE_KIND = new SourceFileKind(
    new String[] {"puml", "plantuml", "pu", "wsd", "txt"}, "msg.filter.plantuml",
    "dialog.plantuml.choose.title", "dialog.plantuml.import.title",
    "dialog.plantuml.choose.empty", "dialog.plantuml.choose.empty.tooltip");

  private static final PlantUmlSourceValidator VALIDATOR = new PlantUmlSourceValidator();

  /** The source from before a change set changed it, null as long as the source is unchanged. Rejecting the
   * change brings it back, and changing the source back to it takes the change mark away. */
  private SourceBackup backup;

  public PlantUmlImageEditArea(String source, ChangeInfo changeInfo) {
    this(source, null, 1, changeInfo);
  }

  public PlantUmlImageEditArea(PlantUmlImageEditAreaModel_V002 model) {
    this(model.source, backupOf(model), model.individualScalePercent,
      model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
  }

  private PlantUmlImageEditArea(String source, SourceBackup backup, float individualScalePercent, ChangeInfo changeInfo) {
    super(source, individualScalePercent, changeInfo);
    this.backup = backup;
    displayInitially();
    setBackground(panelColor());
  }

  /** @return the backup which the file holds, null if the file holds none */
  private static SourceBackup backupOf(PlantUmlImageEditAreaModel_V002 model) {
    if (!model.hasBackup()) {
      return null;
    }
    return new SourceBackup(model.backupSource, nvl(ChangeSet.fromName(model.backupChangeset), changeset()));
  }

  @Override
  protected Picture pictureOf(String source) {
    return new SvgPicture(PlantUmlRenderer.renderSvg(source));
  }

  @Override
  protected SourceValidator sourceValidator() { return VALIDATOR; }

  @Override
  protected String hintMessageKey() { return "msg.plantuml.hint"; }

  @Override
  public void importSource() {
    editor().importPlantUmlSource(this);
  }

  /** Renders the diagram anew even if the source is unchanged, because the result can also depend on files
   * included by the source. */
  private void reload() {
    show(getSource());
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

  // ---- source backup ----

  public SourceBackup getBackup() { return backup; }

  /** Not undoable on its own, see {@link #changeSource(String)}. */
  public void setBackup(SourceBackup backup) { this.backup = backup; }

  @Override
  protected Color panelColor() {
    return backup != null ? backup.changeSet().panelColor() : super.panelColor();
  }

  /** In change mode, the first change of the source keeps the source from before as backup, which marks the diagram
   * as changed. Changing the source back to the backup takes the mark away again. A change by another change set
   * doesn't take over the backup of the first one. */
  @Override
  protected void applySourceChange(String newSource) {
    if (backup == null && editor().aenderungenVerfolgen() && getChangeInfo().isUntracked()) {
      setBackupUDBL(new SourceBackup(getSource(), changeset()));
    }
    super.applySourceChange(newSource);
    if (backup != null && newSource.equals(backup.source())) {
      setBackupUDBL(null);
    }
  }

  private void setBackupUDBL(SourceBackup newBackup) {
    UDBL.setSourceBackupUDBL(this, newBackup);
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

  @Override
  public PlantUmlImageEditAreaModel_V002 toModel(boolean formatierterText) {
    return new PlantUmlImageEditAreaModel_V002(getSource(), backup != null ? backup.source() : null,
      backup != null ? backup.changeSet().name : null, individualScalePercent, getChangeInfo());
  }
}
