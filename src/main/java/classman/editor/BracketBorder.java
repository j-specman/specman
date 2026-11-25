package classman.editor;

import javax.swing.border.Border;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/** Draws a fixed text in front of and behind the content of a one-line text field, like the angle brackets around a
 * stereotype, in the font of the field. The user types only what is between them. */
class BracketBorder implements Border {
  private final String before;
  private final String after;

  BracketBorder(String before, String after) {
    this.before = before;
    this.after = after;
  }

  @Override
  public Insets getBorderInsets(Component field) {
    FontMetrics metrics = field.getFontMetrics(field.getFont());
    return new Insets(0, metrics.stringWidth(before), 0, metrics.stringWidth(after));
  }

  @Override
  public boolean isBorderOpaque() { return false; }

  @Override
  public void paintBorder(Component field, Graphics graphics, int x, int y, int width, int height) {
    Graphics2D g = (Graphics2D) graphics.create();
    try {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setFont(field.getFont());
      g.setColor(field.getForeground());
      FontMetrics metrics = g.getFontMetrics();
      g.drawString(before, x, y + metrics.getAscent());
      g.drawString(after, x + width - metrics.stringWidth(after), y + metrics.getAscent());
    }
    finally {
      g.dispose();
    }
  }
}
