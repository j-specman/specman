package specman.editarea.image.plantuml;

import specman.ChangeInfo;
import specman.editarea.image.AbstractSourceSvgImageEditArea;
import specman.editarea.image.SourceBackup;
import specman.editarea.image.SourceFileKind;
import specman.editarea.image.SourceValidator;
import specman.model.v002.PlantUmlImageEditAreaModel_V002;

import java.awt.event.KeyEvent;

import static specman.Specman.editor;

/** An edit area showing a PlantUML diagram, rendered as SVG so it stays sharp at any zoom level.
 * The diagram is only updated on F5 and when the editor is left, because most intermediate states while typing are
 * not sensible diagrams. The model persists the source only, the diagram is rendered from it when loading. */
public class PlantUmlImageEditArea extends AbstractSourceSvgImageEditArea {
  /** The extensions PlantUML files commonly have. "wsd" is a heritage of WebSequenceDiagrams, which PlantUML tools
   * took over. "iuml" is missing on purpose: Include files have no @startuml and can't be a diagram of their own. */
  public static final SourceFileKind FILE_KIND = new SourceFileKind(
    new String[] {"puml", "plantuml", "pu", "wsd", "txt"}, "msg.filter.plantuml",
    "dialog.plantuml.choose.title", "dialog.plantuml.import.title",
    "dialog.plantuml.choose.empty", "dialog.plantuml.choose.empty.tooltip");

  private static final PlantUmlSourceValidator VALIDATOR = new PlantUmlSourceValidator();

  public PlantUmlImageEditArea(String source, ChangeInfo changeInfo) {
    this(source, null, 1, changeInfo);
  }

  public PlantUmlImageEditArea(PlantUmlImageEditAreaModel_V002 model) {
    this(model.source, backupOf(model.backupSource, model.backupChangeset), model.individualScalePercent,
      model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
  }

  private PlantUmlImageEditArea(String source, SourceBackup backup, float individualScalePercent, ChangeInfo changeInfo) {
    super(source, backup, individualScalePercent, changeInfo);
    displayInitially();
    setBackground(panelColor());
  }

  @Override
  protected byte[] renderSvg(String source) {
    return PlantUmlRenderer.renderSvg(source);
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

  @Override
  public PlantUmlImageEditAreaModel_V002 toModel(boolean formatierterText) {
    return new PlantUmlImageEditAreaModel_V002(getSource(), backupSourceForModel(), backupChangesetForModel(),
      individualScalePercent, getChangeInfo());
  }
}
