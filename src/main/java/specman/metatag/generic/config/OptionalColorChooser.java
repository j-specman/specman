package specman.metatag.generic.config;

import specman.i18n.IButton;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Color;

/** A {@link ColorChooseButton} for a color which is optional, plus a button to go back to the
 * default, i.e. no color of our own. */
class OptionalColorChooser extends JPanel {
  private static final int BUTTON_GAP = 4;

  private final ColorChooseButton chooseButton;
  private final JButton removeButton = new IButton("button.default");
  private final Runnable onColorChanged;

  /** @param initialColor the color to start with, null for the default
   * @param onColorChanged called after the user chose a color or went back to the default */
  OptionalColorChooser(String chooserTitleKey, Color initialColor, Runnable onColorChanged) {
    this.onColorChanged = onColorChanged;
    chooseButton = new ColorChooseButton(chooserTitleKey, initialColor, this::colorChanged);
    removeButton.addActionListener(e -> removeColor());
    removeButton.setEnabled(initialColor != null);
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    add(chooseButton);
    add(Box.createHorizontalStrut(BUTTON_GAP));
    add(removeButton);
  }

  /** @return the chosen color, null for the default */
  Color getColor() {
    return chooseButton.getColor();
  }

  private void colorChanged() {
    removeButton.setEnabled(true);
    onColorChanged.run();
  }

  private void removeColor() {
    chooseButton.setColor(null);
    removeButton.setEnabled(false);
    onColorChanged.run();
  }
}
