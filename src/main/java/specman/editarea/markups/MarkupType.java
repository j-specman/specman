package specman.editarea.markups;

import specman.editarea.document.WrappedElement;

import javax.swing.text.AttributeSet;
import javax.swing.text.html.CSS;

import static specman.styles.Styles.AENDERUNGSFARBE;
import static specman.styles.Styles.STEPNUMBER_LINK_COLOR;

public enum MarkupType {
  Changed, Steplink, ChangedSteplink;

  public boolean marksChange() {
    return this == Changed || this == ChangedSteplink;
  }

  public boolean matches(MarkupSearchPurpose searchPurpose) {
    return searchPurpose == searchPurpose.All ||
      searchPurpose == searchPurpose.FirstChangeOnly && marksChange();
  }

  public static MarkupType fromBackground(WrappedElement element) {
    return AENDERUNGSFARBE.toMarkupType(getBackgroundColorFromElement(element));
  }

  private static String getBackgroundColorFromElement(WrappedElement element) {
    Object backgroundColorValue = element.getAttributes().getAttribute(CSS.Attribute.BACKGROUND_COLOR);
    return backgroundColorValue != null ? backgroundColorValue.toString() : null;
  }

  public AttributeSet toBackground() {
    switch(this) {
      case Changed:
        return AENDERUNGSFARBE.text.background;
      case Steplink:
        return STEPNUMBER_LINK_COLOR.background;
      case ChangedSteplink:
        return AENDERUNGSFARBE.stepnumberLink.background;
    }
    return null;
  }

}
