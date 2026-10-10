package specman.metatag.generic.config;

import specman.graphics.SvgIcon;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** A button to choose an SVG file as icon, a button to remove the icon again, and a small preview
 * of the chosen icon. The icon is optional and held as SVG markup, not as file reference. An icon
 * is displayed tiny, so there is a red warning about an unreasonably large SVG; it is only a
 * warning, the SVG can still be used. The warning is a component of its own, see
 * {@link #getSizeWarning()}, to be placed below the chooser. */
class IconChooser extends JPanel {
  private static final int PREVIEW_SIZE = 16;
  private static final int BUTTON_GAP = 4;
  private static final int PREVIEW_GAP = 8;
  private static final int WARNING_GAP = 4;
  private static final int BYTES_PER_KB = 1024;
  private static final int SIZE_WARNING_BYTES = 2 * BYTES_PER_KB;
  private static final String NO_ICON_TEXT = "no icon";

  private final JLabel preview = new JLabel(NO_ICON_TEXT);
  private final JButton remove = new JButton("Remove");
  private final JLabel sizeWarning = new JLabel();
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
    sizeWarning.setForeground(Color.RED);
    sizeWarning.setBorder(BorderFactory.createEmptyBorder(WARNING_GAP, 0, 0, 0));
    sizeWarning.setVisible(false);
    setIconSvg(initialIconSvg);
  }

  /** @return the warning about an oversized SVG, invisible as long as there is none. The component
   * takes no room while invisible if it is placed in a FormLayout, which honors the visibility. */
  JLabel getSizeWarning() {
    return sizeWarning;
  }

  private static String sizeWarningText(int sizeInBytes) {
    return "Warning: the SVG is " + (sizeInBytes + BYTES_PER_KB - 1) / BYTES_PER_KB + " KB - icons should stay below "
      + SIZE_WARNING_BYTES / BYTES_PER_KB + " KB.";
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
    updateSizeWarning();
  }

  /** The warning only takes room while it is shown, so a window containing the chooser is packed
   * again to grow or shrink with it. Before the window exists, its first pack takes care. */
  private void updateSizeWarning() {
    int size = iconSvg != null ? iconSvg.getBytes(StandardCharsets.UTF_8).length : 0;
    boolean tooLarge = size > SIZE_WARNING_BYTES;
    if (tooLarge) {
      sizeWarning.setText(sizeWarningText(size));
    }
    if (sizeWarning.isVisible() != tooLarge) {
      sizeWarning.setVisible(tooLarge);
      Window window = SwingUtilities.getWindowAncestor(this);
      if (window != null) {
        window.pack();
      }
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
