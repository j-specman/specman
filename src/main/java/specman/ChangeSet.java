package specman;

import specman.editarea.markups.MarkupType;
import specman.graphics.ChangeColorSet;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.util.List;

import static specman.graphics.Styles.Schriftfarbe_Geloescht;
import static specman.graphics.Styles.SCHRITTNR_FONTSIZE;
import static specman.graphics.Styles.STEPNUMBER_LINK_COLOR;

public class ChangeSet {

  public static final ChangeSet DEFAULT = new ChangeSet(new ChangeColorSet(Color.yellow, STEPNUMBER_LINK_COLOR.color));

  public static final List<ChangeSet> ALL = List.of(
    DEFAULT,
    new ChangeSet(new ChangeColorSet(new Color(150, 255, 150), STEPNUMBER_LINK_COLOR.color)), // green
    new ChangeSet(new ChangeColorSet(new Color(100, 255, 255), STEPNUMBER_LINK_COLOR.color)), // cyan
    new ChangeSet(new ChangeColorSet(new Color(255, 220, 150), STEPNUMBER_LINK_COLOR.color)), // orange
    new ChangeSet(new ChangeColorSet(new Color(255, 170, 170), STEPNUMBER_LINK_COLOR.color))  // pink
  );

  public final ChangeColorSet colors;
  private final MutableAttributeSet deletedStyle;
  private final MutableAttributeSet sourceStyle;
  private final MutableAttributeSet deletedStepnumberLinkStyle;

  public ChangeSet(ChangeColorSet colors) {
    this.colors = colors;

    deletedStyle = new SimpleAttributeSet();
    StyleConstants.setBackground(deletedStyle, colors.text.color);
    StyleConstants.setStrikeThrough(deletedStyle, true);

    sourceStyle = new SimpleAttributeSet();
    StyleConstants.setBackground(sourceStyle, colors.text.color);
    StyleConstants.setStrikeThrough(sourceStyle, true);
    StyleConstants.setForeground(sourceStyle, Schriftfarbe_Geloescht);
    StyleConstants.setFontSize(sourceStyle, SCHRITTNR_FONTSIZE - 3);

    deletedStepnumberLinkStyle = new SimpleAttributeSet();
    StyleConstants.setBackground(deletedStepnumberLinkStyle, colors.stepnumberLink.color);
    StyleConstants.setStrikeThrough(deletedStepnumberLinkStyle, true);
  }

  public Color menuColor() { return colors.text.color; }

  public Color panelColor() { return colors.panelColor; }
  public Color buttonColor() { return colors.panelColor; }
  public Color activeButtonColor() { return colors.text.color; }
  public boolean isAnyBackground(String cssColor) { return colors.isAnyBackground(cssColor); }
  public MarkupType toMarkupType(String cssColor) { return colors.toMarkupType(cssColor); }
  public Color textColor() { return colors.text.color; }
  public String textHtmlColor() { return colors.text.htmlColor; }
  public javax.swing.text.AttributeSet textBackground() { return colors.text.background; }
  public Color stepnumberLinkColor() { return colors.stepnumberLink.color; }
  public String stepnumberLinkHtmlColor() { return colors.stepnumberLink.htmlColor; }
  public javax.swing.text.AttributeSet stepnumberLinkBackground() { return colors.stepnumberLink.background; }

  public MutableAttributeSet getDeletedStyle() { return deletedStyle; }

  public MutableAttributeSet getSourceStyle() { return sourceStyle; }

  public MutableAttributeSet getDeletedStepnumberLinkStyle() { return deletedStepnumberLinkStyle; }

  public static ChangeSet changeset() { return Specman.instance().changeset(); }

  public static MarkupType markupTypeFromBackground(String cssColor) {
    for (ChangeSet cs : ALL) {
      MarkupType type = cs.colors.toMarkupType(cssColor);
      if (type != null) {
        return type;
      }
    }
    return null;
  }

}
