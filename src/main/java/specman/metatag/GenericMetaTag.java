package specman.metatag;

import specman.graphics.SvgIcon;

import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static specman.Specman.editor;
import static specman.graphics.Styles.SCHRITTNR_FONTSIZE;

/**
 * Configurable meta-tag displayed to the left of a step number.
 */
public class GenericMetaTag extends AbstractMetaTag {
  private static final int ICON_GLYPH_SIZE = 10;
  // The bottom-only content lift moves the centered content area upward by half its inset.
  private static final int ICON_LIFT = CONTENT_LIFT / 2;
  private static final int SIDE_PADDING = 2;
  private static final int TOOLTIP_WIDTH = 250;

  private final SvgIcon icon;
  private final Icon liftedIcon = new Icon() {
    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
      icon.paintIcon(component, graphics, x, y + (int) editor().scale(ICON_LIFT));
    }

    @Override
    public int getIconWidth() {
      return icon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
      return icon.getIconHeight();
    }
  };

  private final Color backgroundColor;
  private String freetext;

  /** @param freetext optional freetext shown as tooltip while hovering the tag, may be null */
  public GenericMetaTag(String iconName, String labelText, String freetext, Color backgroundColor, Color borderColor) {
    this.icon = new SvgIcon(iconName, ICON_GLYPH_SIZE);
    this.backgroundColor = backgroundColor;

    setHorizontalAlignment(SwingConstants.CENTER);
    setIcon(liftedIcon);
    setText(labelText);
    setFreetext(freetext);
    setOpaque(true);
    setBackground(backgroundColor);
    setBorderColor(borderColor);
    scale();

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
          editFreetext();
        }
      }
    });
  }

  private void editFreetext() {
    // Without this, the tooltip would pop up on top of the editor as soon as the mouse (still
    // resting on the tag after the click) moves a little.
    setToolTipText(null);
    new FreetextEditorPopup(getFont(), freetext, this::setFreetext, this::updateTooltip).showBelow(this);
  }

  public String getFreetext() {
    return freetext;
  }

  public void setFreetext(String freetext) {
    this.freetext = freetext;
    updateTooltip();
  }

  private void updateTooltip() {
    setToolTipText(toTooltipHtml(freetext));
  }

  /** Plain text is escaped and wrapped at a fixed width, so long or multi-line freetext
   * doesn't produce a single endless tooltip line. */
  private static String toTooltipHtml(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    return "<html><body style='width: " + TOOLTIP_WIDTH + "px'>" + escaped + "</body></html>";
  }

  public void scale() {
    icon.setSize((int) editor().scale(ICON_GLYPH_SIZE));
    setFont(getFont().deriveFont((float) editor().scale(SCHRITTNR_FONTSIZE)));
    setBorder(scaledBorder(backgroundColor, 0, SIDE_PADDING));
  }

  /** Borrows the step number's height, same trick as KlappButton#updateLocation. */
  public void updateLocation(Rectangle stepnumberBounds, int gap) {
    if (stepnumberBounds.height > 0) {
      int width = getPreferredSize().width;
      setBounds(stepnumberBounds.x - gap - width, 0, width, stepnumberBounds.height);
    }
  }

  @Override
  protected int arc() {
    return (int) editor().scale(CORNER_ARC);
  }
}
