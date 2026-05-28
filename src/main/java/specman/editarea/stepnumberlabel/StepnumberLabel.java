package specman.editarea.stepnumberlabel;

import org.apache.commons.lang.math.IntRange;
import specman.SchrittID;
import specman.Specman;
import static specman.ChangeSet.changeset;
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

import static specman.SchrittID.asString;
import specman.ChangeSet;
import specman.Specman;
import static specman.ChangeSet.changeset;
import static specman.graphics.Styles.DELETED_BACKGROUND_COLOR;
import static specman.graphics.Styles.SCHRITTNUMMER_FARBE;
import static specman.graphics.Styles.SCHRITTNUMMER_VORDERGRUNDFARBE;
import static specman.graphics.Styles.Schriftfarbe_Geloescht;
import static specman.graphics.Styles.labelFont;
import static specman.Specman.editor;

public class StepnumberLabel extends JLabel implements InteractiveStepFragment {
  private static final Border STANDARD_BORDER = new MatteBorder(0, 2, 0, 1, SCHRITTNUMMER_FARBE.color);
  private static final Border CHANGED_BORDER = new MatteBorder(0, 2, 0, 1, ChangeSet.DEFAULT.colors.panelColor);
  private static final Border DELETED_BORDER = new MatteBorder(0, 2, 0, 1, DELETED_BACKGROUND_COLOR.color);
  private static final String SPACER = " ";
  private static final String TO_TARGET_ARROW = SPACER + ">" + SPACER;
  private static final String FROM_SOURCE_ARROW = SPACER + "<" + SPACER;

  private LabelStructure structure;

  public StepnumberLabel(SchrittID stepNumber) {
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

  public void setStepNumber(SchrittID stepNumber) {
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
    super.paint(g);
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

  public void setStandardStyle(SchrittID id) {
    setBorder(STANDARD_BORDER);
    setBackground(SCHRITTNUMMER_FARBE.color);
    setForeground(SCHRITTNUMMER_VORDERGRUNDFARBE);
    this.structure = LabelStructure.Standard;
    setText(id.toString());
  }

  public void setTargetStyleUDBL(SchrittID quellschrittId, ChangeSet changeset) {
    setStructureUDBL(LabelStructure.Target);
    setBorderUDBL(CHANGED_BORDER);
    setBackgroundUDBL(changeset.panelColor());
    setForegroundUDBL(DELETED_BACKGROUND_COLOR.color);
    resyncSourceSuffixUDBL(quellschrittId);
  }

  public void setSourceStyle(SchrittID zielschrittID) {
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

  public void resyncTargetSuffixUDBL(SchrittID targetStepNumber) {
    resyncRelatedStepNumberUDBL(targetStepNumber);
  }

  public void resyncSourceSuffixUDBL(SchrittID sourceStepNumber) {
    resyncRelatedStepNumberUDBL(sourceStepNumber);
  }

  private void resyncRelatedStepNumberUDBL(SchrittID relatedStepNumber) {
    NumberPair numbers = splitText();
    setTextUDBL(numbers.own, relatedStepNumber.toString());
  }

  public void setDeletedStyleUDBL(SchrittID id) {
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