package specman.editarea.image.classman;

import classman.render.ChangeColors;
import specman.ChangeSet;

import java.awt.Color;

/** The colors of Specman's change sets for Classman diagrams. */
class ClassmanChangeColors implements ChangeColors {

  @Override
  public String strongColorOf(String changeSetName) {
    ChangeSet changeSet = ChangeSet.fromName(changeSetName);
    return changeSet != null ? hex(changeSet.textColor()) : null;
  }

  @Override
  public String selectionColorOf(String changeSetName) {
    ChangeSet changeSet = ChangeSet.fromName(changeSetName);
    return changeSet != null && changeSet.selectColor() != null ? hex(changeSet.selectColor()) : null;
  }

  private static String hex(Color color) {
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }
}
