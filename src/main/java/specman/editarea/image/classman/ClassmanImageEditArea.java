package specman.editarea.image.classman;

import classman.editor.ClassDiagramEditorWindow;
import classman.model.v001.change.SourceMarks_V001;
import classman.model.v001.change.StereotypeReferences_V001;
import classman.model.v001.io.ClassDiagramParseException;
import org.jetbrains.annotations.NotNull;
import specman.ChangeInfo;
import specman.ChangeSet;
import specman.editarea.image.AbstractSourceImageEditArea;
import specman.editarea.image.Picture;
import specman.editarea.image.SourceFileKind;
import specman.editarea.image.SourceValidator;
import specman.metatag.generic.config.MetaTagConfigListener;
import specman.model.v002.ClassmanImageEditAreaModel_V002;
import specman.undo.props.UDBL;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.Set;

import static specman.ChangeSet.changeset;
import static specman.Specman.editor;
import static specman.graphics.Styles.BACKGROUND_COLOR_STANDARD;

/** An edit area showing a Classman class diagram, painted with Graphics2D so it stays sharp at any zoom level; the PDF
 * gets it as SVG.
 * The model persists the source only, the diagram is laid out and rendered from it when loading. Changes are tracked
 * by the change marks in the source, see ClassDiagram_V001.g4: accepting, rejecting and merging change sets resolves
 * or renames them in the source. Nothing is tracked about the source as a whole. */
public class ClassmanImageEditArea extends AbstractSourceImageEditArea {
  public static final String FILE_EXTENSION = "cld";
  public static final SourceFileKind FILE_KIND = new SourceFileKind(new String[] {FILE_EXTENSION}, "msg.filter.classman",
    "dialog.classman.choose.title", "dialog.classman.import.title",
    "dialog.classman.choose.empty", "dialog.classman.choose.empty.tooltip");

  private static final ClassmanSourceValidator VALIDATOR = new ClassmanSourceValidator();
  private static final SourceMarks_V001 MARKS = new SourceMarks_V001();
  private static final StereotypeReferences_V001 STEREOTYPES = new StereotypeReferences_V001();

  /** Shows the diagram anew when a configuration which its stereotypes may refer to was edited. It is registered as
   * long as the diagram is part of the window, see {@link #addNotify()}. */
  private final MetaTagConfigListener configListener = (oldConfig, newConfig) -> show(getSource());

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

  /** The picture of a diagram which a change set has added stays white inside the frame of the change set: the marks
   * of its content show what is new, and the pale color of the change set behind the picture would make them harder to
   * see. The rest of the area keeps the color. A deleted diagram has the color behind the picture too, since nothing in
   * it says that it is gone. */
  @Override
  protected Color pictureBackground() {
    return getChangeInfo().isAdded() ? BACKGROUND_COLOR_STANDARD : null;
  }

  @Override
  public void addNotify() {
    super.addNotify();
    editor().addMetaTagConfigListener(configListener);
  }

  @Override
  public void removeNotify() {
    editor().removeMetaTagConfigListener(configListener);
    super.removeNotify();
  }

  @Override
  public Set<String> metaTagConfigNamesInUse() {
    return STEREOTYPES.configsUsedIn(getSource());
  }

  @Override
  public List<String> metaTagConfigUserNames(String configName) {
    return STEREOTYPES.classesUsing(getSource(), configName);
  }

  /** The stereotypes which refer to the configuration are taken away, without a change mark: the configuration is
   * not part of the content. */
  @Override
  public void removeMetaTagConfigUseUDBL(String configName) {
    changeSourceUDBL(STEREOTYPES.withoutConfigs(getSource(), Set.of(configName)));
  }

  @Override
  public void dropUnknownMetaTagConfigs(Set<String> dropped) {
    String cleaned = ForeignStereotypes.withoutUnknownConfigs(getSource(), dropped);
    if (!cleaned.equals(getSource())) {
      setSource(cleaned);
    }
  }

  @Override
  protected Picture pictureOf(String source) {
    return new ScenePicture(ClassmanRenderer.renderScene(source));
  }

  @Override
  protected SourceValidator sourceValidator() { return VALIDATOR; }

  @Override
  protected String hintMessageKey() { return "msg.classman.hint"; }

  @Override
  public void importSource() {
    editor().importClassmanSource(this);
  }

  /** Opens the diagram editor, in a window of its own. A source which can't be drawn is for the source editor, which
   * is also available in the context menu. */
  @Override
  protected void doubleClicked() {
    openDiagramEditor();
  }

  /** @return false if the source can't be drawn, which opened the source editor instead */
  private boolean openDiagramEditor() {
    try {
      new ClassDiagramEditorWindow(this, getSource(), new ClassmanEditorHost(this)).setVisible(true);
      return true;
    }
    catch (ClassDiagramParseException | IllegalArgumentException problem) {
      editSource();
      return false;
    }
  }

  /** For a diagram which has just been inserted empty: opens the diagram editor right away, not the source editor
   * like for other diagrams. The relayout which the insertion requested may still be pending, so the layout is
   * completed and the diagram scrolled into view first, because the editor opens near the diagram. The window is
   * modal, so this returns when the user has left it. */
  @Override
  public void startFirstEdit() {
    if (!isShowing()) {
      return;
    }
    SwingUtilities.getRoot(this).validate();
    scrollRectToVisible(new Rectangle(getSize()));
    String insertedSource = getSource();
    if (openDiagramEditor()) {
      firstEditClosed(insertedSource);
    }
  }

  /** Called when the user has left the diagram editor which was opened for the diagram just inserted. If the source is
   * as it was inserted, the user did nothing, and the diagram is taken out again, without trace. Otherwise it stays,
   * and every operation of the editor is an undo step of its own. */
  public void firstEditClosed(String insertedSource) {
    if (getSource().equals(insertedSource)) {
      editor().undoLastEditAndForget();
    }
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
