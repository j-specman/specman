package classman.editor;

import classman.render.ChangeColors;

import java.util.HashMap;
import java.util.Map;

/** Change set colors which a test sets up: no strong colors, but the selection colors it puts in. */
class FixedChangeColors implements ChangeColors {
  final Map<String, String> selectionColors = new HashMap<>();

  @Override
  public String strongColorOf(String changeSet) {
    return null;
  }

  @Override
  public String selectionColorOf(String changeSet) {
    return selectionColors.get(changeSet);
  }
}
