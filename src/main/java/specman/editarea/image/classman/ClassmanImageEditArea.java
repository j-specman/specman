package specman.editarea.image.classman;

import specman.ChangeInfo;
import specman.editarea.image.AbstractSourceSvgImageEditArea;
import specman.editarea.image.SourceBackup;
import specman.editarea.image.SourceFileKind;
import specman.editarea.image.SourceValidator;
import specman.model.v002.ClassmanImageEditAreaModel_V002;

import static specman.Specman.editor;

/** An edit area showing a Classman class diagram, rendered as SVG so it stays sharp at any zoom level.
 * The model persists the source only, the diagram is laid out and rendered from it when loading. Changes of the
 * source are tracked as a whole, see {@link AbstractSourceSvgImageEditArea}. That is an interim solution: Classman
 * is meant to get change marks of its own, and this class is then to leave that base. */
public class ClassmanImageEditArea extends AbstractSourceSvgImageEditArea {
  public static final String FILE_EXTENSION = "cld";
  public static final SourceFileKind FILE_KIND = new SourceFileKind(new String[] {FILE_EXTENSION}, "msg.filter.classman",
    "dialog.classman.choose.title", "dialog.classman.import.title",
    "dialog.classman.choose.empty", "dialog.classman.choose.empty.tooltip");

  private static final ClassmanSourceValidator VALIDATOR = new ClassmanSourceValidator();

  public ClassmanImageEditArea(String source, ChangeInfo changeInfo) {
    this(source, null, 1, changeInfo);
  }

  public ClassmanImageEditArea(ClassmanImageEditAreaModel_V002 model) {
    this(model.source, backupOf(model.backupSource, model.backupChangeset), model.individualScalePercent,
      model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
  }

  private ClassmanImageEditArea(String source, SourceBackup backup, float individualScalePercent, ChangeInfo changeInfo) {
    super(source, backup, individualScalePercent, changeInfo);
    displayInitially();
    setBackground(panelColor());
  }

  @Override
  protected byte[] renderSvg(String source) {
    return ClassmanRenderer.renderSvg(source);
  }

  @Override
  protected SourceValidator sourceValidator() { return VALIDATOR; }

  @Override
  protected String hintMessageKey() { return "msg.classman.hint"; }

  @Override
  public void importSource() {
    editor().importClassmanSource(this);
  }

  @Override
  public ClassmanImageEditAreaModel_V002 toModel(boolean formatierterText) {
    return new ClassmanImageEditAreaModel_V002(getSource(), backupSourceForModel(), backupChangesetForModel(),
      individualScalePercent, getChangeInfo());
  }
}
