package specman.metatag.generic.config;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Color;

/** A {@link ColorChooseButton} for a color which is optional, plus a button to remove the color
 * again. */
class OptionalColorChooser extends JPanel {
  private static final int BUTTON_GAP = 4;

  private final ColorChooseButton chooseButton;
  private final JButton removeButton = new JButton("Remove");
  private final Runnable onColorChanged;

  /** @param initialColor the color to start with, null for none
   * @param onColorChanged called after the user chose or removed a color */
  OptionalColorChooser(String chooserTitle, Color initialColor, Runnable onColorChanged) {
    this.onColorChanged = onColorChanged;
    chooseButton = new ColorChooseButton(chooserTitle, initialColor, this::colorChanged);
    removeButton.addActionListener(e -> removeColor());
    removeButton.setEnabled(initialColor != null);
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    add(chooseButton);
    add(Box.createHorizontalStrut(BUTTON_GAP));
    add(removeButton);
  }

  /** @return the chosen color, null for none */
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
