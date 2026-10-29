package specman.i18n;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.util.WrappedHtml;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.io.Serial;

import static specman.i18n.Messages.i18n;

/**
 * Header of a dialog: bold title and a description below it on a gradient background, optionally with an icon
 * on the right. Title and description are message keys. The description wraps at {@link #DEFAULT_DESCRIPTION_WIDTH}
 * unless the dialog needs another width. Wrapping requires a defined width, as Swing can't wrap a text
 * to the width the layout happens to offer.
 * <p>
 * Replaces the header panel of Shef, which is made for a one-line description: It doesn't wrap, has a
 * fixed size, no bottom margin and its own layout manager.
 */
public class IHeaderPanel extends JPanel {
  @Serial
  private static final long serialVersionUID = 1L;

  /** In HTML pixels, which Swing scales with the display scaling like fonts. */
  public static final int DEFAULT_DESCRIPTION_WIDTH = 340;

  private static final Color GRADIENT_END_COLOR = new Color(153, 204, 255);
  private static final float TITLE_FONT_SCALE = 4f / 3f;

  public IHeaderPanel(String titleKey, String descriptionKey, Icon icon) {
    this(titleKey, descriptionKey, icon, DEFAULT_DESCRIPTION_WIDTH);
  }

  /** @param icon optional, may be null
   * @param descriptionWidth width in HTML pixels the description wraps at */
  public IHeaderPanel(String titleKey, String descriptionKey, Icon icon, int descriptionWidth) {
    super(new FormLayout("12px, fill:default:grow, 10px, pref, 10px", "6px, pref, 2px, pref, 8px"));
    setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
    JLabel title = new JLabel(i18n(titleKey));
    Font baseFont = title.getFont();
    title.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D() * TITLE_FONT_SCALE));
    JLabel description = new JLabel(WrappedHtml.of(i18n(descriptionKey), descriptionWidth));
    description.setVerticalAlignment(JLabel.TOP);
    add(title, CC.xy(2, 2));
    add(description, CC.xy(2, 4));
    if (icon != null) {
      add(new JLabel(icon), CC.xywh(4, 2, 1, 3, "center, center"));
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setPaint(new GradientPaint(getWidth() * 0.5f, 0, Color.WHITE, getWidth(), 0, GRADIENT_END_COLOR));
      g2.fillRect(0, 0, getWidth(), getHeight());
    }
    finally {
      g2.dispose();
    }
  }
}
