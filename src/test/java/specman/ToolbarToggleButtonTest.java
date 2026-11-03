package specman;

import com.formdev.flatlaf.FlatLightLaf;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import specman.graphics.SpecmanIcons;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The selected state is an accent background with a white icon and takes effect with the very next paint. */
class ToolbarToggleButtonTest {

  private static final Color ACCENT = new Color(0x2f6fd1);
  private static LookAndFeel previousLookAndFeel;

  @BeforeAll
  static void useFlatLaf() throws Exception {
    System.setProperty("java.awt.headless", "true");
    previousLookAndFeel = UIManager.getLookAndFeel();
    UIManager.setLookAndFeel(new FlatLightLaf());
  }

  @AfterAll
  static void restoreLookAndFeel() throws Exception {
    UIManager.setLookAndFeel(previousLookAndFeel);
  }

  /** Looks like in the toolbar: ToolbarGroup.addControl gives every button the toolBarButton type. */
  private static ToolbarToggleButton toolbarButton() {
    ToolbarToggleButton button = new ToolbarToggleButton(SpecmanIcons.TRACK_CHANGES, "tip");
    button.putClientProperty("JButton.buttonType", "toolBarButton");
    return button;
  }

  private static BufferedImage paint(ToolbarToggleButton button) {
    Dimension size = button.getPreferredSize();
    button.setSize(size);
    BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    button.paint(g);
    g.dispose();
    return image;
  }

  private static Color pixel(BufferedImage image, int x, int y) {
    return new Color(image.getRGB(x, y), true);
  }

  private static boolean containsColor(BufferedImage image, Color color, int tolerance) {
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        Color c = pixel(image, x, y);
        if (Math.abs(c.getRed() - color.getRed()) <= tolerance
            && Math.abs(c.getGreen() - color.getGreen()) <= tolerance
            && Math.abs(c.getBlue() - color.getBlue()) <= tolerance && c.getAlpha() > 250) {
          return true;
        }
      }
    }
    return false;
  }

  @Test
  void selectedButtonHasAccentBackgroundAndWhiteIcon() {
    ToolbarToggleButton button = toolbarButton();
    button.setSelected(true);
    BufferedImage image = paint(button);
    assertEquals(ACCENT, pixel(image, 3, image.getHeight() / 2));
    assertTrue(containsColor(image, Color.WHITE, 8), "icon is not white");
  }

  @Test
  void iconIsRepaintedAtOnceWhenTheStateChanges() {
    ToolbarToggleButton button = toolbarButton();
    button.setSelected(true);
    assertTrue(containsColor(paint(button), Color.WHITE, 8));

    button.setSelected(false);
    BufferedImage unselected = paint(button);
    assertFalse(containsColor(unselected, Color.WHITE, 8), "icon still white after deselecting");
    assertFalse(containsColor(unselected, ACCENT, 0), "accent background after deselecting");
  }
}
