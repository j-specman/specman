package classman.editor;

import classman.render.ChangeColors;
import classman.render.DiagramRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A host which remembers the sources the editor reported. */
class RecordingEditorHost implements EditorHost {
  final List<String> reportedSources = new ArrayList<>();
  String trackedChangeSet;

  @Override
  public void sourceChanged(String source) {
    reportedSources.add(source);
  }

  @Override
  public DiagramRenderer renderer() {
    return new DiagramRenderer(ChangeColors.NONE, "Can't be drawn");
  }

  @Override
  public String trackedChangeSet() {
    return trackedChangeSet;
  }

  @Override
  public Locale locale() {
    return Locale.ENGLISH;
  }
}
