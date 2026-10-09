package specman.metatag.generic.config;

import specman.graphics.SvgIcon;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** A button to choose an SVG file as icon, a button to remove the icon again, and a small preview
 * of the chosen icon. The icon is optional and held as SVG markup, not as file reference. */
class IconChooser extends JPanel {
  private static final int PREVIEW_SIZE = 16;
  private static final int BUTTON_GAP = 4;
  private static final int PREVIEW_GAP = 8;
  private static final String NO_ICON_TEXT = "no icon";

  private final JLabel preview = new JLabel(NO_ICON_TEXT);
  private final JButton remove = new JButton("Remove");
  private final Runnable onIconChanged;
  private String iconSvg;

  /** @param initialIconSvg the icon to start with, null for none
   * @param onIconChanged called after the user chose or removed an icon */
  IconChooser(String initialIconSvg, Runnable onIconChanged) {
    this.onIconChanged = onIconChanged;
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    JButton choose = new JButton("Choose SVG...");
    choose.addActionListener(e -> chooseIcon());
    remove.addActionListener(e -> removeIcon());
    add(choose);
    add(Box.createHorizontalStrut(BUTTON_GAP));
    add(remove);
    add(Box.createHorizontalStrut(PREVIEW_GAP));
    add(preview);
    setIconSvg(initialIconSvg);
  }

  /** @return the SVG markup of the chosen icon, null if there is none */
  String getIconSvg() {
    return iconSvg;
  }

  private void setIconSvg(String iconSvg) {
    this.iconSvg = iconSvg;
    remove.setEnabled(iconSvg != null);
    if (iconSvg != null) {
      preview.setText(null);
      preview.setIcon(new SvgIcon(PREVIEW_SIZE, iconSvg));
    }
    else {
      preview.setIcon(null);
      preview.setText(NO_ICON_TEXT);
    }
  }

  private void removeIcon() {
    setIconSvg(null);
    onIconChanged.run();
  }

  private void chooseIcon() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new FileNameExtensionFilter("SVG files", "svg"));
    if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File file = chooser.getSelectedFile();
    try {
      String svg = Files.readString(file.toPath(), StandardCharsets.UTF_8);
      new SvgIcon(PREVIEW_SIZE, svg);
      setIconSvg(svg);
      onIconChanged.run();
    }
    catch (IOException | RuntimeException ex) {
      JOptionPane.showMessageDialog(this, "The file '" + file.getName() + "' can't be used as icon: " + ex.getMessage(),
        "Choose SVG icon", JOptionPane.WARNING_MESSAGE);
    }
  }
}
