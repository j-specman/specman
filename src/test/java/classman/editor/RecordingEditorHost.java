package classman.editor;

import classman.render.DiagramRenderer;
import classman.render.StereotypeLook;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A host which remembers the sources the editor reported. It has no history: undo and redo are tested with
 * Specman's own, see ClassmanEditorSessionTest. */
class RecordingEditorHost implements EditorHost {
  final List<String> reportedSources = new ArrayList<>();
  String trackedChangeSet;
  final FixedChangeColors changeColors = new FixedChangeColors();
  /** The configurations of the host which stereotypes can refer to, by name. */
  final Map<String, StereotypeLook> stereotypeLooks = new LinkedHashMap<>();

  @Override
  public void sourceChanged(String source) {
    reportedSources.add(source);
  }

  @Override
  public String source() {
    return reportedSources.get(reportedSources.size() - 1);
  }

  @Override
  public void undo() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void redo() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DiagramRenderer renderer() {
    return new DiagramRenderer(changeColors, stereotypeLooks::get, "Can't be drawn");
  }

  @Override
  public String trackedChangeSet() {
    return trackedChangeSet;
  }

  @Override
  public List<String> changeSets() {
    return List.copyOf(changeColors.strongColors.keySet());
  }

  @Override
  public List<String> stereotypeConfigs() {
    return List.copyOf(stereotypeLooks.keySet());
  }

  @Override
  public Locale locale() {
    return Locale.ENGLISH;
  }
}
