package specman.editarea.stepnumberlabel;

import org.apache.commons.lang.math.IntRange;
import specman.StepNumber;
import specman.draganddrop.DragMouseAdapter;
import specman.editarea.InteractiveStepFragment;
import specman.pdf.LineShape;
import specman.undo.props.UDBL;
import specman.pdf.LabelShapeText;
import specman.pdf.Shape;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import static specman.StepNumber.asString;
import specman.ChangeSet;

import static specman.graphics.Styles.DELETED_BACKGROUND_COLOR;
import static specman.graphics.Styles.SCHRITTNUMMER_FARBE;
import static specman.graphics.Styles.SCHRITTNUMMER_VORDERGRUNDFARBE;
import static specman.graphics.Styles.Schriftfarbe_Geloescht;
import static specman.graphics.Styles.labelFont;
import static specman.Specman.editor;

public class StepnumberLabel extends JLabel implements InteractiveStepFragment {
  // Quick and dirty prototype for rounded bottom corners - just to get a visual impression.
  private static final int ROUNDED_CORNER_ARC = 7;

  // Border color matches the respective background color in each state, so widening left/right
  // here visually widens the gray step number box itself (quick and dirty, no separate padding).
  // Tied to ROUNDED_CORNER_ARC so the widened box stays visually matched to the rounding.
  // Bottom inset is larger than top on purpose: Swing centers the text within bounds minus
  // insets, so a larger bottom inset nudges the centered text upward within the same overall
  // box height, without changing the gray background size itself. Public so EditContainer can
  // compensate for the resulting preferredSize growth when it forces the actual bounds height.
  public static final int TEXT_LIFT_BOTTOM_INSET = 2;
  private static final Border STANDARD_BORDER = new MatteBorder(0, ROUNDED_CORNER_ARC / 2, TEXT_LIFT_BOTTOM_INSET, ROUNDED_CORNER_ARC / 2, SCHRITTNUMMER_FARBE.color);
  private static final Border CHANGED_BORDER = new MatteBorder(0, ROUNDED_CORNER_ARC / 2, TEXT_LIFT_BOTTOM_INSET, ROUNDED_CORNER_ARC / 2, ChangeSet.DEFAULT.colors.panelColor);
  private static final Border DELETED_BORDER = new MatteBorder(0, ROUNDED_CORNER_ARC / 2, TEXT_LIFT_BOTTOM_INSET, ROUNDED_CORNER_ARC / 2, DELETED_BACKGROUND_COLOR.color);
  private static final String SPACER = " ";
  private static final String TO_TARGET_ARROW = SPACER + ">" + SPACER;
  private static final String FROM_SOURCE_ARROW = SPACER + "<" + SPACER;

  private LabelStructure structure;

  public StepnumberLabel(StepNumber stepNumber) {
    super(String.valueOf(stepNumber));

    structure = LabelStructure.Standard;
    setFont(labelFont);
    setBackground(SCHRITTNUMMER_FARBE.color);
    setBorder(STANDARD_BORDER);
    setForeground(Color.WHITE);
    setOpaque(true);

    DragMouseAdapter ada = editor().createDragMouseAdapter();
    addMouseListener(ada);
    addMouseMotionListener(ada);
    addMouseListener(BreakCatchScrollMouseAdapter.instance);
    addMouseListener(StepnumberContextMenu.instance);
  }

  private java.awt.Shape roundedBottomClip() {
    RoundRectangle2D rounded = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ROUNDED_CORNER_ARC, ROUNDED_CORNER_ARC);
    Area area = new Area(rounded);
    area.add(new Area(new Rectangle(0, 0, getWidth(), getHeight() / 2))); // square the top corners off again
    return area;
  }

  public void setStepNumber(StepNumber stepNumber) {
    NumberPair numbers = splitText();
    setTextUDBL(stepNumber.toString(), numbers.related);
  }

  private NumberPair splitText() {
    String text = getText();
    if (structure != LabelStructure.Standard) {
      int arrowStart = text.indexOf(SPACER);
      // This might happen if the label structure has been updated from standard to source/target,
      // but the text has not yet been updated accordingly.
      if (arrowStart >= 0) {
        return new NumberPair(
          text.substring(0, arrowStart),
          text.substring(arrowStart2RelatedNumberStart(arrowStart)));
      }
    }
    return new NumberPair(text);
  }

  @Override
  public void paint(Graphics g) {
    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) {
      super.paint(g);
      drawDeletionLine(g);
      return;
    }
    // Trial of "real" antialiasing, as opposed to the plain setClip() approach: setClip() produces
    // a hard pixel mask regardless of antialiasing hints (they only affect fill/draw/text, not
    // clipping). True soft edges require rendering into an offscreen buffer and compositing it
    // against an antialiased mask shape via AlphaComposite.DstIn.
    BufferedImage content = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D contentG = content.createGraphics();
    contentG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    super.paint(contentG);
    contentG.dispose();

    BufferedImage mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D maskG = mask.createGraphics();
    maskG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    maskG.setColor(Color.WHITE);
    maskG.fill(roundedBottomClip());
    maskG.dispose();

    Graphics2D compositeG = content.createGraphics();
    compositeG.setComposite(AlphaComposite.DstIn);
    compositeG.drawImage(mask, 0, 0, null);
    compositeG.dispose();

    Graphics2D g2 = (Graphics2D) g.create();
    g2.drawImage(content, 0, 0, null);
    g2.dispose();
    drawDeletionLine(g);
  }

  private void drawDeletionLine(Graphics g) {
    LineShape dline = createDeletionLine();
    if (dline != null) {
      g.drawLine(dline.start().x, dline.start().y, dline.end().x, dline.end().y);
    }
  }

  private LineShape createDeletionLine() {
    // + 1 turned out to produce a better vertical line placement
    int VERTICAL_LINE_PLACEMENT_OFFSET = 1;
    IntRange delSubStringRange = findDelSubStringRange();

    if (delSubStringRange != null) {
      FontMetrics metrics = getFontMetrics(getFont());
      int undeletedWidth = metrics.stringWidth(getText().substring(0, delSubStringRange.getMinimumInteger()));
      int deletedWidth = metrics.stringWidth(getText().substring(delSubStringRange.getMinimumInteger(), delSubStringRange.getMaximumInteger()));
      return new LineShape(
        undeletedWidth + VERTICAL_LINE_PLACEMENT_OFFSET,
        getHeight() / 2,
        undeletedWidth + deletedWidth + VERTICAL_LINE_PLACEMENT_OFFSET,
        getHeight() / 2)
        .withColor(getForeground())
        .withWidth(0.5f);
    }
    return null;
  }

  /** Important to remember: The end index of Java's String#substring method
   * is the index of the first character NOT included in the substring. I.e.
   * a substring from index 0 to index 0 is an empty string. The end index
   * runs from 0 to */
  private IntRange findDelSubStringRange() {
    if (structure == LabelStructure.Standard) {
      if (fullTextDeleted()) {
        return new IntRange(0, getText().length());
      }
      return null;
    }
    int arrowStart = getText().indexOf(SPACER);
    if (structure == LabelStructure.Source) {
      return new IntRange(0, arrowStart);
    }
    return new IntRange(arrowStart2RelatedNumberStart(arrowStart), getText().length());
  }

  private int arrowStart2RelatedNumberStart(int arrowStart) {
    return arrowStart + TO_TARGET_ARROW.length();
  }

  private boolean fullTextDeleted() {
    return structure == LabelStructure.Standard && getBackground() == DELETED_BACKGROUND_COLOR.color;
  }

  public void setStandardStyle(StepNumber id) {
    setBorder(STANDARD_BORDER);
    setBackground(SCHRITTNUMMER_FARBE.color);
    setForeground(SCHRITTNUMMER_VORDERGRUNDFARBE);
    this.structure = LabelStructure.Standard;
    setText(id.toString());
  }

  public void setTargetStyleUDBL(StepNumber quellschrittId, ChangeSet changeset) {
    setStructureUDBL(LabelStructure.Target);
    setBorderUDBL(CHANGED_BORDER);
    setBackgroundUDBL(changeset.panelColor());
    setForegroundUDBL(DELETED_BACKGROUND_COLOR.color);
    resyncSourceSuffixUDBL(quellschrittId);
  }

  public void setSourceStyle(StepNumber zielschrittID) {
    setStructure(LabelStructure.Source);
    setBorder(DELETED_BORDER);
    setBackground(DELETED_BACKGROUND_COLOR.color);
    setForeground(Schriftfarbe_Geloescht);
    NumberPair numbers = splitText();
    setTextUDBL(numbers.own, asString(zielschrittID));
  }

  private void setTextUDBL(String own, String related) {
    if (structure == LabelStructure.Standard) {
      setTextUDBL(own);
    }
    else {
      String arrow = structure == LabelStructure.Target ? FROM_SOURCE_ARROW : TO_TARGET_ARROW;
      setTextUDBL(own + arrow + related);
    }
  }

  public void resyncTargetSuffixUDBL(StepNumber targetStepNumber) {
    resyncRelatedStepNumberUDBL(targetStepNumber);
  }

  public void resyncSourceSuffixUDBL(StepNumber sourceStepNumber) {
    resyncRelatedStepNumberUDBL(sourceStepNumber);
  }

  private void resyncRelatedStepNumberUDBL(StepNumber relatedStepNumber) {
    NumberPair numbers = splitText();
    setTextUDBL(numbers.own, relatedStepNumber.toString());
  }

  public void setDeletedStyleUDBL(StepNumber id) {
    setStructureUDBL(LabelStructure.Standard);
    setBorderUDBL(DELETED_BORDER);
    setBackgroundUDBL(DELETED_BACKGROUND_COLOR.color);
    setForegroundUDBL(Schriftfarbe_Geloescht);
    setTextUDBL(id.toString(), null);
  }

  public Shape getShape() {
    return new Shape(this)
      .withText(new LabelShapeText(getText(), getInsets(), getForeground(), getFont()))
      .add(createDeletionLine());
  }

  private void setStructureUDBL(LabelStructure structure) { UDBL.setStructureUDBL(this, structure); }
  private void setTextUDBL(String text) { UDBL.setTextUDBL(this, text); }
  private void setForegroundUDBL(Color fg) { UDBL.setForegroundUDBL(this, fg); }
  private void setBackgroundUDBL(Color fg) { UDBL.setBackgroundUDBL(this, fg); }
  private void setBorderUDBL(Border border) { UDBL.setBorderUDBL(this, border); }

  @Override
  public String toString() {
    return "SchrittNummerLabel " + getText();
  }

  public void setStructure(LabelStructure structure) { this.structure = structure; }

  public LabelStructure getStructure() { return structure; }

  public enum LabelStructure {
    Standard, Source, Target;
  }

  private static class NumberPair {
    final String own;
    final String related;

    NumberPair(String own, String related) {
      this.own = own;
      this.related = related;
    }

    NumberPair(String own) {
      this(own, null);
    }
  }
}