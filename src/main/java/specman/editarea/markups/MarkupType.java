package specman.editarea.markups;

import specman.editarea.document.WrappedElement;

import javax.swing.text.AttributeSet;
import javax.swing.text.html.CSS;

import specman.ChangeSet;
import static specman.ChangeSet.changeset;
import static specman.ChangeSet.STEPNUMBER_LINK_COLOR;

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
    String cssColor = getBackgroundColorFromElement(element);
    return ChangeSet.markupTypeFromBackground(cssColor);
  }

  private static String getBackgroundColorFromElement(WrappedElement element) {
    Object backgroundColorValue = element.getAttributes().getAttribute(CSS.Attribute.BACKGROUND_COLOR);
    return backgroundColorValue != null ? backgroundColorValue.toString() : null;
  }

  public AttributeSet toBackground() {
    switch(this) {
      case Changed:
        return changeset().textBackground();
      case Steplink:
        return STEPNUMBER_LINK_COLOR.background;
      case ChangedSteplink:
        return changeset().stepnumberLinkBackground();
    }
    return null;
  }

}
