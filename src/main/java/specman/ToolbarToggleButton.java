package specman;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import specman.graphics.IconReader;
import specman.graphics.SpecmanIcons;

import javax.swing.*;
import java.awt.*;

/**
 * Toggle button for toolbars. Selected state: muted blue background with a light gray icon, so the active
 * toggle stays clearly visible. Use this for every toolbar toggle to keep the look consistent.
 */
public class ToolbarToggleButton extends JToggleButton {

  /**
   * a good accent color on its own. The contrast was just tested manually and
   * this code is not based on any other color code like FlatLaf's color codes.
   */
  private static final String SELECTED_BACKGROUND = "#5b7ea8";
  private static final Color SELECTED_ICON_COLOR = new Color(0xEE, 0xEE, 0xEE);

  public ToolbarToggleButton(String iconPath, String tooltip) {
    setToolTipText(tooltip);
    setIcon(IconReader.readSvgIcon(iconPath, SpecmanIcons.SIZE_TOOLBAR));
    applyToolbarToggleLook(this);
  }

  /**
   * Gives any toggle button the look of this class, e.g. the SHEF text style and alignment buttons, which are
   * created from actions. The button must already have its icon, because the icon colors depend on the selection.
   */
  public static void applyToolbarToggleLook(AbstractButton button) {
    button.putClientProperty("FlatLaf.style", "toolbar.selectedBackground: " + SELECTED_BACKGROUND);
    button.setMargin(new Insets(0, 0, 0, 0));
    if (button.getIcon() instanceof FlatSVGIcon icon) {
      icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> recolor(color, button)));
    }
  }

  /** Only the black of the Material icons is recolored: light gray on the selected background, the normal
   * label color otherwise, which also follows the theme. */
  private static Color recolor(Color iconColor, AbstractButton button) {
    return Color.BLACK.equals(iconColor) ? iconColorOf(button) : iconColor;
  }

  private static Color iconColorOf(AbstractButton button) {
    return button.isSelected() ? SELECTED_ICON_COLOR : UIManager.getColor("Label.foreground");
  }
}
