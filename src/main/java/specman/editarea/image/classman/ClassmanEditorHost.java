package specman.editarea.image.classman;

import classman.editor.EditorHost;
import classman.render.DiagramRenderer;
import specman.ChangeSet;

import java.util.List;
import java.util.Locale;

import static specman.ChangeSet.changeset;
import static specman.Specman.editor;

/** What the Classman diagram editor gets from Specman: it reports its source to the edit area, which records it as an
 * undoable edit, and writes change marks if Specman tracks changes. */
public class ClassmanEditorHost implements EditorHost {
  private final ClassmanImageEditArea area;

  public ClassmanEditorHost(ClassmanImageEditArea area) {
    this.area = area;
  }

  @Override
  public void sourceChanged(String source) {
    area.changeSource(source);
  }

  @Override
  public String source() {
    return area.getSource();
  }

  @Override
  public void undo() {
    editor().getUndoManager().undo();
  }

  @Override
  public void redo() {
    editor().getUndoManager().redo();
  }

  @Override
  public DiagramRenderer renderer() {
    return ClassmanRenderer.renderer();
  }

  @Override
  public String trackedChangeSet() {
    return editor().aenderungenVerfolgen() ? changeset().name : null;
  }

  @Override
  public List<String> changeSets() {
    return List.copyOf(ChangeSet.ALL.keySet());
  }

  @Override
  public Locale locale() {
    return Locale.getDefault();
  }
}
