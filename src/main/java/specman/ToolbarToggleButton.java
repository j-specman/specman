package specman;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import specman.graphics.IconReader;
import specman.graphics.SpecmanIcons;

import javax.swing.*;
import java.awt.*;

/**
 * Toggle button for toolbars. Selected state: accent background with a white icon, so the active
 * toggle stays clearly visible. Use this for every toolbar toggle to keep the look consistent.
 */
public class ToolbarToggleButton extends JToggleButton {

  /**
   * a good accent color on its own. The contrast was just tested manually and
   * this code is not based on any other color code like FlatLaf's color codes.
   */
  private static final String SELECTED_BACKGROUND = "#2f6fd1";

  public ToolbarToggleButton(String iconPath, String tooltip) {
    putClientProperty("FlatLaf.style", "toolbar.selectedBackground: " + SELECTED_BACKGROUND);
    setMargin(new Insets(0, 0, 0, 0));
    setToolTipText(tooltip);
    FlatSVGIcon icon = IconReader.readSvgIcon(iconPath, SpecmanIcons.SIZE_TOOLBAR);
    icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Color.BLACK.equals(color)
        ? (isSelected() ? Color.WHITE : UIManager.getColor("Label.foreground"))
        : color));
    setIcon(icon);
  }
}
