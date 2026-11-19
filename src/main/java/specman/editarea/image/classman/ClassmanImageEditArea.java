package specman.editarea.image.classman;

import org.jetbrains.annotations.NotNull;
import specman.ChangeInfo;
import specman.ChangeSet;
import specman.editarea.image.AbstractSourceSvgImageEditArea;
import specman.editarea.image.SourceFileKind;
import specman.editarea.image.SourceValidator;
import specman.model.v002.ClassmanImageEditAreaModel_V002;
import specman.undo.props.UDBL;

import static specman.ChangeSet.changeset;
import static specman.Specman.editor;

/** An edit area showing a Classman class diagram, rendered as SVG so it stays sharp at any zoom level.
 * The model persists the source only, the diagram is laid out and rendered from it when loading. Changes are tracked
 * by the change marks in the source, see ClassDiagram_V001.g4: accepting, rejecting and merging change sets resolves
 * or renames them in the source. Nothing is tracked about the source as a whole. */
public class ClassmanImageEditArea extends AbstractSourceSvgImageEditArea {
  public static final String FILE_EXTENSION = "cld";
  public static final SourceFileKind FILE_KIND = new SourceFileKind(new String[] {FILE_EXTENSION}, "msg.filter.classman",
    "dialog.classman.choose.title", "dialog.classman.import.title",
    "dialog.classman.choose.empty", "dialog.classman.choose.empty.tooltip");

  private static final ClassmanSourceValidator VALIDATOR = new ClassmanSourceValidator();
  private static final ClassmanSourceMarks MARKS = new ClassmanSourceMarks();

  public ClassmanImageEditArea(String source, ChangeInfo changeInfo) {
    this(source, 1, changeInfo);
  }

  public ClassmanImageEditArea(ClassmanImageEditAreaModel_V002 model) {
    this(model.source, model.individualScalePercent,
      model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
  }

  private ClassmanImageEditArea(String source, float individualScalePercent, ChangeInfo changeInfo) {
    super(source, individualScalePercent, changeInfo);
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

  /** Shows the diagram painted with Graphics2D, the first step towards the diagram editor. The source editor is
   * still available in the context menu. */
  @Override
  protected void doubleClicked() {
    new ClassmanDiagramDialog(this, ClassmanRenderer.renderScene(getSource())).setVisible(true);
  }

  // ---- change mode ----

  @Override
  public int aenderungenUebernehmen() {
    int changes = changeSourceUDBL(MARKS.accepted(getSource(), changeset().name));
    return changes + super.aenderungenUebernehmen();
  }

  @Override
  public int aenderungenVerwerfen() {
    int changes = changeSourceUDBL(MARKS.rejected(getSource(), changeset().name));
    return changes + super.aenderungenVerwerfen();
  }

  @Override
  public void mergeChangeSetUDBL(@NotNull ChangeSet target, @NotNull ChangeSet source, boolean withMarkups) {
    changeSourceUDBL(MARKS.renamed(getSource(), source.name, target.name));
    super.mergeChangeSetUDBL(target, source, withMarkups);
  }

  /** @return 1 if the source changed, otherwise 0 */
  private int changeSourceUDBL(String newSource) {
    if (newSource.equals(getSource())) {
      return 0;
    }
    UDBL.setSourceUDBL(this, newSource);
    return 1;
  }

  @Override
  public ClassmanImageEditAreaModel_V002 toModel(boolean formatierterText) {
    return new ClassmanImageEditAreaModel_V002(getSource(), individualScalePercent, getChangeInfo());
  }
}
