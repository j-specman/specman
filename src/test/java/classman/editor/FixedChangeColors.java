package classman.editor;

import classman.render.ChangeColors;

import java.util.HashMap;
import java.util.Map;

/** Change set colors which a test sets up: the strong and the selection colors it puts in, none for other change sets. */
class FixedChangeColors implements ChangeColors {
  final Map<String, String> strongColors = new HashMap<>();
  final Map<String, String> selectionColors = new HashMap<>();

  @Override
  public String strongColorOf(String changeSet) {
    return strongColors.get(changeSet);
  }

  @Override
  public String selectionColorOf(String changeSet) {
    return selectionColors.get(changeSet);
  }
}
