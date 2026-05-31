package specman;

import specman.editarea.markups.MarkupType;
import specman.editarea.markups.TextMarkup;
import specman.graphics.ChangeColorSet;
import specman.graphics.ReadWriteColor;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static specman.graphics.Styles.Schriftfarbe_Geloescht;
import static specman.graphics.Styles.SCHRITTNR_FONTSIZE;
import static specman.Specman.editor;

public class ChangeSet {

  public static final ReadWriteColor STEPNUMBER_LINK_COLOR = new ReadWriteColor(new Color(220, 220, 220));

  public static final ChangeSet DEFAULT;

  public static final Map<String, ChangeSet> ALL;

  /** The colors in the following palette have been carefully chosen to remain distinguishable
   *  for users with common forms of color vision deficiency (deuteranopia, protanopia). */
  static {
    ALL = new LinkedHashMap<>();
    register("yellow", Color.yellow);
    register("lilac",  new Color(200, 170, 240));
    register("blue",   new Color(130, 210, 255));
    register("orange", new Color(255, 220, 150));
    register("pink",   new Color(255, 170, 170));
    DEFAULT = ALL.get("yellow");
  }

  private static void register(String name, Color color) {
    ALL.put(name, new ChangeSet(name, new ChangeColorSet(color, STEPNUMBER_LINK_COLOR.color)));
  }

  private static void register(String name, Color color, Color menuColor) {
    ALL.put(name, new ChangeSet(name, new ChangeColorSet(color, STEPNUMBER_LINK_COLOR.color, menuColor)));
  }

  public final String name;
  public final ChangeColorSet colors;
  private final MutableAttributeSet deletedStyle;
  private final MutableAttributeSet sourceStyle;
  private final MutableAttributeSet deletedStepnumberLinkStyle;

  private ChangeSet(String name, ChangeColorSet colors) {
    this.name = name;
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

  public Color menuColor() { return colors.menuColor; }

  public Color panelColor() { return colors.panelColor; }
  public Color buttonColor() { return colors.panelColor; }
  public Color activeButtonColor() { return colors.text.color; }
  public boolean isAnyBackground(String cssColor) { return colors.isAnyBackground(cssColor); }
  public Color textColor() { return colors.text.color; }
  public String textHtmlColor() { return colors.text.htmlColor; }
  public javax.swing.text.AttributeSet textBackground() { return colors.text.background; }
  public Color stepnumberLinkColor() { return colors.stepnumberLink.color; }
  public String stepnumberLinkHtmlColor() { return colors.stepnumberLink.htmlColor; }
  public javax.swing.text.AttributeSet stepnumberLinkBackground() { return colors.stepnumberLink.background; }

  public MutableAttributeSet getDeletedStyle() { return deletedStyle; }

  public MutableAttributeSet getSourceStyle() { return sourceStyle; }

  public MutableAttributeSet getDeletedStepnumberLinkStyle() { return deletedStepnumberLinkStyle; }

  public static ChangeSet changeset() { return editor().changeset(); }

  public static ChangeSet fromName(String name) {
    return name != null ? ALL.get(name) : null;
  }

  public static boolean isAnyChangeSetBackground(String cssColor) {
    return ALL.values().stream().anyMatch(cs -> cs.isAnyBackground(cssColor));
  }

  public static TextMarkup textMarkupFromBackground(String cssColor) {
    for (ChangeSet cs : ALL.values()) {
      MarkupType type = cs.colors.toMarkupType(cssColor);
      if (type != null) {
        return (type != MarkupType.Steplink)
          ? new TextMarkup(type, cs)
          : new TextMarkup(type, null);
      }
    }
    return null;
  }

  @Override
  public String toString() { return name; }
}
