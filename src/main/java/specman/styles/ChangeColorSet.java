package specman.styles;

import specman.editarea.markups.MarkupType;
import java.awt.Color;

public class ChangeColorSet {

  public final ReadWriteColor text;
  public final Color panelColor;
  public final ReadWriteColor stepnumberLink;

  public ChangeColorSet(Color color, Color panelColor, Color stepnumberLinkBaseColor) {
    this.text = new ReadWriteColor(color);
    this.panelColor = panelColor;
    this.stepnumberLink = new ReadWriteColor(Styles.combineColors(stepnumberLinkBaseColor, color));
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
    if (Styles.STEPNUMBER_LINK_COLOR.isBackground(cssColor)) {
      return MarkupType.Steplink;
    }
    return null;
  }
}
