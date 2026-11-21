package specman.editarea.image.classman;

import classman.editor.EditorHost;
import classman.render.DiagramRenderer;

import java.util.Locale;

import static specman.ChangeSet.changeset;
import static specman.Specman.editor;

/** What the Classman diagram editor gets from Specman: it reports its source to the edit area, which records it as an
 * undoable edit, and writes change marks if Specman tracks changes. */
class ClassmanEditorHost implements EditorHost {
  private final ClassmanImageEditArea area;

  ClassmanEditorHost(ClassmanImageEditArea area) {
    this.area = area;
  }

  @Override
  public void sourceChanged(String source) {
    area.changeSource(source);
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
  public Locale locale() {
    return Locale.getDefault();
  }
}
