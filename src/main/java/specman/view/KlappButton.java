package specman.view;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import specman.editarea.MetaStripPanel;
import specman.graphics.HangingTab;
import specman.graphics.SvgIcon;
import specman.Specman;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import static specman.view.AbstractSchrittView.ZEILENLAYOUT_INHALT_SICHTBAR;
import static specman.view.AbstractSchrittView.ZEILENLAYOUT_INHALT_VERBORGEN;
import static specman.graphics.Styles.SCHRITTNUMMER_FARBE;
import static specman.Specman.editor;

/**
 * Dieser Button dient dazu, unterstrukturierte Schritte auf und zuzuklappen.
 * Dafür würde man normalerweise von JToggleButton ableiten, aber leider hat diese Klasse
 * die Macke, dass man die Hintergrundfarbe im selektierten Zustand nicht individuell festlegen
 * kann. Die wollen wir aber als Indikator verwenden, ob ein Zusammenklappen etwaige Änderungen
 * verbirgt oder nicht. Nichtmal JButton ist als Basisklasse geeignet, weil dieser abhängig vom
 * Look&Feel die Hintergrundfarbe Status-abhängig verändert. Also basteln wir uns aus einem
 * JLabel selber einen Toggle-Button.
 *
 * @author less02
 */
public class KlappButton extends HangingTab implements MouseMotionListener, MouseListener {
  // Base (100% zoom) size of the square button - also used by CatchBereich to size an unrelated
  // row spec, independent of any button instance, hence public and static.
  public static final int MINIMUM_ICON_LENGTH = 12;
  public static final String ZEILENLAYOUT_FILLER_VISIBLE = "fill:0px:grow";
  public static final String ZEILENLAYOUT_FILLER_HIDDEN = ZEILENLAYOUT_INHALT_VERBORGEN;
  // Base (100% zoom) size of the plus/minus glyph itself, distinct from MINIMUM_ICON_LENGTH
  // (the button's own box size) - the glyph is drawn smaller than its box so it doesn't crowd the
  // rounded corners.
  private static final int ICON_GLYPH_SIZE = 10;
  // Narrower than HangingTab's default SIDE_PADDING: unlike StepnumberLabel's digits, the
  // plus/minus glyph is known never to reach wide enough to collide with the rounded bottom
  // corners, so it can sit closer to the button's own left/right edges.
  private static final int ICON_SIDE_PADDING = 2;

  private final FormLayout layout;
  private final int contentrow;
  private final Integer fillerrow;
  private final KlappbarerBereichI klappbarerBereich;
  private final Container parent;
  private final SvgIcon collapseIcon = new SvgIcon("minus", ICON_GLYPH_SIZE);
  private final SvgIcon expandIcon = new SvgIcon("plus", ICON_GLYPH_SIZE);
  private Color currentColor;
  private boolean selected;

  public KlappButton(KlappbarerBereichI klappbarerBereich, Container parent, FormLayout layout, int contentrow, Integer fillerrow) {
    this.parent = parent;
    this.layout = layout;
    this.contentrow = contentrow;
    this.fillerrow = fillerrow;
    this.klappbarerBereich = klappbarerBereich;
    setHorizontalAlignment(SwingConstants.CENTER);
    setIcon(collapseIcon);
    resetToDefaultBackground();
    setBorderColor(SCHRITTNUMMER_FARBE.color);
    setVisible(false);
    addMouseListener(this);
    parent.addMouseMotionListener(this);
    parent.add(this);
    scale(editor().getZoomFactor(), 100);
  }

  public boolean isSelected() {
    return selected;
  }

  private void setSelected(boolean selected) {
    this.selected = selected;
    setIcon(selected ? expandIcon : collapseIcon);
  }

  public void init(boolean zugeklappt) {
    if (zugeklappt && !isSelected()) {
      mouseClicked(null);
      setVisible(isSelected());
    }
  }

  /** Default look: takes on the step's actual background color (possibly changeset-tinted)
   * rather than being transparent - transparency let whatever happened to be painted underneath
   * (e.g. a text selection highlight in the first edit area, which the metaPanel overlaps) show
   * through the button, which looked broken. parent is metaPanel, kept in sync with the step's own
   * background by EditContainer#setBackground()/setBackgroundUDBL(). */
  private void resetToDefaultBackground() {
    setColor(parent.getBackground());
  }

  /** Same padding/lift trick as StepnumberLabel's own border (see HangingTab#scaledBorder) - this
   * MatteBorder only ever reserves space, it's never meant to be seen itself (same color as the
   * background), unlike HangingTab's own setBorderColor() line which is the one actually painted. */
  private void setColor(Color color) {
    currentColor = color;
    setOpaque(true);
    setBackground(color);
    setBorder(scaledBorder(color, 0, ICON_SIDE_PADDING));
  }

  public void refreshGeklappt() {
    String requiredRowLayoutContent = isSelected()
      ? ZEILENLAYOUT_INHALT_VERBORGEN
      : ZEILENLAYOUT_INHALT_SICHTBAR;
    layout.setRowSpec(contentrow, RowSpec.decode(requiredRowLayoutContent));

    if (fillerrow != null) {
      String requiredRowLayoutFiller = isSelected()
        ? ZEILENLAYOUT_FILLER_VISIBLE
        : ZEILENLAYOUT_INHALT_VERBORGEN;
      layout.setRowSpec(fillerrow, RowSpec.decode(requiredRowLayoutFiller));
    }

    klappbarerBereich.geklappt(!isSelected());
  }

  @Override public void mouseDragged(MouseEvent e) {
  }

  @Override public void mouseMoved(MouseEvent e) {
    // Wenn die Sequenz zugeklappt ist, lassen wir den Aufklapp-Button dauerhaft angezeigt
    // Der unbedarfte User erkennt auf diese Weise leichter, wo er drücken muss, um den
    // Inhalt zu sehen. Wenn die Sequenz aufgeklappt ist, zeigen wir den Button nur an,
    // wenn die Maus an der richtigen Stelle steht. Sonst stören im aufgeklappten Zustand
    // die vielen Button-Icons das Erscheinungsbild des Diagramms
    if (!isSelected()) {
      boolean mausUeberKlappenButton = getBounds().contains(e.getPoint());
      setVisible(mausUeberKlappenButton);
    }
  }

  /**
   * Sorgt dafür, dass der Button auch verschwindet, wenn man ihn (und seinen Container)
   * über den oberen oder linken Rand verlässt. Dann kriegt man nämlich kein mouseMoved
   * mehr mit der Info, dass die Maus nicht mehr über dem Button steht
   */
  @Override public void mouseExited(MouseEvent e) {
    if (!isSelected()) {
      setVisible(false);
    }
  }

  @Override public void mouseClicked(MouseEvent e) {
    setSelected(!isSelected());
    if (isSelected()) {
      boolean zuklappenVerbirgtAenderungen = klappbarerBereich.enthaeltAenderungsmarkierungen();
      if (zuklappenVerbirgtAenderungen) {
        setColor(Color.yellow);
      }
    } else {
      resetToDefaultBackground();
    }
    refreshGeklappt();
  }

  @Override public void mousePressed(MouseEvent e) {
  }

  @Override public void mouseReleased(MouseEvent e) {
  }

  @Override public void mouseEntered(MouseEvent e) {
  }

  public void scale(int newPercentage, int currentPercentage) {
    int iconSize = (int) editor().scale(ICON_GLYPH_SIZE);
    collapseIcon.setSize(iconSize);
    expandIcon.setSize(iconSize);
    if (currentColor != null) {
      setBorder(scaledBorder(currentColor, 0, ICON_SIDE_PADDING));
    }
  }

  /** Matches StepnumberLabel's own width/height recipe exactly - borrows the step number's
   * already-computed height (see EditContainer#updateBounds()) rather than independently
   * recomputing it, so the two meta-strip widgets are guaranteed to end up the same height
   * instead of relying on two separate formulas staying in sync by hand. */
  public void updateLocation(Rectangle stepnumberBounds) {
    if (stepnumberBounds.height > 0) {
      int gap = (int) editor().scale(MetaStripPanel.WIDGET_GAP);
      int width = getPreferredSize().width;
      setBounds(stepnumberBounds.x - gap - width, 0, width, stepnumberBounds.height);
    }
  }

  /** For KlappButtons not paired with a step number (CatchBereich's "collapse catch sequences"
   * button) - there's no StepnumberLabel bounds to borrow a height from, so this falls back to
   * the fixed MINIMUM_ICON_LENGTH as a plain square. */
  public void updateLocation(int remainingWidth) {
    if (remainingWidth > 0) {
      int desiredSize = (int) editor().scale(MINIMUM_ICON_LENGTH);
      setBounds(remainingWidth - desiredSize, 0, desiredSize, desiredSize);
    }
  }

  @Override
  protected int arc() {
    return (int) editor().scale(CORNER_ARC);
  }

  @Override
  protected float borderStrokeWidth() {
    return (float) editor().scale(1.5);
  }

}