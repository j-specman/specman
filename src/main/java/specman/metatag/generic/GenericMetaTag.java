package specman.metatag.generic;

import specman.editarea.InteractiveStepFragment;
import specman.metatag.AbstractMetaTag;
import specman.metatag.MetaTagPanel;
import specman.metatag.generic.config.MetaTagConfig;
import specman.pdf.HangingTabShape;
import specman.pdf.IconShape;
import specman.pdf.LabelShapeText;
import specman.pdf.Shape;
import specman.undo.UndoableMetaTagFreetextChanged;

import javax.swing.BorderFactory;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

import static specman.Specman.editor;
import static specman.graphics.Styles.SCHRITTNR_FONTSIZE;
import static specman.graphics.Styles.Schriftfarbe_Geloescht;
import static specman.graphics.Styles.metaTagFont;

/**
 * Configurable meta-tag displayed to the left of a step number.
 */
public class GenericMetaTag extends AbstractMetaTag implements InteractiveStepFragment {
  private static final int SIDE_PADDING = 2;
  // The last letter of a label would otherwise come too close to the rounded bottom right corner.
  // Added on the left as well if there is no icon, to keep a text-only label centered.
  private static final int LABEL_EXTRA_PADDING = 2;
  private static final int TOOLTIP_WIDTH = 250;
  private static final int FEEDBACK_DURATION_MS = 3000;
  private static final Color DELETED_BACKGROUND = Color.BLACK;

  private final Color defaultForeground = getForeground();
  private LiftedMetaTagIcon icon;
  private MetaTagConfig config;
  private String freetext;
  private boolean freetextEditable = true;
  private boolean deleted = false;
  // Lifts the text, see alignTextVertically(). Zero until the tag knows its height.
  private int bottomInset = 0;
  private final TagLabelLayout labelLayout = new TagLabelLayout(this);
  private Popup feedbackPopup;
  private Timer feedbackTimer;

  /** @param freetext optional freetext shown as tooltip while hovering the tag, may be null. It is
   * ignored as long as the configuration doesn't allow a freetext, see {@link #getFreetext()}. */
  public GenericMetaTag(MetaTagConfig config, String freetext) {
    setHorizontalAlignment(SwingConstants.CENTER);
    // Not opaque although the tag has a background of its own: see paintComponent().
    setOpaque(false);
    this.freetext = freetext;
    applyConfig(config);

    addMouseListener(MetaTagContextMenu.instance);
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (freetextEditable && SwingUtilities.isLeftMouseButton(e)) {
          editFreetext();
        }
      }
    });
  }

  /** Turned off for pure previews of a configuration, e.g. in the configuration dialog. */
  public void setFreetextEditable(boolean freetextEditable) {
    this.freetextEditable = freetextEditable;
  }

  public MetaTagConfig getConfig() {
    return config;
  }

  /** Whether the step the tag belongs to is marked as deleted. Such a tag can't be changed, but its
   * freetext can still be read. Set by the step, which is the one who knows. */
  public void setDeleted(boolean deleted) {
    if (this.deleted != deleted) {
      this.deleted = deleted;
      applyAppearance();
    }
  }

  public boolean isDeleted() {
    return deleted;
  }

  /** Takes over icon, label and colors of the given configuration, e.g. after it was edited. */
  public void applyConfig(MetaTagConfig config) {
    this.config = config;
    icon = config.hasIcon() ? new LiftedMetaTagIcon(config.getIconSvg()) : null;
    setIcon(icon);
    setText(config.getLabel());
    updateTooltip();
    applyAppearance();
    alignTextVertically(getHeight());
  }

  /** The tag of a deleted step is shown as the step number is: gray on black, with a gray border
   * even if the configuration has none (black on black would be hard to see), and a gray scale icon. */
  private void applyAppearance() {
    setBackground(deleted ? DELETED_BACKGROUND : config.getBackgroundColor());
    setForeground(deleted ? Schriftfarbe_Geloescht : defaultForeground);
    setBorderColor(deleted ? DELETED_BORDER_COLOR : config.getBorderColor());
    if (icon != null) {
      icon.setGrayedOut(deleted);
    }
    scale();
    revalidate();
    repaint();
  }

  void editFreetext() {
    if (!config.allowsFreetext() || deleted) {
      return;
    }
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

  public void setFreetext(String freetext) {
    this.freetext = freetext;
    updateTooltip();
  }

  /** @return the freetext, null if the tag has none or its configuration doesn't allow one. In the
   * latter case the text itself is kept, so that undoing the configuration change brings it back,
   * but it is neither shown nor saved. */
  public String getFreetext() {
    return config.allowsFreetext() ? freetext : null;
  }

  private void updateTooltip() {
    setToolTipText(toTooltipHtml(getFreetext()));
  }

  /** Shows the tooltip for a few seconds without the mouse hovering over the tag, e.g. as visual
   * feedback which tag was just changed by an undo/redo. Swing has no public API to trigger a
   * tooltip programmatically, so a JToolTip is put into a popup by hand. Nothing is shown if the
   * tag has no freetext or is currently not visible. */
  public void showFreetextFeedback() {
    hideFreetextFeedback();
    String tipText = toTooltipHtml(getFreetext());
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
    if (icon != null) {
      icon.scale();
    }
    setFont(metaTagFont.deriveFont((float) editor().scale(SCHRITTNR_FONTSIZE)));
    updateBorder();
  }

  private void updateBorder() {
    boolean hasLabel = !config.getLabel().isEmpty();
    int leftPadding = hasLabel && icon == null ? SIDE_PADDING + LABEL_EXTRA_PADDING : SIDE_PADDING;
    int rightPadding = hasLabel ? SIDE_PADDING + LABEL_EXTRA_PADDING : SIDE_PADDING;
    // An empty border: a colored one would be painted on top of the background, which is not
    // opaque, and make the tag look different in the area of the insets.
    setBorder(BorderFactory.createEmptyBorder(0, (int) editor().scale(leftPadding), bottomInset, (int) editor().scale(rightPadding)));
  }

  /** The height of the tag is only known when it is given its bounds, hence the text is aligned
   * here, see {@link TagLabelLayout}. */
  @Override
  public void setBounds(int x, int y, int width, int height) {
    super.setBounds(x, y, width, height);
    alignTextVertically(height);
  }

  /** Lifts the text via the bottom inset. The icon is held where it would be without lifting, only
   * the text moves. A tag without label has nothing to lift. */
  private void alignTextVertically(int height) {
    if (height <= 0) {
      return;
    }
    int newInset = getText().isEmpty() ? 0 : labelLayout.bottomInset(height);
    if (newInset != bottomInset) {
      bottomInset = newInset;
      updateBorder();
    }
    if (icon != null) {
      icon.setLift(labelLayout.iconLift(height, bottomInset));
    }
  }

  /** Hangs the tag left of its right neighbour at the given height, which is the step number's,
   * same trick as KlappButton#updateLocation. */
  public void updateLocation(int rightNeighborX, int height) {
    if (height > 0) {
      int gap = (int) editor().scale(MetaTagPanel.WIDGET_GAP);
      int width = getPreferredSize().width;
      setBounds(rightNeighborX - gap - width, 0, width, height);
    }
  }

  /** Fills the background itself, because the tag is deliberately not opaque, see the constructor.
   * <p>
   * The rounded bottom corners of a tag are transparent: {@link specman.metatag.HangingTabRenderer}
   * masks them out of what the tag paints. A component which reports itself as opaque promises to
   * cover its whole bounds, though, and Swing relies on that: when only the tag is repainted, e.g.
   * after the step was inserted or the zoom factor was changed, it doesn't paint what is behind the
   * tag first. Whatever was painted behind the corners before then stays, which showed up as small
   * glitches in the corners, particularly visible where the tag sits on a dark background. As the
   * tag is not opaque, Swing paints its surroundings first, and the tag only paints its own,
   * rounded shape on top of that. */
  @Override
  protected void paintComponent(Graphics g) {
    g.setColor(getBackground());
    g.fillRect(0, 0, getWidth(), getHeight());
    super.paintComponent(g);
  }

  @Override
  protected int arc() {
    return (int) editor().scale(CORNER_ARC);
  }

  /** The PDF counterpart of the tag as it looks on screen, also at a deleted step. Where Swing
   * puts icon and text is taken from the same layout computation the label itself uses. */
  public Shape getShape() {
    FontMetrics metrics = getFontMetrics(getFont());
    Rectangle iconRect = new Rectangle();
    Rectangle textRect = new Rectangle();
    labelLayout.layout(SwingUtilities.calculateInnerArea(this, null), iconRect, textRect);

    HangingTabShape shape = new HangingTabShape(this, arc())
      .withBorder(paintedBorderColor(), borderStrokeWidth());
    if (!getText().isEmpty()) {
      // LabelShapeText takes the distance of the text's baseline from the top as its top inset
      int baselineCorrection = metrics.getAscent() - getFont().getSize() + 1;
      Insets textInsets = new Insets(textRect.y + baselineCorrection, textRect.x + 1, 0, 0);
      shape.withText(new LabelShapeText(getText(), textInsets, getForeground(), getFont()));
    }
    if (icon != null) {
      Rectangle paintedArea = new Rectangle(iconRect.x, iconRect.y, iconRect.width, iconRect.height + icon.lift());
      shape.add(new IconShape(this, icon, paintedArea));
    }
    return shape;
  }
}
