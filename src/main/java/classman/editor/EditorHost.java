package classman.editor;

import classman.render.DiagramRenderer;

import java.util.Locale;

/** What the diagram editor needs from the application it runs in, and the way it reports its work. The editor is
 * modal, so nothing else changes the source while it is open. */
public interface EditorHost {
  /** Called after each completed operation, with the whole new source of the diagram. */
  void sourceChanged(String source);

  /** @return the renderer which knows the colors of the host's change sets and the words of its language */
  DiagramRenderer renderer();

  /** @return the name of the change set whose marks the editor writes, null if the host doesn't track changes */
  String trackedChangeSet();

  /** @return the language of the editor's texts */
  Locale locale();
}
