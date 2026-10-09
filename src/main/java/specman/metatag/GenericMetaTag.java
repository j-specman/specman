package specman.metatag;

import specman.graphics.SvgIcon;
import specman.undo.UndoableMetaTagFreetextChanged;

import javax.swing.Icon;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

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
  private static final int FEEDBACK_DURATION_MS = 3000;

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
  private Popup feedbackPopup;
  private Timer feedbackTimer;
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
    hideFreetextFeedback();
    // Without this, the tooltip would pop up on top of the editor as soon as the mouse (still
    // resting on the tag after the click) moves a little.
    setToolTipText(null);
    new FreetextEditorPopup(getFont(), freetext, this::commitFreetext, this::updateTooltip).showBelow(this);
  }

  /** The editor delivers an empty string for a cleared text, while a never-edited tag has null -
   * both mean "no freetext", so switching between them is not an edit worth an undo entry. */
  private void commitFreetext(String newFreetext) {
    String oldFreetext = freetext;
    if (Objects.toString(oldFreetext, "").equals(Objects.toString(newFreetext, ""))) {
      return;
    }
    setFreetext(newFreetext);
    editor().addEdit(new UndoableMetaTagFreetextChanged(this, oldFreetext, newFreetext));
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

  /** Shows the tooltip for a few seconds without the mouse hovering over the tag, e.g. as visual
   * feedback which tag was just changed by an undo/redo. Swing has no public API to trigger a
   * tooltip programmatically, so a JToolTip is put into a popup by hand. Nothing is shown if the
   * tag has no freetext or is currently not visible. */
  public void showFreetextFeedback() {
    hideFreetextFeedback();
    String tipText = toTooltipHtml(freetext);
    if (tipText == null || !isShowing()) {
      return;
    }
    scrollRectToVisible(new Rectangle(getSize()));
    JToolTip toolTip = createToolTip();
    toolTip.setTipText(tipText);
    Point location = new Point(0, getHeight());
    SwingUtilities.convertPointToScreen(location, this);
    feedbackPopup = PopupFactory.getSharedInstance().getPopup(this, toolTip, location.x, location.y);
    feedbackPopup.show();
    feedbackTimer = new Timer(FEEDBACK_DURATION_MS, e -> hideFreetextFeedback());
    feedbackTimer.setRepeats(false);
    feedbackTimer.start();
  }

  public void hideFreetextFeedback() {
    if (feedbackTimer != null) {
      feedbackTimer.stop();
      feedbackTimer = null;
    }
    if (feedbackPopup != null) {
      feedbackPopup.hide();
      feedbackPopup = null;
    }
  }

  @Override
  public void removeNotify() {
    hideFreetextFeedback();
    super.removeNotify();
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
