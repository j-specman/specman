package classman.editor;

import classman.render.DiagramRenderer;

import java.util.List;
import java.util.Locale;

/** What the diagram editor needs from the application it runs in, and the way it reports its work. The editor is
 * modal, so nothing else changes the source while it is open. */
public interface EditorHost {
  /** Called after each completed operation, with the whole new source of the diagram. */
  void sourceChanged(String source);

  /** @return the current source of the diagram: after {@link #undo()} or {@link #redo()}, the editor reads it from
   * here. */
  String source();

  /** Takes back the last source reported by {@link #sourceChanged(String)}. The editor asks for this only as many
   * times as it has reported sources. */
  void undo();

  /** Restores the source which {@link #undo()} took back, as long as nothing was reported since. */
  void redo();

  /** @return the renderer which knows the colors of the host's change sets and the words of its language */
  DiagramRenderer renderer();

  /** @return the name of the change set whose marks the editor writes, null if the host doesn't track changes */
  String trackedChangeSet();

  /** @return the names of all change sets which the host knows, so that the editor can show what their colors look
   * like */
  List<String> changeSets();

  /** @return the names of the configurations which the user can choose a stereotype from, in the order the user
   * knows them. The renderer tells what each of them looks like, see {@link #renderer()}. */
  List<String> stereotypeConfigs();

  /** @return the language of the editor's texts */
  Locale locale();
}
