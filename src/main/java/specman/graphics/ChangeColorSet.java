package specman.graphics;

import specman.ChangeSet;
import specman.editarea.markups.MarkupType;
import java.awt.Color;

public class ChangeColorSet {

  public final ReadWriteColor text;
  public final Color panelColor;
  public final ReadWriteColor stepnumberLink;

  public ChangeColorSet(Color color, Color stepnumberLinkBaseColor) {
    this.text = new ReadWriteColor(color);
    this.panelColor = mixWithWhite(color, 0.8f);
    this.stepnumberLink = new ReadWriteColor(combineColors(stepnumberLinkBaseColor, color));
  }

  public static Color combineColors(Color color, Color anotherColor) {
    int r = (color.getRed()   + anotherColor.getRed())   / 2;
    int g = (color.getGreen() + anotherColor.getGreen()) / 2;
    int b = (color.getBlue()  + anotherColor.getBlue())  / 2;
    return new Color(r, g, b);
  }

  public static Color mixWithWhite(Color color, float whiteFraction) {
    int r = (int)(color.getRed()   + (255 - color.getRed())   * whiteFraction);
    int g = (int)(color.getGreen() + (255 - color.getGreen()) * whiteFraction);
    int b = (int)(color.getBlue()  + (255 - color.getBlue())  * whiteFraction);
    return new Color(r, g, b);
  }

  public boolean isTextBackground(String cssColor) {
    return text.isBackground(cssColor);
  }

  public boolean isStepnumberLinkBackground(String cssColor) {
    return stepnumberLink.isBackground(cssColor);
  }

  public boolean isAnyBackground(String cssColor) {
    return isTextBackground(cssColor) || isStepnumberLinkBackground(cssColor);
  }

  public MarkupType toMarkupType(String cssColor) {
    if (isStepnumberLinkBackground(cssColor)) {
      return MarkupType.ChangedSteplink;
    }
    if (isTextBackground(cssColor)) {
      return MarkupType.Changed;
    }
    if (ChangeSet.STEPNUMBER_LINK_COLOR.isBackground(cssColor)) {
      return MarkupType.Steplink;
    }
    return null;
  }
}
